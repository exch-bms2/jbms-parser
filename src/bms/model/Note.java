package bms.model;

import java.util.ArrayList;
import java.util.List;

/**
 * ノート
 * 
 * @author exch
 */
public abstract class Note implements Cloneable {
	
	/**
	 * ノートが配置されている小節
	 */
	private float section;
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

	private List<Note> notes = new ArrayList();

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
		return (int) start;
	}

	public void setStarttime(long start) {
		this.start = start;
	}

	public int getDuration() {
		return (int) duration;
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

	public float getSection() {
		return section;
	}

	public void setSection(float section) {
		this.section = section;
	}

	public int getTime() {
		return (int) time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public void addLayeredNote(Note n) {
		notes.add(n);
	}

	public List<Note> getLayeredNotes() {
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
