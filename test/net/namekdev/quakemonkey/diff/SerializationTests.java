package net.namekdev.quakemonkey.diff;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.namekdev.quakemonkey.diff.DiffClassRegistration;
import net.namekdev.quakemonkey.diff.messages.AckMessage;
import net.namekdev.quakemonkey.diff.messages.LabeledMessage;
import net.namekdev.quakemonkey.example.GameStateMessage;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class SerializationTests {
	/**
	 * Scenario: server sends first gamestate and client receives it correctly.
	 * No DiffMessage should be sent here.
	 */
	@Test
	public void firstMessageReceiveTest() {
		Client fakeClient = new FakeClient();
		Kryo kryoSerializer = fakeClient.getKryo();
		DiffClassRegistration.registerClasses(kryoSerializer);
		kryoSerializer.register(GameStateMessage.class);
		
		// First server side
		DiffConnection<GameStateMessage> diffConnection = new DiffConnection<GameStateMessage>(kryoSerializer, (short)20);
		List<Float> position = Arrays.asList(new Float[] { 0.5f, 0.6f, 0.7f });
		List<Float> orientation = Arrays.asList(new Float[] { 0f, 0f, 1f });
		byte id = (byte)1;
		GameStateMessage message = new GameStateMessage("nmk", position, orientation, id);
		
		final Object messageToSend = diffConnection.generateSnapshot(message);
		
		// Now client side
		ClientDiffHandler<GameStateMessage> clientDiffHandler = new ClientDiffHandler<GameStateMessage>(fakeClient, GameStateMessage.class, (short) 30);
		Object messageReceived = messageToSend;
		
		clientDiffHandler.addListener(new Listener() {
			@Override
			public void received(Connection connection, Object message) {
				assertTrue(message instanceof GameStateMessage);
				GameStateMessage messageReceived = (GameStateMessage) message;
				
				assertEquals(message, messageReceived);
			}
		});
		
		// will call listener above
		clientDiffHandler.received(fakeClient, messageReceived);
	}

	/**
	 * Scenario: server sends two gamestates and client acknowledges only first,
	 * then the third one is being sent and now client receives it.
	 * Test if diff makes all correct.
	 */
	@Test
	public void snapshotDiffTest() {
		final FakeClient fakeClient = new FakeClient();
		Kryo kryoSerializer = fakeClient.getKryo();
		DiffClassRegistration.registerClasses(kryoSerializer);
		kryoSerializer.register(GameStateMessage.class);
		
		DiffConnection<GameStateMessage> diffConnection = new DiffConnection<GameStateMessage>(kryoSerializer, (short)20, true);
		ClientDiffHandler<GameStateMessage> clientDiffHandler = new ClientDiffHandler<GameStateMessage>(fakeClient, GameStateMessage.class, (short) 30);
		//ServerDiffHandler<GameStateMessage> serverDiffHandler = new ServerDiffHandler<SerializationTests.GameStateMessage>(new FakeServer());
		
		// Server sends first gamestate
		String name = "nmk";
		List<Float> position = Arrays.asList(new Float[] { 0.5f, 0.6f, 0.7f });
		List<Float> orientation = Arrays.asList(new Float[] { 0f, 0f, 1f });
		byte id = (byte)1;
		GameStateMessage message = new GameStateMessage(name, position, orientation, id);
		
		// should be: serverDiffHandler.dispatchMessage(fakeServer, fakeServer.getConnections(), message);
		// but we'll do that directly
		LabeledMessage firstMessage = (LabeledMessage) diffConnection.generateSnapshot(message);
		
		// Client acknowledges first message.
		// should be: fakeClient.sendUDP(new AckMessage(firstMessage.getLabel()));
		clientDiffHandler.received(fakeClient, firstMessage);
		diffConnection.registerAck(firstMessage.getLabel());
		
		// Server sends second gamestate
		position = Arrays.asList((Float[])position.toArray());//clone
		orientation = Arrays.asList((Float[])orientation.toArray());
		orientation.set(0, 1f);
		message = new GameStateMessage("" + name, position, orientation, (byte)2);
		final Object secondMessage = diffConnection.generateSnapshot(message);
		
		// Now Client didn't receive or didn't acknowledge properly 2nd gamestate.
		// Server doesn't know what happened, so it should send delta based on 1st and 3rd gamestate. 
		
		// Server sends third gamestate
		position = Arrays.asList((Float[])position.toArray());//clone
		orientation = Arrays.asList((Float[])orientation.toArray());
		message = new GameStateMessage("" + name, position, orientation, (byte)3);
		final Object thirdMessage = diffConnection.generateSnapshot(message);
		
		// Client receives snapshot delta based on 1st and 3rd gamestate. 
		Object messageReceived = thirdMessage;
		
		clientDiffHandler.addListener(new Listener() {
			@Override
			public void received(Connection connection, Object message) {
				assertTrue(message instanceof GameStateMessage);
				GameStateMessage messageReceived = (GameStateMessage) message;
				
				assertEquals(message, messageReceived);
			}
		});
		
		// will call listener above
		clientDiffHandler.received(fakeClient, messageReceived);
	}
	
	/**
	 * Scenario: server sends two gamestates, but client acknowledges only the second one.
	 * Third gamestate is a delta to second one.
	 */
	@Test
	public void messageDeliverTest() {
		fail("not yet implemented");
	}
	
	/**
	 * Scenaro: application wants to send same gamestate twice.
	 * Second message shouldn't be sent.
	 */
	@Test
	public void sameMessagesTest() {
		fail("not yet implemented");
	}

	@DefaultSerializer(GameStateMessage.GameStateSerializer.class)
	private static class GameStateMessage {
		private String name;
		private List<Float> position;
		private List<Float> orientation;
		private byte id;
		
		public GameStateMessage() {
		}

		public GameStateMessage(String name, List<Float> position, List<Float> orientation, byte id) {
			this.name = name;
			this.position = position;
			this.orientation = orientation;
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public List<Float> getPosition() {
			return position;
		}

		public List<Float> getOrientation() {
			return orientation;
		}

		public byte getId() {
			return id;
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof GameStateMessage)) {
				return false;
			}
			GameStateMessage message = (GameStateMessage) other;
			
			return name.equals(message.name)
				&& position.equals(message.position)
				&& orientation.equals(message.orientation)
				&& id == message.id;
		}
		
		public static class GameStateSerializer extends Serializer<GameStateMessage> {
			@Override
			public GameStateMessage read(Kryo kryo, Input input, Class<GameStateMessage> cls) {
				String name = input.readString();
				List<Float> position = new ArrayList<Float>();
				List<Float> orientation = new ArrayList<Float>();
				byte id;
				
				int n = input.readShort();
				for (int i = 0; i < n; ++i) {
					position.add(input.readFloat());
				}
				
				n = input.readShort();
				for (int i = 0; i < n; ++i) {
					orientation.add(input.readFloat());
				}
				
				id = input.readByte();
				
				return new GameStateMessage(name, position, orientation, id);
			}

			@Override
			public void write(Kryo kryo, Output output, GameStateMessage object) {
				output.writeString(object.name);
				
				int n = object.position.size();
				output.writeShort(n);
				for (int i = 0; i < n; ++i) {
					output.writeFloat(object.position.get(i));
				}
				
				n = object.orientation.size();
				output.writeShort(n);
				for (int i = 0; i < n; ++i) {
					output.writeFloat(object.orientation.get(i));
				}
				
				output.writeByte(object.id);
			}
		}
	}
		
	private static class FakeClient extends Client {
		@Override
		public int sendUDP(Object object) {
			// please do NOTHING for a good test purpose.
			return 0;
		}

		@Override
		public void addListener(Listener listener) {
		}
	}
}
