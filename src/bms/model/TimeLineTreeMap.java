package bms.model;

import java.util.Arrays;

/**
 * TimeLineEntry に特化した double key の sorted map。
 * BMSDecoder の TimeLineEntry 参照で Double boxing と Map.Entry 生成を避けるために使う。
 */
public final class TimeLineTreeMap {

	private double[] keys;
	private TimeLineEntry[] values;
	private int size;

	public TimeLineTreeMap() {
		this(4096);
	}

	public TimeLineTreeMap(int capacity) {
		keys = new double[capacity];
		values = new TimeLineEntry[capacity];
	}

	public void clear() {
		Arrays.fill(values, 0, size, null);
		size = 0;
	}

	public void put(double key, double time, TimeLine timeline) {
		put(key, new TimeLineEntry(time, timeline));
	}

	private void put(double key, TimeLineEntry value) {
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

	public TimeLineEntry get(double key) {
		int index = indexOf(key);
		return index >= 0 ? values[index] : null;
	}

	public int indexOf(double key) {
		int low = 0;
		int high = size - 1;
		while(low <= high) {
			int mid = (low + high) >>> 1;
			int compare = Double.compare(keys[mid], key);
			if(compare < 0) {
				low = mid + 1;
			} else if(compare > 0) {
				high = mid - 1;
			} else {
				return mid;
			}
		}
		return -(low + 1);
	}

	public int lowerIndex(double key) {
		int index = indexOf(key);
		if(index >= 0) {
			return index - 1;
		}
		return -index - 2;
	}

	public double keyAt(int index) {
		return keys[index];
	}

	public TimeLineEntry valueAt(int index) {
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

	public static class TimeLineEntry {
		
		public final double time;
		public final TimeLine timeline;
		
		public TimeLineEntry(double time, TimeLine timeline) {
			this.time = time;
			this.timeline = timeline;
		}
	}
}
