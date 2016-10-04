package bms.model.bmson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)

public class BpmEvent {
	public int y; // as locate( 240BPM,1sec = 960 )
	public double bpm; // as value. Meaning of value depends on Channel.
}
