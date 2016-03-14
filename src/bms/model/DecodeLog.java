package bms.model;

/**
 * 
 * 
 * @author exch
 */
public class DecodeLog {

	private final String message;
	
	private final int state;
	
	public static final int STATE_WARNING = 1;
	public static final int STATE_ERROR = 2;
	
	public DecodeLog(int state, String message) {
		this.message = message;
		this.state = state;
	}
	
	public int getState() {
		return state;
	}
	
	public String getMessage() {
		return message;
	}
}
