package net.namekdev.quakemonkey.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import net.namekdev.quakemonkey.diff.DiffClassRegistration;
import net.namekdev.quakemonkey.diff.ServerDiffHandler;

/*
import com.jme3.network.ConnectionListener;
import com.jme3.network.Filters;
import com.jme3.network.HostedConnection;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializer;
*/


/**
 * An example server that shows how the snapshot network code works.
 * 
 * @author Ben Ruijl
 * 
 */
public class ServerTest {
	int serverTicks = 0;
	final Server kryoServer;
	final ServerDiffHandler<GameStateMessage> diffHandler;

	public ServerTest() throws IOException, InterruptedException {
		kryoServer = new Server();
		Kryo kryo = kryoServer.getKryo();
		
		DiffClassRegistration.registerClasses(kryo);
		kryo.register(GameStateMessage.class);

		diffHandler = new ServerDiffHandler<GameStateMessage>(kryoServer);
		kryoServer.bind(6143, 6143);
		kryoServer.start();

		kryoServer.addListener(new Listener() {
			@Override
			public void connected(Connection connection) {
				System.out.println("Connection closed from " + connection.getRemoteAddressTCP());
			}

			@Override
			public void disconnected(Connection connection) {
				System.out.println("New connection from " + connection.getRemoteAddressTCP());
			}
			
		});
		
		List<Float> or = Arrays.asList(new Float[] { 0.5f, 0.6f, 0.7f });

		while (true) {
			if (kryoServer.getConnections().length > 0) {
				List<Float> newPos = new ArrayList<Float>(
					Arrays.asList(new Float[] { (float) serverTicks, 8.0f, 3.0f })
				);

				GameStateMessage newMessage = new GameStateMessage("test", newPos, or, (byte) 0);

				/* Dispatch same message to all clients */
				diffHandler.dispatchMessage(kryoServer,	kryoServer.getConnections(), newMessage);

				// send a message that is old (id=1), see what happens
				// myServer.broadcast(new LabeledMessage((short)1, newMessage));

				/* Check if the connection is lagging badly */
				for (Connection conn : kryoServer.getConnections()) {
					if (diffHandler.getLag(conn) > 20) {
						System.out.println("Client " + conn.getRemoteAddressUDP() + " is lagging badly.");
					}
				}
			}
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}

			serverTicks++;
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		new ServerTest();
	}
}
