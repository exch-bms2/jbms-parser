package bms.model;

/**
 * プレイモード
 * 
 * @author exch
 */
public enum Mode {

	BEAT_5K(5, "beat-5k", 1, 6, new int[] { 5 }), 
	BEAT_7K(7, "beat-7k", 1, 8, new int[] { 7 }), 
	BEAT_10K(10, "beat-10k", 2, 12, new int[] { 5, 11 }), 
	BEAT_14K(14, "beat-14k", 2, 16, new int[] { 7, 15 }), 
	POPN_5K(9, "popn-5k", 1, 5, new int[] {}),
	POPN_9K(9, "popn-9k", 1, 9, new int[] {}),
	KEYBOARD_24K(25, "keyboard-24k", 1, 26, new int[] { 24, 25 }), 
	KEYBOARD_24K_DOUBLE(50, "keyboard-24k-double", 2, 52, new int[] { 24, 25, 50, 51 }),
	;

	public final int id;
	/**
	 * モードの名称。bmsonのmode_hintに対応
	 */
	public final String hint;
	/**
	 * プレイヤー数
	 */
	public final int player;
	/**
	 * 使用するキーの数
	 */
	public final int key;
	/**
	 * スクラッチキーアサイン
	 */
	public final int[] scratchKey;

	private Mode(int id, String hint, int player, int key, int[] scratchKey) {
		this.id = id;
		this.hint = hint;
		this.player = player;
		this.key = key;
		this.scratchKey = scratchKey;
	}

	/**
	 * 指定するkeyがスクラッチキーかどうかを返す
	 * 
	 * @param key キー番号
	 * @return スクラッチであればtrue
	 */
	public boolean isScratchKey(int key) {
		for (int sc : scratchKey) {
			if (key == sc) {
				return true;
			}
		}
		return false;
	}

	/**
	 * mode_hintに対応するModeを取得する
	 * 
	 * @param hint
	 *            mode_hint
	 * @return 対応するMode
	 */
	public static Mode getMode(String hint) {
		for (Mode mode : values()) {
			if (mode.hint.equals(hint)) {
				return mode;
			}
		}
		return null;
	}
}
