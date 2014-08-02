package net.namekdev.quakemonkey.diff;

import java.util.ArrayList;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class MessageMultiplexer {
	private ArrayList<Listener> listeners = new ArrayList<Listener>();

	public MessageMultiplexer() {
	}

	public void addMessageListener(Listener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeMessageListener(Listener listener) {
		listeners.remove(listener);
	}

	public void dispatch(Connection source, Object message) {
		for (Listener listener : listeners) {
			listener.received(source, message);
		}
	}
}
