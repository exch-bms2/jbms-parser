package bms.model;

import java.util.Arrays;

/**
 * int key / double value に特化した小さな sorted map。
 * BMSDecoder の定義テーブル参照で boxing と Map.Entry 生成を避けるために使う。
 */
public final class IntDoubleTreeMap {

	private int[] keys;
	private double[] values;
	private int size;

	public IntDoubleTreeMap() {
		this(128);
	}

	public IntDoubleTreeMap(int capacity) {
		keys = new int[capacity];
		values = new double[capacity];
	}

	public void clear() {
		size = 0;
	}

	public void put(int key, double value) {
		int index = indexOf(key);
		if(index >= 0) {
			values[index] = value;
			return;
		}

		index = -index - 1;
		ensureCapacity(size + 1);
		if(index < size) {
			System.arraycopy(keys, index, keys, index + 1, size - index);
			System.arraycopy(values, index, values, index + 1, size - index);
		}
		keys[index] = key;
		values[index] = value;
		size++;
	}

	public int indexOf(int key) {
		int low = 0;
		int high = size - 1;
		while(low <= high) {
			int mid = (low + high) >>> 1;
			int midKey = keys[mid];
			if(midKey < key) {
				low = mid + 1;
			} else if(midKey > key) {
				high = mid - 1;
			} else {
				return mid;
			}
		}
		return -(low + 1);
	}

	public double valueAt(int index) {
		return values[index];
	}

	public int size() {
		return size;
	}

	private void ensureCapacity(int capacity) {
		if(capacity <= keys.length) {
			return;
		}
		int newCapacity = Math.max(capacity, keys.length * 2);
		keys = Arrays.copyOf(keys, newCapacity);
		values = Arrays.copyOf(values, newCapacity);
	}
}
