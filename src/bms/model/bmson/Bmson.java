package bms.model.bmson;

public class Bmson {
    public String      version;        // bmson version
	public BMSInfo info = new BMSInfo(); // as bmson informations.
	public BarLine[] lines = {}; // as line locates.
	public EventNote[] bpm_events = {}; // change BPM. value is BPM.
	public EventNote[] stop_events = {}; // Stop flow. value is StopTime.
	public SoundChannel[] sound_channels = {}; // as Note data.
	public BGA bga = new BGA(); // as BGA(movie) data.
}
