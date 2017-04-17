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
	 * タイムライン上に配置されている演奏レーン分(+フリースクラッチ)のノート。配置されていないレーンにはnullを入れる。
	 */
	private Note[] notes;

//	public static final int[] NOTEASSIGN_BEAT = { 0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15, 16 };
//	public static final int[] NOTEASSIGN_POPN = { 0, 1, 2, 3, 4, 10, 11, 12, 13 };
//	public static final int[] NOTEASSIGN_KB_24KEY = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
//			26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51 };
	/**
	 * タイムライン上に配置されている演奏レーン分(+フリースクラッチ)の不可視ノート。配置されていないレーンにはnullを入れる。
	 */
	private Note[] hiddennotes;
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

	public TimeLine(float section, int time, int notesize) {
		this.section = section;
		this.time = time;
		notes = new Note[notesize];
		hiddennotes = new Note[notesize];
	}

	public int getTime() {
		return time;
	}

	protected void setTime(int time) {
		this.time = time;
		for(Note n : notes) {
			if(n != null) {
				n.setSectiontime(time);
			}
		}
		for(Note n : hiddennotes) {
			if(n != null) {
				n.setSectiontime(time);
			}
		}
		for(Note n : bgnotes) {
			n.setSectiontime(time);
		}
	}

	public int getLaneCount() {
		return notes.length;
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
		for (Note note : notes) {
			if (note != null) {
				if (note instanceof LongNote) {
					final LongNote ln = (LongNote) note;
					if (ln.getType() == LongNote.TYPE_CHARGENOTE || ln.getType() == LongNote.TYPE_HELLCHARGENOTE
							|| (ln.getType() == LongNote.TYPE_UNDEFINED && lntype != BMSModel.LNTYPE_LONGNOTE)
							|| ln.getSection() == section) {
						count++;
					}
				} else if (note instanceof NormalNote) {
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
		if(note == null) {
			return;
		}
		if(note instanceof LongNote && ((LongNote)note).getSection() != 0f && ((LongNote)note).getSection() != section) {
			((LongNote)note).getEndnote().setSection(section);
			((LongNote)note).getEndnote().setSectiontime(time);			
		} else {
			note.setSection(section);
			note.setSectiontime(time);
		}
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
		return bgnotes.toArray(new Note[bgnotes.size()]);
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
		for(Note n : notes) {
			if(n != null) {
				if(n instanceof LongNote && n.getSection() != this.section) {
					((LongNote)n).getEndnote().setSection(section);
				} else {
					n.setSection(section);					
				}
			}
		}
		for(Note n : hiddennotes) {
			if(n != null) {
				n.setSection(section);					
			}
		}
		for(Note n : bgnotes) {
			n.setSection(section);
		}
		this.section = section;
	}

	public int getStop() {
		return stop;
	}

	public void setStop(int stop) {
		this.stop = stop;
	}
}