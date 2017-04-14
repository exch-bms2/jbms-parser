package bms.model;

/**
 * プレイモード
 * 
 * @author exch
 */
public enum Mode {
	
	BEAT_5K(5, "beat-5k"),
	BEAT_7K(7, "beat-7k"),
	BEAT_10K(10, "beat-10k"),
	BEAT_14K(14, "beat-14k"),
	POPN_5K(9, "popn-5k"),
	POPN_9K(9, "popn-9k"),
	KEYBOARD_24K(24, "keyboard-24k"),	
	;

	/**
	 * 使用するキーの数
	 */
	public final int keys;
	/**
	 * モードの名称。bmsonのmode_hintに対応
	 */
	public final String hint;

	private Mode(int keys, String hint) {
		this.keys = keys;
		this.hint = hint;
	}
}
