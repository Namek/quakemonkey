package net.namekdev.quakemonkey.diff;

import net.namekdev.quakemonkey.diff.messages.AckMessage;
import net.namekdev.quakemonkey.diff.messages.DiffMessage;
import net.namekdev.quakemonkey.diff.messages.DiffMessageSerializer;
import net.namekdev.quakemonkey.diff.messages.LabeledMessage;

import com.esotericsoftware.kryo.Kryo;

/**
 * Registers messages in the serializer that are required for the snapshot
 * protocol, for both the server and the client.
 * 
 * @author Ben Ruijl
 * 
 */
public class DiffClassRegistration {
	/**
	 * Registers the messages that are required for the snapshot protocol, for
	 * both the server and the client. Make <b>absolutely</b> sure that this function
	 * is called before creation of the server and the client and that the
	 * position in the code relative to other {@link Serializer.registerClass}
	 * calls is the same.
	 */
	public static void registerClasses(Kryo kryoSerializer) {
		kryoSerializer.register(DiffMessage.class, new DiffMessageSerializer());
		kryoSerializer.register(AckMessage.class);
		kryoSerializer.register(LabeledMessage.class);
	}
}
