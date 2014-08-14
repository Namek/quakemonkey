package net.namekdev.quakemonkey.diff.messages;

import net.namekdev.quakemonkey.diff.utils.Pool;

/**
 * A message containing another message with an identifier.
 * 
 * @author Ben Ruijl
 */
public class LabeledMessage {
	public static final Pool<LabeledMessage> Pool = new Pool<LabeledMessage>(new Pool.ObjectServicer<LabeledMessage>() {
		@Override
		public LabeledMessage onCreate() {
			return new LabeledMessage();
		}

		@Override
		public void onGet(LabeledMessage obj) {
		}

		@Override
		public void onFree(LabeledMessage obj) {
			obj.label = 0;
			obj.message = null;
		}
	});
	
	private short label;
	private Object message;
	
	public LabeledMessage() {
	}

	public LabeledMessage(short label, Object message) {
		this.label = label;
		this.message = message;
	}

	public short getLabel() {
		return label;
	}

	public Object getMessage() {
		return message;
	}
	
	/**
	 * Method existing only for pooling.
	 */
	public LabeledMessage set(short label, Object message) {
		this.label = label;
		this.message = message;
		
		return this;
	}
}
