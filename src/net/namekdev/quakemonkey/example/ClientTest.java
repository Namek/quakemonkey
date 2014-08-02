package net.namekdev.quakemonkey.example;

import java.io.IOException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;


/*
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.network.Network;
import com.jme3.network.serializing.Serializer;
*/
import net.namekdev.quakemonkey.diff.ClientDiffHandler;
import net.namekdev.quakemonkey.diff.DiffClassRegistration;

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
		kryo.register(GameStateMessage.class);
		
		kryoClient.connect(10000000, "localhost", 6143);
		
		diffHandler = new ClientDiffHandler<GameStateMessage>(kryoClient, GameStateMessage.class, (short)30);
		diffHandler.addListener(this); // register listener for GameStateMessage
		
		kryoClient.start();
		
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
