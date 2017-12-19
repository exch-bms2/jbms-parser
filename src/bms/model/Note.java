package bms.model;

import java.util.Arrays;

/**
 * ノート
 * 
 * @author exch
 */
public abstract class Note implements Cloneable {
	
	public static final Note[] EMPTYARRAY = new Note[0];
	/**
	 * ノートが配置されている小節
	 */
	private double section;
	/**
	 * ノートが配置されている時間
	 */
	private long time;

	/**
	 * アサインされている 音源ID
	 */
	private int wav;
	/**
	 * 音源IDの音の開始時間
	 */
	private long start;
	/**
	 * 音源IDの音を鳴らす長さ
	 */
	private long duration;
	/**
	 * ノーツの状態
	 */
	private int state;
	/**
	 * ノーツの演奏時間
	 */
	private long playtime;

	private Note[] notes = EMPTYARRAY;

	public int getWav() {
		return wav;
	}

	public void setWav(int wav) {
		this.wav = wav;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public int getStarttime() {
		return (int) (start / 1000);
	}
	
	public long getMicroStarttime() {
		return start;
	}

	public void setStarttime(long start) {
		this.start = start;
	}

	public int getDuration() {
		return (int) (duration / 1000);
	}
	
	public long getMicroDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public int getPlayTime() {
		return (int)playtime;
	}

	public void setPlayTime(long playtime) {
		this.playtime = playtime;
	}

	public double getSection() {
		return section;
	}

	public void setSection(double section) {
		this.section = section;
	}

	public int getTime() {
		return (int) (time / 1000);
	}
	
	public long getMicroTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public void addLayeredNote(Note n) {
		if(n == null) {
			return;
		}
		n.setSection(section);
		n.setTime(time);
		notes = Arrays.copyOf(notes, notes.length + 1);
		notes[notes.length - 1] = n;
	}

	public Note[] getLayeredNotes() {
		return notes;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
		}
		return null;
	}	
}
