package bms.model.bmson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)

public class BMSInfo {
	public String title = ""; // as it is.
	public String subtitle = ""; // self-explanatory
	public String genre = ""; // as it is.
	public String artist = ""; // as it is.
	public String[] subartists = {}; // ["key:value"]
	public String mode_hint = "beat-7k"; // layout hints, e.g. "beat-7k",
											// "popn-5k", "generic-nkeys"
	public String chart_name = ""; // e.g. "HYPER", "FOUR DIMENSIONS"
	public int judge_rank = 100; // as defined standard judge width is 100
	public double total = 100; // as it is.
	public double init_bpm; // as it is
	public int level; // as it is?

	public String back_image = ""; // background image filename
	public String eyecatch_image = ""; // eyecatch image filename
	public String banner_image = ""; // banner image filename
	public String preview_music = ""; // preview music filename
	public int resolution = 240; // pulses per quarter note

    public int ln_type;        // LN type

	public int getJudgeRank() {
		return judge_rank;
	}
	
	public void setJudgeRank(int value) {
		judge_rank = value;
	}
	
	public double getInitBPM() {
		return init_bpm;
	}
	
	public void setInitBPM(double bpm) {
		init_bpm = bpm;
	}
}
