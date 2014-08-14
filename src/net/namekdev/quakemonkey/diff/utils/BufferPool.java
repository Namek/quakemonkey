package net.namekdev.quakemonkey.diff.utils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class BufferPool {
	public static final BufferPool Default = new BufferPool();
	
	private final DuplicatedKeysTreeMap<Integer, byte[]> _byteArrayPool = new DuplicatedKeysTreeMap<Integer, byte[]>();
	private final DuplicatedKeysTreeMap<Integer, int[]> _intArrayPool = new DuplicatedKeysTreeMap<Integer, int[]>();
	private final DuplicatedKeysTreeMap<Integer, ByteBuffer> _byteBufferPool = new DuplicatedKeysTreeMap<Integer, ByteBuffer>(false);
	private final DuplicatedKeysTreeMap<Integer, IntBuffer> _intBufferPool = new DuplicatedKeysTreeMap<Integer, IntBuffer>(false);
	
	
	public byte[] obtainBytes(int minimumSize) {
		return obtainBytes(minimumSize, false);
	}
	
	public byte[] obtainBytes(int size, boolean exactSize) {
		synchronized (_byteArrayPool) {
			byte[] array = exactSize ? _byteArrayPool.poll(size) : _byteArrayPool.pollCeiling(size);
			
			if (array == null) {
				array = new byte[size];
			}

			assert(array != null);
			return array;
		}
	}
	
	public void saveBytes(byte[] array) {
		synchronized (_byteArrayPool) {
			_byteArrayPool.put(array.length, array);
		}
	}
	
	public int[] obtainInts(int size) {
		return obtainInts(size, false);
	}
	
	public int[] obtainInts(int size, boolean exactSize) {
		synchronized (_intArrayPool) {
			int[] array = exactSize ? _intArrayPool.poll(size) : _intArrayPool.pollCeiling(size);

			if (array == null) {
				array = new int[size];
			}
			
			assert(array != null);
			return array;
		}
	}
	
	public void saveInts(int[] array) {
		synchronized (_intArrayPool) {
			_intArrayPool.put(array.length, array);
		}
	}
	
	public ByteBuffer obtainByteBuffer(int minimumSize) {
		return obtainByteBuffer(minimumSize, false);
	}

	public ByteBuffer obtainByteBuffer(int size, boolean exactSize) {
		synchronized (_byteBufferPool) {
			ByteBuffer buffer = exactSize ? _byteBufferPool.poll(size) : _byteBufferPool.pollCeiling(size);
			
			if (buffer == null) {
				buffer = ByteBuffer.allocate(size);
			}
			
			assert(buffer != null);
			return buffer;
		}
	}
	
	public void saveByteBuffer(ByteBuffer buffer) {
		synchronized (_byteBufferPool) {
			buffer.clear();
			_byteBufferPool.put(buffer.capacity(), buffer);
		}
	}
	
	public IntBuffer obtainIntBuffer(int minimumSize) {
		return obtainIntBuffer(minimumSize, false);
	}
	
	public IntBuffer obtainIntBuffer(int size, boolean exactSize) {
		synchronized (_intBufferPool) {
			IntBuffer buffer = exactSize ? _intBufferPool.poll(size) : _intBufferPool.pollCeiling(size);
			
			if (buffer == null) {
				buffer = IntBuffer.allocate(size);
			}
			
			assert(buffer != null);
			return buffer;
		}
	}
	
	public void saveIntBuffer(IntBuffer buffer) {
		synchronized (_intBufferPool) {
			buffer.clear();
			_intBufferPool.put(buffer.capacity(), buffer);
		}
	}
}
