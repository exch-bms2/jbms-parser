package bms.model;

/**
 * 通常ノート
 * 
 * @author exch
 */
public class NormalNote extends Note {
	
	public NormalNote(int wav) {
		this.setWav(wav);
	}
	
	public NormalNote(int wav, int start, int duration) {
		this.setWav(wav);
		this.setStarttime(start);
		this.setDuration(duration);
	}
	
}