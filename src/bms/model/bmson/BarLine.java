package bms.model.bmson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)

// event note
public class BarLine {
	public int y; // as locate( 240BPM,1sec = 960 )
	public int k; // as kind.
}
