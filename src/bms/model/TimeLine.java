package bms.model;

import java.util.*;

/**
 * タイムライン
 * 
 * @author exch
 */
public class TimeLine {
	
	/**
	 * タイムラインの時間(us)
	 */
	private long time;
	/**
	 * タイムラインの小節
	 */
	private double section;
	/**
	 * タイムライン上に配置されている演奏レーン分のノート。配置されていないレーンにはnullを入れる。
	 */
	private Note[] notes;

	/**
	 * タイムライン上に配置されている演奏レーン分の不可視ノート。配置されていないレーンにはnullを入れる。
	 */
	private Note[] hiddennotes;
	/**
	 * タイムライン上に配置されているBGMノート
	 */
	private Note[] bgnotes = Note.EMPTYARRAY;
	/**
	 * 小節線の有無
	 */
	private boolean sectionLine = false;
	/**
	 * タイムライン上からのBPM変化
	 */
	private double bpm;
	/**
	 * ストップ時間(us)
	 */
	private long stop;
	/**
	 * スクロールスピード
	 */
	private double scroll = 1.0;
	
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
	private Layer[] eventlayer = Layer.EMPTY;

	public TimeLine(double section, long time, int notesize) {
		this.section = section;
		this.time = time;
		notes = new Note[notesize];
		hiddennotes = new Note[notesize];
	}

	public int getTime() {
		return (int) (time / 1000);
	}
	
	public long getMicroTime() {
		return time;
	}

	protected void setTime(long time) {
		this.time = time;
		for(Note n : notes) {
			if(n != null) {
				n.setTime(time);
			}
		}
		for(Note n : hiddennotes) {
			if(n != null) {
				n.setTime(time);
			}
		}
		for(Note n : bgnotes) {
			n.setTime(time);
		}
	}

	public int getLaneCount() {
		return notes.length;
	}
	
	protected void setLaneCount(int lanes) {
		if(notes.length != lanes) {
			Note[] newnotes = new Note[lanes];
			Note[] newhiddennotes = new Note[lanes];
			for(int i = 0;i < lanes;i++) {
				if(i < notes.length) {
					newnotes[i] = notes[i];
					newhiddennotes[i] = hiddennotes[i];
				}
			}
			notes = newnotes;
			hiddennotes = newhiddennotes;
		}
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
							|| !ln.isEnd()) {
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
		note.setSection(section);
		note.setTime(time);
	}

	public void setHiddenNote(int lane, Note note) {
		hiddennotes[lane] = note;
		if(note == null) {
			return;
		}
		note.setSection(section);
		note.setTime(time);
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
		if(note == null) {
			return;
		}
		note.setSection(section);
		note.setTime(time);
		bgnotes = Arrays.copyOf(bgnotes, bgnotes.length + 1);
		bgnotes[bgnotes.length - 1] = note;
	}

	public void removeBackGroundNote(Note note) {
		for(int i = 0;i < bgnotes.length;i++) {
			if(bgnotes[i] == note) {
				final Note[] newbg = new Note[bgnotes.length - 1];
				for(int j = 0, index = 0;j < bgnotes.length;j++) {
					if(i != j) {
						newbg[index] = bgnotes[j];
						index++;
					}
				}
				bgnotes = newbg;
				break;
			}
		}
	}

	public Note[] getBackGroundNotes() {
		return bgnotes;
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

	public Layer[] getEventlayer() {
		return eventlayer;
	}

	public void setEventlayer(Layer[] eventlayer) {
		this.eventlayer = eventlayer;
	}

	public double getSection() {
		return section;
	}

	public void setSection(double section) {
		for(Note n : notes) {
			if(n != null) {
				n.setSection(section);					
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
		return (int) (stop / 1000);
	}
	
	public long getMicroStop() {
		return stop;
	}

	public void setStop(long stop) {
		this.stop = stop;
	}
	
	public double getScroll() {
		return scroll;
	}

	public void setScroll(double scroll) {
		this.scroll = scroll;
	}
}