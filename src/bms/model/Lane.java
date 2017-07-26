package bms.model;

import java.util.*;

public class Lane {

	private Note[] notes;
	private int notebasepos;
	private int noteseekpos;

	private Note[] hiddens;
	private int hiddenbasepos;
	private int hiddenseekpos;

	public Lane(BMSModel model, int lane) {
		Collection<Note> note = new ArrayDeque<Note>();
		Collection<Note> hnote = new ArrayDeque<Note>();
		for (TimeLine tl : model.getAllTimeLines()) {
			if (tl.existNote(lane)) {
				note.add(tl.getNote(lane));
			}
			if (tl.getHiddenNote(lane) != null) {
				hnote.add(tl.getHiddenNote(lane));
			}
		}
		notes = note.toArray(new Note[note.size()]);
		hiddens = hnote.toArray(new Note[hnote.size()]);
	}

	public Note[] getNotes() {
		return notes;
	}

	public Note[] getHiddens() {
		return hiddens;
	}

	public Note getNote() {
		if (noteseekpos < notes.length) {
			return notes[noteseekpos++];
		}
		return null;
	}

	public Note getHidden() {
		if (hiddenseekpos < hiddens.length) {
			return hiddens[hiddenseekpos++];
		}
		return null;
	}
	
	public void reset() {
		noteseekpos = notebasepos;		
		hiddenseekpos = hiddenbasepos;
	}

	public void mark(int time) {
		for (; notebasepos < notes.length - 1 && notes[notebasepos + 1].getTime() < time; notebasepos++)
			;
		for (; notebasepos > 0 && notes[notebasepos].getTime() > time; notebasepos--)
			;
		noteseekpos = notebasepos;
		for (; hiddenbasepos < hiddens.length - 1
				&& hiddens[hiddenbasepos + 1].getTime() < time; hiddenbasepos++)
			;
		for (; hiddenbasepos > 0 && hiddens[hiddenbasepos].getTime() > time; hiddenbasepos--)
			;
		hiddenseekpos = hiddenbasepos;
	}
}
