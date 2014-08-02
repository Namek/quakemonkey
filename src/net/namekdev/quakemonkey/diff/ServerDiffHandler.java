package net.namekdev.quakemonkey.diff;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import net.namekdev.quakemonkey.diff.messages.AckMessage;

/*
import com.jme3.network.AbstractMessage;
import com.jme3.network.ConnectionListener;
import com.jme3.network.Filter;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Server;
*/

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
	protected static final Logger log = Logger
			.getLogger(ServerDiffHandler.class.getName());
	private final short numHistory;
	private final Map<Connection, DiffConnection<T>> connectionSnapshots;

	public ServerDiffHandler(Server server, short numHistory) {
		this.numHistory = numHistory;
		connectionSnapshots = new HashMap<Connection, DiffConnection<T>>();

		server.addListener(this);
	}

	public ServerDiffHandler(Server server) {
		this(server, (short) 20);
	}

	/**
	 * Dispatches a message to all clients in the filter. If it is more
	 * efficient to send a delta message, this is sent instead.
	 * 
	 * @param server
	 *            The server that should send the message
	 * @param message
	 *            The message to be sent
	 */
	public void dispatchMessage(Server server, Connection[] connections, T message) {
		for (Connection connection : server.getConnections()) {
			if (arrayContains(connections, connection)) {
				if (!connectionSnapshots.containsKey(connection)) {
					connectionSnapshots.put(connection, new DiffConnection<T>(numHistory));
				}

				Object newMessage = connectionSnapshots.get(connection).generateSnapshot(message);
				server.sendToUDP(connection.getID(), newMessage);
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
			log.log(Level.WARNING,
					"Trying to get lag of connection that does not exist (yet).");
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
			connectionSnapshots.get(source).registerAck(((AckMessage) m).getId());
		}
	}

	private static <T> boolean arrayContains(final T[] array, final T key) {
	    for (final T el : array) {
	        if (el == key) {
	            return true;
	        }
	    }
	    return false;
	}
}
