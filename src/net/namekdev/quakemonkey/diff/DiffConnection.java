package net.namekdev.quakemonkey.diff;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.esotericsoftware.kryo.Kryo;

import net.namekdev.quakemonkey.diff.messages.DiffMessage;
import net.namekdev.quakemonkey.diff.messages.LabeledMessage;
import net.namekdev.quakemonkey.diff.utils.BufferPool;
import net.namekdev.quakemonkey.diff.utils.Pool;

/**
 * The server-side handler of generating delta messages for one connection. It
 * keeps track of a list of snapshots in a cyclic array and registers the last
 * snapshot that was successfully received by the client.
 * 
 * @author Ben Ruijl
 * @see #ServerDiffHandler
 * @param <T>
 *            Message type
 */
public class DiffConnection<T> {
	protected static final Logger log = Logger.getLogger(DiffConnection.class.getName());
	private final Kryo kryoSerializer;
	private final short numSnapshots;
	private final List<T> snapshots;
	private short curPos; // position in cyclic array
	private short ackPos;
	
	/**
	 * If set to false, then size of full message and message diff is compared;
	 * based on that the smaller message is being sent.
	 * 
	 * If set to true, then message diff is being sent instead of full message.
	 */
	private final boolean alwaysSendDiff;

	public DiffConnection(Kryo kryoSerializer, short numSnapshots, boolean alwaysSendDiff) {
		this.kryoSerializer = kryoSerializer;
		this.numSnapshots = numSnapshots;
		this.alwaysSendDiff = alwaysSendDiff;
		snapshots = new ArrayList<T>(numSnapshots);

		for (int i = 0; i < numSnapshots; i++) {
			snapshots.add(null);
		}

		curPos = 0;
		ackPos = -1;
	}
	
	public DiffConnection(Kryo kryoSerializer, short numSnapshots) {
		this(kryoSerializer, numSnapshots, false);
	}

	/**
	 * Adds a new message to the snapshot list and either returns the full
	 * message or a delta message if the latter is possible.
	 * 
	 * @param message
	 *            Message to add to snapshot list
	 * @return {@code message} or a delta message
	 */
	public LabeledMessage generateSnapshot(T message) {
		short oldPos = curPos;
		snapshots.set((short) (oldPos % numSnapshots), message);
		curPos++;

		// only allow positive positions
		if (curPos < 0) {
			curPos = 0;
		}

		if (ackPos < 0) {
			return LabeledMessage.Pool.obtain().set(oldPos, message);
		}

		/* Is the last received message too old? Send a full one */
		if (oldPos - ackPos > numSnapshots
				|| (ackPos - oldPos > Short.MAX_VALUE / 2 && Short.MAX_VALUE
						- ackPos + oldPos > numSnapshots)) {
			return LabeledMessage.Pool.obtain().set(oldPos, message);
		}

		T oldMessage = snapshots.get(ackPos % numSnapshots);
		return LabeledMessage.Pool.obtain().set(oldPos, generateDelta(message, oldMessage, ackPos));
	}

	/**
	 * Gets the number of messages the server is lagging behind
	 * 
	 * @return Number of messages left behind
	 */
	public int getLag() {
		if (curPos >= ackPos) {
			return curPos - ackPos;
		}

		return Short.MAX_VALUE - ackPos + curPos;
	}

	public void registerAck(short id) {
		// because the array is cyclic, the id could be in front of the old
		// ackPos,
		// so we check if the difference between the two is very large ( > 4
		// minutes at
		// 60 fps).
		if (id > ackPos || ackPos - id > Short.MAX_VALUE / 2) {
			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Client received message " + id);
			}
			ackPos = id;
			return;
		}

		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Client received old message " + id);
		}
	}

	/**
	 * Returns a delta message from message and prevMessage or just message if
	 * that happens to be smaller.
	 * 
	 * @param message
	 *            Message to send
	 * @param prevMessage
	 *            Previous message
	 * @return
	 */
	private Object generateDelta(T message, T prevMessage, short prevID) {
		BufferPool pool = BufferPool.Default;
		
		ByteBuffer old = Utils.messageToBuffer(prevMessage, null, kryoSerializer);
		ByteBuffer buffer = Utils.messageToBuffer(message, null, kryoSerializer);

		int intBound = (int) (Math.ceil(buffer.remaining() / 4)) * 4;
		old.limit(intBound);
		buffer.limit(intBound); // set buffers to be the same size

		IntBuffer diffInts = pool.obtainIntBuffer(buffer.limit()); // great overestimation

		// check block of size int
		int numBits = intBound / 4;
		int numBytes = (numBits - 1) / 8 + 1;
		byte[] flag = BufferPool.Default.obtainBytes(numBytes);

		// also works if old and new are not the same size, but less efficiently
		int i = 0;
		while (buffer.remaining() >= 4) {
			int val = buffer.getInt();
			if (old.remaining() < 4 || val != old.getInt()) {
				diffInts.put(val);
				flag[i / 8] |= 1 << (i % 8);
			}
			i++;
		}

		diffInts.flip();

		/* Check what is smaller, delta message or original buffer */
		Object retMessage = null;
		
		// TODO: fix numbers to be more accurate
		if (alwaysSendDiff || diffInts.remaining() * 4 + 8 < buffer.limit()) {
			int diffDataSize = diffInts.remaining();
			int[] diffData = pool.obtainInts(diffDataSize, true);
			diffInts.get(diffData, 0, diffDataSize);

			retMessage = DiffMessage.Pool.obtain().set(prevID, flag, diffData);
		}
		else {
			retMessage = message;
		}
		
		pool.saveByteBuffer(old);
		pool.saveByteBuffer(buffer);
		pool.saveIntBuffer(diffInts);
		
		return retMessage;
	}
}
