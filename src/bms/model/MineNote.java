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
	private double damage;
	
	public MineNote(int wav, double damage) {
		this.setWav(wav);
		this.setDamage(damage);
	}

	/**
	 * 地雷ノーツのダメージ量を取得する
	 * @return 地雷ノーツのダメージ量
	 */
	public double getDamage() {
		return damage;
	}

	/**
	 * 地雷ノーツのダメージ量を設定する
	 * @param damage 地雷ノーツのダメージ量
	 */
	public void setDamage(double damage) {
		this.damage = damage;
	}
	
}