package net.namekdev.quakemonkey.diff;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import net.namekdev.quakemonkey.diff.messages.AckMessage;
import net.namekdev.quakemonkey.diff.messages.DiffMessage;
import net.namekdev.quakemonkey.diff.messages.LabeledMessage;
import net.namekdev.quakemonkey.diff.utils.BufferPool;

/**
 * Handles the dispatching of messages of type {@code T} to clients, using a
 * protocol of delta messages.
 * <p>
 * Important: make sure that you call
 * {@link DiffClassRegistration#registerClasses()} before starting the server.
 * 
 * @author Ben Ruijl
 * 
 * @param <T>
 *            Message type
 * @see #ClientDiffHandler
 */
public class ServerDiffHandler<T> extends Listener {
	protected static final Logger log = Logger.getLogger(ServerDiffHandler.class.getName());
	private final Kryo kryoSerializer;
	private final short numHistory;
	private final Map<Connection, DiffConnection<T>> connectionSnapshots;
	private final boolean alwaysSendDiffs;

	public ServerDiffHandler(Server server, short numHistory, boolean alwaysSendDiffs) {
		this.kryoSerializer = server.getKryo();
		this.numHistory = numHistory;
		this.alwaysSendDiffs = alwaysSendDiffs;
		connectionSnapshots = new HashMap<Connection, DiffConnection<T>>();

		server.addListener(this);
	}
	
	public ServerDiffHandler(Server server, short numHistory) {
		this(server, numHistory, false);
	}

	public ServerDiffHandler(Server server, boolean alwaysSendDiffs) {
		this(server, (short) 20, alwaysSendDiffs);
	}
	
	public ServerDiffHandler(Server server) {
		this(server, false);
	}

	/**
	 * Dispatches a message to all clients in the filter.
	 */
	public void dispatchMessage(Server server, Connection[] connections, T message) {
		for (Connection connection : server.getConnections()) {
			if (Utils.arrayContainsRef(connections, connection)) {
				if (!connectionSnapshots.containsKey(connection)) {
					connectionSnapshots.put(connection, new DiffConnection<T>(kryoSerializer, numHistory, alwaysSendDiffs));
				}

				DiffConnection<T> diffConnection = connectionSnapshots.get(connection);
				LabeledMessage newMessage = diffConnection.generateSnapshot(message);
				server.sendToUDP(connection.getID(), newMessage);
				
				// Everything back to pools
				if (newMessage.getMessage() instanceof DiffMessage) {
					DiffMessage diffMessage = (DiffMessage) newMessage.getMessage();
					
					BufferPool.Default.saveBytes(diffMessage.getFlag());
					BufferPool.Default.saveInts(diffMessage.getData());
					DiffMessage.Pool.free(diffMessage);
				}
				LabeledMessage.Pool.free(newMessage);
			}
		}
	}

	/**
	 * Returns the lag in terms of how many messages sent to the client haven't
	 * been acknowledged. If the connection does not exist, for example because
	 * no messages have been sent yet, 0 is returned.
	 * 
	 * @param conn
	 *            Connection to client
	 * @return Connection lag
	 */
	public int getLag(Connection conn) {
		if (!connectionSnapshots.containsKey(conn)) {
			if (log.isLoggable(Level.WARNING)) {
				log.log(Level.WARNING, "Trying to get lag of connection that does not exist (yet).");
			}
			return 0;
		}

		return connectionSnapshots.get(conn).getLag();
	}

	@Override
	public void connected(Connection connection) {
	}

	@Override
	public void disconnected(Connection connection) {
		connectionSnapshots.remove(connection);
	}

	@Override
	public void received(Connection source, Object m) {
		if (m instanceof AckMessage && connectionSnapshots.containsKey(source)) {
			DiffConnection<T> diffConnection = connectionSnapshots.get(source);
			diffConnection.registerAck(((AckMessage) m).getId());
		}
	}
}
