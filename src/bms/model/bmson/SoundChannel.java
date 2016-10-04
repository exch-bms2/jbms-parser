package bms.model.bmson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)

// sound channel.
public class SoundChannel {
	public String name; // as sound file name
	public Note[] notes; // as notes using this sound.
}
