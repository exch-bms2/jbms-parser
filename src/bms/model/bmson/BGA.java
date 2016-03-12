package bms.model.bmson;

// BGA.
public class BGA {
	public BGAHeader[] bga_header;  // picture id and filename
	public BNote[] bga_events; // as notes using this sound.
	public BNote[] layer_events; // as notes using this sound.
	public BNote[] poor_events; // as notes using this sound.
	
}
