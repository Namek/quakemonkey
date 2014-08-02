package net.namekdev.quakemonkey.diff.messages;

/**
 * A message containing another message with an identifier.
 * 
 * @author Ben Ruijl
 * 
 */
public class LabeledMessage {
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
}
