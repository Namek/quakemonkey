package net.namekdev.quakemonkey.diff.utils;

import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;

class DuplicatedKeysTreeMap<TKey, TValue> {
	private final TreeMap<TKey, Queue<TValue>> _byteArrayPool = new TreeMap<TKey, Queue<TValue>>();
	
	/**
	 * Set to @{code false} if the same buffer size is often used.
	 */
	public boolean shouldRemoveSubBagsWhenEmpty = true;
	
	
	public DuplicatedKeysTreeMap() {
		this(true);
	}

	public DuplicatedKeysTreeMap(boolean shouldRemoveSubBagsWhenEmpty) {
		this.shouldRemoveSubBagsWhenEmpty = shouldRemoveSubBagsWhenEmpty;
	}

	public TValue poll(TKey key) {
		Queue<TValue> bag = _byteArrayPool.get(key);
		TValue retValue = null;
		
		if (bag != null) {
			retValue = bag.poll();
			
			if (shouldRemoveSubBagsWhenEmpty && bag.isEmpty()) {
				_byteArrayPool.remove(key);
			}
		}
		
		return retValue;
	}

	public TValue pollCeiling(TKey minimumKey) {
		Entry<TKey, Queue<TValue>> entry = _byteArrayPool.ceilingEntry(minimumKey);
		TValue retValue = null;
		
		if (entry != null) {
			Queue<TValue> bag = entry.getValue();
			retValue = bag.poll();
			
			if (shouldRemoveSubBagsWhenEmpty && bag.isEmpty()) {
				_byteArrayPool.remove(entry.getKey());
			}
		}
		
		return retValue;
	}

	public void put(TKey key, TValue value) {
		Queue<TValue> bag = _byteArrayPool.get(key);
		
		if (bag == null) {
			bag = new PriorityQueue<TValue>();
			_byteArrayPool.put(key, bag);
		}
		
		bag.add(value);
	}
	
}