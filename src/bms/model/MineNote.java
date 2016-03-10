package bms.model;

/**
 * 地雷ノート
 * 
 * @author exch
 */
public class MineNote extends Note {
	
	/**
	 * 地雷のダメージ量
	 */
	private int damage;
	
	public MineNote(int wav, int damage) {
		this.setWav(wav);
		this.setDamage(damage);
	}

	/**
	 * 地雷ノーツのダメージ量を取得する
	 * @return 地雷ノーツのダメージ量
	 */
	public int getDamage() {
		return damage;
	}

	/**
	 * 地雷ノーツのダメージ量を設定する
	 * @param damage 地雷ノーツのダメージ量
	 */
	public void setDamage(int damage) {
		this.damage = damage;
	}
	
}