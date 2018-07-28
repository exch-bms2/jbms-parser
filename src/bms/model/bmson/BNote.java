package bms.model.bmson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)

public class BNote extends BMSONObject {
	public int id; // as it is.
	public int[] id_set; // as it is.
	public String condition;
	public int interval;
}
