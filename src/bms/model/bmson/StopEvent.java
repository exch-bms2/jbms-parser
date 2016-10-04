package bms.model.bmson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)

public class StopEvent {
	public int y; // as locate( 240BPM,1sec = 960 )
	public long duration; // as value. Meaning of value depends on Channel.
}
