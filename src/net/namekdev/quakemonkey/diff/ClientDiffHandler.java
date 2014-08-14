package net.namekdev.quakemonkey.diff;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.namekdev.quakemonkey.diff.messages.AckMessage;
import net.namekdev.quakemonkey.diff.messages.DiffMessage;
import net.namekdev.quakemonkey.diff.messages.LabeledMessage;
import net.namekdev.quakemonkey.diff.utils.BufferPool;
import net.namekdev.quakemonkey.diff.utils.MessageMultiplexer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

/**
 * Handles the client-side job of receiving either messages of type {@code T} or
 * delta messages. If a delta message is received, it is merged with a cached
 * old message. When the message is processed, an acknowledgment is sent to the
 * server.
 * <p>
 * Client can register message listeners for type {@code T} by calling
 * {@link #addListener()}. It is very important that the client does not listen
 * to message type {@code T} through other methods (for example directly from
 * the server).
 * <p>
 * * Important: make sure that you call
 * {@link DiffClassRegistration#registerClasses()} before starting the client.
 * 
 * @author Ben Ruijl
 * 
 * @param <T>
 *            Message type
 */
@SuppressWarnings("unchecked")
public class ClientDiffHandler<T> extends Listener {
	protected static final Logger log = Logger.getLogger(ClientDiffHandler.class.getName());
	private final Kryo kryoSerializer;
	private final short numSnapshots;
	private final Class<T> cls;
	private final List<T> snapshots;
	private final MessageMultiplexer listenerRegistry;
	private short curPos;

	public ClientDiffHandler(Client client, Class<T> cls, short numSnapshots) {
		this.kryoSerializer = client.getKryo();
		this.numSnapshots = numSnapshots;
		this.cls = cls;
		listenerRegistry = new MessageMultiplexer();
		snapshots = new ArrayList<T>(numSnapshots);

		for (int i = 0; i < numSnapshots; i++) {
			snapshots.add(null);
		}

		client.addListener(this);
	}

	public void addListener(Listener listener) {
		listenerRegistry.addMessageListener(listener);
	}

	public void removeListener(Listener listener) {
		listenerRegistry.removeMessageListener(listener);
	}

	/**
	 * Applies the delta message to the old message to generate a new message of
	 * type {@code T}.
	 * 
	 * @param oldMessage
	 *            The old message
	 * @param diffMessage
	 *            The delta message
	 * @return A new message of type {@code T}
	 */
	public T mergeMessage(T oldMessage, DiffMessage diffMessage) {
		ByteBuffer oldBuffer = Utils.messageToBuffer(oldMessage, null, kryoSerializer);

		// Copy old message
		ByteBuffer newBuffer = BufferPool.Default.obtainByteBuffer(32767);
		newBuffer.put(oldBuffer);
		newBuffer.position(0);

		BufferPool.Default.saveByteBuffer(oldBuffer);
		
		byte[] diffFlags = diffMessage.getFlag();
		int[] diffData = diffMessage.getData();
		int index = 0;
		
		for (int i = 0; i < 8 * diffFlags.length; i++) {
			if ((diffFlags[i / 8] & (1 << (i % 8))) != 0) {
				newBuffer.putInt(i * 4, diffData[index]);
				index++;
			}
		}

		Input input = new Input(newBuffer.array());
		input.setPosition(2); // skip size
		Object obj = kryoSerializer.readClassAndObject(input);
		
		BufferPool.Default.saveByteBuffer(newBuffer);
		
		return (T) obj;
	}

	/**
	 * Process the arrival of either a message of type {@code T} or a delta
	 * message. Sends an acknowledgment to the server when a message is
	 * received.
	 */
	@Override
	public void received (Connection source, Object m) {
		if (m instanceof LabeledMessage) {
			LabeledMessage lm = (LabeledMessage) m;
			T message = (T) lm.getMessage();

			boolean isNew = curPos == 0 || curPos < lm.getLabel() || lm.getLabel() - curPos > Short.MAX_VALUE / 2;

			// message is too old
			if (curPos - lm.getLabel() > numSnapshots
					|| (lm.getLabel() - curPos > Short.MAX_VALUE / 2 && Short.MAX_VALUE
							- lm.getLabel() + curPos > numSnapshots)) {
				
				if (log.isLoggable(Level.INFO)) {
					log.log(Level.INFO, "Discarding too old message: " + lm.getLabel() + " vs. cur " + curPos);
				}
				return;
			}

			if (cls.isInstance(lm.getMessage())) { // received full message
				snapshots.set(lm.getLabel() % numSnapshots, message);
			}
			else {
				if (lm.getMessage() instanceof DiffMessage) {
					if (log.isLoggable(Level.FINE)) {
						ByteBuffer logBuffer = Utils.messageToBuffer(message, null, kryoSerializer);
						log.log(Level.FINE, "Received diff of size " + logBuffer.limit());
						BufferPool.Default.saveByteBuffer(logBuffer);
					}

					DiffMessage diffMessage = (DiffMessage) message;

					T oldMessage = snapshots.get(diffMessage.getMessageId() % numSnapshots);
					T newMessage = mergeMessage(oldMessage, diffMessage);

					snapshots.set(lm.getLabel() % numSnapshots, newMessage);
				}
			}

			/* Send an ACK back */
			source.sendUDP(AckMessage.Pool.obtain().set(lm.getLabel()));

			/* Broadcast changes */
			if (isNew) {
				curPos = lm.getLabel();
				listenerRegistry.dispatch(source, snapshots.get(curPos % numSnapshots));
			} else {
				// notify if message was old, for testing
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Old message received: " + lm.getLabel() + " vs. cur " + curPos);
				}
			}

		}
	}
}
