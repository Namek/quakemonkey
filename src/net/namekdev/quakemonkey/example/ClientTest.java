package net.namekdev.quakemonkey.example;

import java.io.IOException;

import net.namekdev.quakemonkey.diff.ClientDiffHandler;
import net.namekdev.quakemonkey.diff.DiffClassRegistration;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

/**
 * An example client that shows how the snapshot network code works.
 * 
 * @author Ben Ruijl
 * 
 */
public class ClientTest extends Listener {
	final ClientDiffHandler<GameStateMessage> diffHandler;
	
	public ClientTest() throws IOException {
		Client kryoClient = new Client();
		
		Kryo kryo = kryoClient.getKryo();
		DiffClassRegistration.registerClasses(kryo);
		kryo.register(GameStateMessage.class, new GameStateMessage.GameStateSerializer());
		
		kryoClient.start();
		kryoClient.connect(1000, "localhost", 6143, 6143);
		
		diffHandler = new ClientDiffHandler<GameStateMessage>(kryoClient, GameStateMessage.class, (short)30);
		diffHandler.addListener(this); // register listener for GameStateMessage
		
		
		try {
			Thread.sleep(10000000);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		kryoClient.close();
	}
	
	public static void main(String[] args) throws IOException {
		new ClientTest();
	}
	
	@Override
	public void connected(Connection connection) {
		System.out.println("Connected to server.");
	}

	@Override
	public void disconnected(Connection connection) {
		System.out.println("Disconnected from server.");
	}

	@Override
	public void received(Connection source, Object message) {
		if (message instanceof GameStateMessage) {
			// do something with the message
			GameStateMessage gsMessage = (GameStateMessage) message;
			System.out.println("Client #" + source.getID() + " received: '"
					+ gsMessage.getName() + ", " + gsMessage.getPosition()
					+ ", " + gsMessage.getOrientation() + "'");
		}
	}
}
