package net.namekdev.quakemonkey.diff;

import java.nio.ByteBuffer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

class Utils {
	public static ByteBuffer messageToBuffer(Object message, ByteBuffer target, Kryo kryoSerializer) {
		// Could let the caller pass their own in       
        ByteBuffer buffer = target == null ? ByteBuffer.allocate( 32767 + 2 ) : target;
        
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
	    for (final T el : array) {
	        if (el == key) {
	            return true;
	        }
	    }
	    return false;
	}
}
