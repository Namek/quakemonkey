package net.namekdev.quakemonkey.diff.utils;

import java.util.Map.Entry;
import java.util.TreeMap;

public class BufferPool {
	private static final TreeMap<Integer, byte[]> _byteArrayPool = new TreeMap<Integer, byte[]>();
	private static final TreeMap<Integer, int[]> _intArrayPool = new TreeMap<Integer, int[]>();
	
	public static byte[] obtainBytes(int minimumSize) {
		return obtainBytes(minimumSize, false);
	}
	
	public static byte[] obtainBytes(int size, boolean exactSize) {
		synchronized (_byteArrayPool) {
			Entry<Integer, byte[]> arrayEntry = exactSize ? null : _byteArrayPool.higherEntry(size - 1);
			byte[] array = exactSize ? _byteArrayPool.get(size) : null;
			
			if (arrayEntry != null) {
				array = arrayEntry.getValue();
				_byteArrayPool.remove(arrayEntry);
			}
			else if (array == null) {
				array = new byte[size];
			}
			
			assert(array != null);
			return array;
		}
	}
	
	public static void saveBytes(byte[] array) {
		synchronized (_byteArrayPool) {
			_byteArrayPool.put(array.length, array);
		}
	}
	
	public static int[] obtainInts(int size) {
		return obtainInts(size, false);
	}
	
	public static int[] obtainInts(int size, boolean exactSize) {
		synchronized (_intArrayPool) {
			Entry<Integer, int[]> arrayEntry = exactSize ? null : _intArrayPool.higherEntry(size - 1);
			int[] array = exactSize ? _intArrayPool.get(size) : null;
			
			if (arrayEntry != null) {
				array = arrayEntry.getValue();
				_intArrayPool.remove(arrayEntry);
			}
			else if (array == null) {
				array = new int[size];
			}
			
			assert(array != null);
			return array;
		}
	}
	
	public static void saveInts(int[] array) {
		synchronized (_intArrayPool) {
			_intArrayPool.put(array.length, array);
		}
	}
}
