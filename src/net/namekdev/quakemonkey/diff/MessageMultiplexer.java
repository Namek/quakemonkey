package net.namekdev.quakemonkey.diff;

import java.util.ArrayList;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class MessageMultiplexer {
	private Connection _connection;
	private ArrayList<Listener> _listeners = new ArrayList<Listener>();

	public MessageMultiplexer(Connection connection) {
		_connection = connection;
	}

	public void addMessageListener(Listener listener) {
		if (!_listeners.contains(listener)) {
			_listeners.add(listener);
		}
	}

	public void removeMessageListener(Listener listener) {
		_listeners.remove(listener);
	}

	public void dispatch(Object object) {
		for (Listener listener : _listeners) {
			listener.received(_connection, object);
		}
	}
}
