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
	 * ノートが配置されている時間(us)
	 */
	private long time;

	/**
	 * アサインされている 音源ID
	 */
	private int wav;
	/**
	 * 音源IDの音の開始時間(us)
	 */
	private long start;
	/**
	 * 音源IDの音を鳴らす長さ(us)
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
	/**
	 * 同時演奏されるノート
	 */
	private Note[] layerednotes = EMPTYARRAY;

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

	public long getMilliStarttime() {
		return start / 1000;
	}

	public long getMicroStarttime() {
		return start;
	}

	public void setMicroStarttime(long start) {
		this.start = start;
	}

	public long getMilliDuration() {
		return duration / 1000;
	}

	public long getMicroDuration() {
		return duration;
	}

	public void setMicroDuration(long duration) {
		this.duration = duration;
	}

	public int getPlayTime() {
		return (int) (playtime / 1000);
	}

	public long getMilliPlayTime() {
		return playtime / 1000;
	}

	public long getMicroPlayTime() {
		return playtime;
	}

	public void setPlayTime(int playtime) {
		this.playtime = playtime * 1000;
	}

	public void setMicroPlayTime(long playtime) {
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
	
	public long getMilliTime() {
		return time / 1000;
	}

	public long getMicroTime() {
		return time;
	}

	public void setMicroTime(long time) {
		this.time = time;
	}

	public void addLayeredNote(Note n) {
		if(n == null) {
			return;
		}
		n.setSection(section);
		n.setMicroTime(time);
		layerednotes = Arrays.copyOf(layerednotes, layerednotes.length + 1);
		layerednotes[layerednotes.length - 1] = n;
	}

	public Note[] getLayeredNotes() {
		return layerednotes;
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
