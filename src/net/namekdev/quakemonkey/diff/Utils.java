package net.namekdev.quakemonkey.diff;

import java.nio.ByteBuffer;

import net.namekdev.quakemonkey.diff.utils.BufferPool;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

class Utils {
	public static ByteBuffer messageToBuffer(Object message, ByteBuffer target, Kryo kryoSerializer) {
		// Could let the caller pass their own in       
        ByteBuffer buffer = target == null ? BufferPool.Default.obtainByteBuffer(32767 + 2) : target;
        
        Output output = new Output(buffer.array());

        output.setPosition(2);
		kryoSerializer.writeClassAndObject(output, message);
		buffer.position(output.position());
		buffer.flip();
		short dataLength = (short)(buffer.remaining() - 2);
		buffer.putShort(dataLength);
		buffer.position(0);
		
		return buffer;
	}

	public static <T extends Object> boolean arrayContainsRef(final T[] array, final T key) {
		boolean foundKey = false;
		
	    for (int i = 0, n = array.length; i < n; ++i) {
	        if (array[i] == key) {
	            foundKey = true;
	            break;
	        }
	    }
	    return foundKey;
	}
}
