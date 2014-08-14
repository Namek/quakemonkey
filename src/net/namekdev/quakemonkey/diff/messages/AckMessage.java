package net.namekdev.quakemonkey.diff.messages;

import net.namekdev.quakemonkey.diff.utils.Pool;

/**
 * An acknowledgment message that is sent from the client to the server. It
 * contains and identifier of the message that was received.
 * 
 * @author Ben Ruijl
 * 
 */
public class AckMessage {
	public static final Pool<AckMessage> Pool = new Pool<AckMessage>(new Pool.ObjectServicer<AckMessage>() {
		@Override
		public AckMessage onCreate() {
			return new AckMessage();
		}

		@Override
		public void onGet(AckMessage obj) {
		}

		@Override
		public void onFree(AckMessage obj) {
			obj.id = 0;
		}
	});
	
	private short id;

	public AckMessage() {
	}

	public AckMessage(short id) {
		this.id = id;
	}

	public short getId() {
		return id;
	}
	
	public AckMessage set(short id) {
		this.id = id;
		
		return this;
	}
}
