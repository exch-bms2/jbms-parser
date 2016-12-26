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
	private int sectiontime;

	/**
	 * アサインされている 音源ID
	 */
	private int wav;
	/**
	 * 音源IDの音の開始時間
	 */
	private int start;
	/**
	 * 音源IDの音を鳴らす長さ
	 */
	private int duration;
	/**
	 * ノーツの状態
	 */
	private int state;
	/**
	 * ノーツの演奏時間
	 */
	private int time;

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
		return start;
	}

	public void setStarttime(int start) {
		this.start = start;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public float getSection() {
		return section;
	}

	public void setSection(float section) {
		this.section = section;
	}

	public int getSectiontime() {
		return sectiontime;
	}

	public void setSectiontime(int sectiontime) {
		this.sectiontime = sectiontime;
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
