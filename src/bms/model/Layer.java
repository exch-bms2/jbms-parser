package bms.model;

public class Layer {
	
	public static final Layer[] EMPTY = new Layer[0];

	public final Event event;
	
	public final Sequence[][] sequence;
	
	public Layer(Event event, Sequence[][] sequence) {
		this.event = event;
		this.sequence = sequence;
	}

	public static class Event {
		public final EventType type;
		public final int interval;
		
		public Event(EventType type, int interval) {
			this.type = type;
			this.interval = interval;
		}
	}
	
	public enum EventType {
		ALWAYS,PLAY,MISS
	}
	
	public static class Sequence {
		
		public static final int END = Integer.MIN_VALUE;

		public final long time;
		public final int id;
		
		public Sequence(long time) {
			this.time = time;
			this.id = END;
		}
		
		public Sequence(long time, int id) {
			this.time = time;
			this.id = id;
		}
		
		public boolean isEnd() {
			return id == END;
		}
	}
}
