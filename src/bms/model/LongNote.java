package bms.model;

/**
 * ロングノート
 * 
 * @author exch
 */
public class LongNote extends Note {
	
	private boolean end;
	
	private LongNote pair;
	
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
	
	public LongNote(int wav,long starttime, long duration) {
		this.setWav(wav);
		this.setStarttime(starttime);
		this.setDuration(duration);
	}
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	protected void setPair(LongNote pair) {
		pair.pair = this;
		this.pair = pair;
		
		pair.end = pair.getSection() > this.getSection();
		this.end = !pair.end;
		type = pair.type = (type != TYPE_UNDEFINED ? type : pair.type);
	}
	
	public LongNote getPair() {
		return pair;
	}
	
	public boolean isEnd() {
		return end;
	}

	@Override
	public Object clone() {		
		return clone(true);
	}
	
	private Object clone(boolean copypair) {
		LongNote ln = (LongNote) super.clone();
		if(copypair) {
			ln.setPair((LongNote) pair.clone(false));
		}
		return ln;
	}
	
}
