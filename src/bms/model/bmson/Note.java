package bms.model.bmson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)

public class Note  extends BMSONObject {
	/**
	 * レーン番号(BGM:0 1P 1-8 2P 11-18 ?)
	 */
	public int x;
	/**
	 * ノーツの長さ( 0:normal note 1- : long note)
	 */
	public int l;
	/**
	 * 鳴らしている音源の続きから再生するかどうか
	 */
	public boolean c;
	
	public int t; // as type
	
	public boolean up = false;
}
