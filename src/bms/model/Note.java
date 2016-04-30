package bms.model;

/**
 * ノート
 * 
 * @author exch
 */
public abstract class Note {

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

}
