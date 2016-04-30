package bms.model;

import java.util.*;

/**
 * タイムライン
 * 
 * @author exch
 */
public class TimeLine {
	/**
	 * タイムラインの時間(ms)
	 */
	private int time;
	/**
	 * タイムラインの小節
	 */
	private float section;
	/**
	 * タイムライン上に配置されている16レーン分(+フリースクラッチ)のノート。配置されていないレーンにはnullを入れる。
	 */
	private Note[] notes = new Note[18];
	/**
	 * タイムライン上に配置されている16レーン分(+フリースクラッチ)の不可視ノート。配置されていないレーンにはnullを入れる。
	 */
	private Note[] hiddennotes = new Note[18];
	/**
	 * タイムライン上に配置されているBGMノート
	 */
	private List<Note> bgnotes = new ArrayList<Note>();
	/**
	 * 小節線の有無
	 */
	private boolean sectionLine = false;
	/**
	 * タイムライン上からのBPM変化
	 */
	private double bpm;
	/**
	 * ストップ時間(ms)
	 */
	private int stop;
	/**
	 * 表示するBGAのID
	 */
	private int bga = -1;
	/**
	 * 表示するレイヤーのID
	 */
	private int layer = -1;
	/**
	 * POORレイヤー
	 */
	private int[] poor;

	public TimeLine(int time) {
		this.time = time;
	}

	public int getTime() {
		return time;
	}

	/**
	 * タイムライン上の総ノート数を返す
	 * 
	 * @return
	 */
	public int getTotalNotes() {
		return getTotalNotes(BMSModel.LNTYPE_LONGNOTE);
	}

	/**
	 * タイムライン上の総ノート数を返す
	 * 
	 * @return
	 */
	public int getTotalNotes(int lntype) {
		int count = 0;
		for (int i = 0; i < notes.length; i++) {
			if (notes[i] != null) {
				if (notes[i] instanceof LongNote) {
					if (lntype != BMSModel.LNTYPE_LONGNOTE || (((LongNote) notes[i])).getStart() == this) {
						count++;
					}
				} else if (notes[i] instanceof NormalNote) {
					count++;
				}
			}
		}
		return count;
	}

	public boolean existNote() {
		for (Note n : notes) {
			if (n != null) {
				return true;
			}
		}
		return false;
	}

	public boolean existNote(int lane) {
		return notes[lane] != null;
	}

	public Note getNote(int lane) {
		return notes[lane];
	}

	public void setNote(int lane, Note note) {
		notes[lane] = note;
	}

	public void setHiddenNote(int lane, Note note) {
		hiddennotes[lane] = note;
	}

	public boolean existHiddenNote() {
		for (Note n : hiddennotes) {
			if (n != null) {
				return true;
			}
		}
		return false;
	}

	public Note getHiddenNote(int lane) {
		return hiddennotes[lane];
	}

	public void addBackGroundNote(Note note) {
		bgnotes.add(note);
	}

	public void removeBackGroundNote(Note note) {
		bgnotes.remove(note);
	}

	public Note[] getBackGroundNotes() {
		return bgnotes.toArray(new Note[0]);
	}

	public void setBPM(double bpm) {
		this.bpm = bpm;
	}

	public double getBPM() {
		return bpm;
	}

	public void setSectionLine(boolean section) {
		this.sectionLine = section;
	}

	public boolean getSectionLine() {
		return sectionLine;
	}

	/**
	 * 表示するBGAのIDを取得する
	 * 
	 * @return BGAのID
	 */
	public int getBGA() {
		return bga;
	}

	/**
	 * 表示するBGAのIDを設定する
	 * 
	 * @param bga
	 *            BGAのID
	 */
	public void setBGA(int bga) {
		this.bga = bga;
	}

	/**
	 * 表示するレイヤーBGAのIDを取得する
	 * 
	 * @return レイヤーBGAのID
	 */
	public int getLayer() {
		return layer;
	}

	public void setLayer(int layer) {
		this.layer = layer;
	}

	public int[] getPoor() {
		return poor;
	}

	public void setPoor(int[] poor) {
		this.poor = poor;
	}

	public float getSection() {
		return section;
	}

	public void setSection(float section) {
		this.section = section;
	}

	public int getStop() {
		return stop;
	}

	public void setStop(int stop) {
		this.stop = stop;
	}
}