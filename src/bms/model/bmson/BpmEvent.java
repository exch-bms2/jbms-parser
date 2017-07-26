package bms.model.bmson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)

public class BpmEvent  extends BMSONObject {
	/**
	 * 変更するBPM
	 */
	public double bpm;
}
