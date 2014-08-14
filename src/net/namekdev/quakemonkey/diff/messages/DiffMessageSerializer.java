package net.namekdev.quakemonkey.diff.messages;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Serializes a delta message efficiently.
 * 
 * @author Ben Ruijl
 */
public class DiffMessageSerializer extends Serializer<DiffMessage> {

	@Override
	public DiffMessage read(Kryo kryo, Input input, Class<DiffMessage> type) {
		short messageID = input.readShort();
		int flagSize = input.readShort();
		
		byte[] flags = new byte[flagSize];
		input.readBytes(flags, 0, flagSize);
		
		int intCount = 0;
		for (int i = 0; i < 8 * flagSize; i++) {
			if ((flags[i / 8] & (1 << (i % 8))) != 0) {
				intCount++;
			}
		}

		int[] val = input.readInts(intCount);
		
		return DiffMessage.Pool.obtain().set(messageID, flags, val);
	}

	@Override
	public void write(Kryo kryo, Output output, DiffMessage diff) {
		output.writeShort(diff.getMessageId());
		output.writeShort((short) diff.getFlag().length);
		output.write(diff.getFlag());
		output.writeInts(diff.getData());
		output.setPosition(output.position() + diff.getData().length * 4);
	}

}
