package bms.model;

/**
 * 
 * 
 * @author exch
 */
public class DecodeLog {

	private final String message;
	
	private final State state;
	
	public DecodeLog(State state, String message) {
		this.message = message;
		this.state = state;
	}
	
	public State getState() {
		return state;
	}
	
	public String getMessage() {
		return message;
	}
	
	public enum State {
		INFO, WARNING, ERROR;
	}
}
