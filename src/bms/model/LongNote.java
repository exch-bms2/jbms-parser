package bms.model;

/**
 * ロングノート
 * 
 * @author exch
 */
public class LongNote extends Note {
	
	private Note endnote = new NormalNote(-2);
	
	private int type;
	
	public static final int TYPE_UNDEFINED = 0;
	public static final int TYPE_LONGNOTE = 1;
	public static final int TYPE_CHARGENOTE = 2;
	public static final int TYPE_HELLCHARGENOTE = 3;
	
	/**
	 * 指定のTimeLineを始点としたロングノートを作成する
	 * @param start
	 */
	public LongNote(int wav) {
		this.setWav(wav);
	}
	
	public LongNote(int wav,int starttime) {
		this.setStarttime(starttime);
		this.setWav(wav);
	}
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Note getEndnote() {
		return endnote;
	}

	public void setEndnote(Note endnote) {
		this.endnote = endnote;
	}

	@Override
	public Object clone() {
		LongNote ln = (LongNote) super.clone();
		ln.endnote = (Note) endnote.clone();
		return ln;
	}
	
	
}
