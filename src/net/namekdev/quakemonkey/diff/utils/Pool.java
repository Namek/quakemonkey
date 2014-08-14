package net.namekdev.quakemonkey.diff.utils;

import java.util.PriorityQueue;
import java.util.Queue;

public class Pool<T> {
	private final Queue<T> _bag;
	private ObjectServicer<T> _objServicer;
	
	public Pool(ObjectServicer<T> objServicer) {
		_bag = new PriorityQueue<T>(127);
		_objServicer = objServicer;
	}
	
	public T obtain() {
		synchronized (_bag) {
			T item = _bag.poll();
			
			if (item == null) {
				item = _objServicer.onCreate();
			}
			_objServicer.onGet(item);
			
			return item;
		}
	}
	
	public void free(T obj) {
		assert(obj != null);
		synchronized (_bag) {
			_objServicer.onFree(obj);
			_bag.add(obj);
		}
	}
	
	public static interface ObjectServicer<T> {
		public T onCreate();
		public void onGet(T obj);
		public void onFree(T obj);
	}
}
