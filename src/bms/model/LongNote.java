package bms.model;

/**
 * ロングノート
 * 
 * @author exch
 */
public class LongNote extends Note {

	/**
	 * ロングノート開始点
	 */
	private TimeLine start;
	/**
	 * ロングノート終了点
	 */
	private TimeLine end;
	/**
	 * 終端の状態
	 */
	private int endstate;
	
	/**
	 * 指定のTimeLineを始点としたロングノートを作成する
	 * @param start
	 */
	public LongNote(int wav,TimeLine start) {
		this.start = start;
		this.setWav(wav);
	}
	
	public LongNote(int wav,int starttime, TimeLine start) {
		this.start = start;
		this.setStarttime(starttime);
		this.setWav(wav);
	}
	
	/**
	 * ロングノートの終点を設定する
	 * @param time
	 */
	public void setEnd(TimeLine time) {
		end = time;
		this.setDuration(end.getTime() - start.getTime());
	}
	
	/**
	 * ロングノートの始点を取得する
	 * @return
	 */
	public TimeLine getStart() {
		return start;
	}
	
	/**
	 * ロングノートの終点を取得する
	 * @return
	 */
	public TimeLine getEnd() {
		return end;
	}

	public int getEndstate() {
		return endstate;
	}

	public void setEndstate(int endstate) {
		this.endstate = endstate;
	}
}
