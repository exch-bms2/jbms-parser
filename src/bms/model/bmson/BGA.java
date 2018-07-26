package bms.model.bmson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)

// BGA.
public class BGA {
	public BGAHeader[] bga_header;  // picture id and filename
	public BGASequence[] bga_sequence;  // picture id and filename
	public BNote[] bga_events; // as notes using this sound.
	public BNote[] layer_events; // as notes using this sound.
	public BNote[] poor_events; // as notes using this sound.
	
	public BGAHeader[] getBgaHeader() {
		return bga_header;
	}
	public void setBgaHeader(BGAHeader[] bga_header) {
		this.bga_header = bga_header;
	}
	
	public BNote[] getBgaNotes() {
		return bga_events;
	}
	
	public void setBgaNotes(BNote[] bga_events) {
		this.bga_events = bga_events;
	}

	public BNote[] getLayerNotes() {
		return layer_events;
	}
	
	public void setLayerNotes(BNote[] layer_events) {
		this.layer_events = layer_events;
	}

	public BNote[] getPoorNotes() {
		return poor_events;
	}
	
	public void setPoorNotes(BNote[] poor_events) {
		this.poor_events = poor_events;
	}
}
