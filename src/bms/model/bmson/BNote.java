package bms.model.bmson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)

public class BNote {
	public int id; // as it is.
	public int y; // as locate( 240BPM,1sec = 960 )
}
