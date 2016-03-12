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
	
	private int start;
	
	private int duration;
	
	private int state;

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

}
