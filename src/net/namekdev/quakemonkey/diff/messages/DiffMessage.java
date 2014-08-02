package net.namekdev.quakemonkey.diff.messages;

/**
 * This message is used to send the byte-level difference of two messages to the
 * client.
 * 
 * @author Ben Ruijl
 * 
 */
public class DiffMessage {
	private short messageId; // ID of the message the diff is from
	private byte[] flag;
	private int[] data;

	public DiffMessage() {
	}

	public DiffMessage(short messageId, byte[] flag, int[] data) {
		this.messageId = messageId;
		this.data = data;
		this.flag = flag;
	}
	
	public short getMessageId() {
		return messageId;
	}

	public int[] getData() {
		return data;
	}

	public byte[] getFlag() {
		return flag;
	}
}
