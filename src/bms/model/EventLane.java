package bms.model;

import java.util.ArrayDeque;
import java.util.Collection;

public class EventLane {

	private TimeLine[] sections;
	private int sectionbasepos;
	private int sectionseekpos;

	private TimeLine[] bpms;
	private int bpmbasepos;
	private int bpmseekpos;

	private TimeLine[] stops;
	private int stopbasepos;
	private int stopseekpos;

	public EventLane(BMSModel model) {
		Collection<TimeLine> section = new ArrayDeque<TimeLine>();
		Collection<TimeLine> bpm = new ArrayDeque<TimeLine>();
		Collection<TimeLine> stop = new ArrayDeque<TimeLine>();
		
		TimeLine prev = null;
		for (TimeLine tl : model.getAllTimeLines()) {
			if (tl.getSectionLine()) {
				section.add(tl);
			}
			if (tl.getBPM() != (prev != null ? prev.getBPM() : model.getBpm())) {
				bpm.add(tl);
			}
			if (tl.getStop() != 0) {
				stop.add(tl);
			}
			prev = tl;
		}
		sections = section.toArray(new TimeLine[section.size()]);
		bpms = bpm.toArray(new TimeLine[bpm.size()]);
		stops = stop.toArray(new TimeLine[stop.size()]);
	}

	public TimeLine[] getSections() {
		return sections;
	}

	public TimeLine[] getBpmChanges() {
		return bpms;
	}
	
	public TimeLine[] getStops() {
		return stops;
	}
	
	public TimeLine getSection() {
		if (sectionseekpos < sections.length) {
			return sections[sectionseekpos++];
		}
		return null;
	}

	public TimeLine getBpm() {
		if (bpmseekpos < bpms.length) {
			return bpms[bpmseekpos++];
		}
		return null;
	}
	
	public TimeLine getStop() {
		if (stopseekpos < stops.length) {
			return stops[stopseekpos++];
		}
		return null;
	}
	
	public void reset() {
		sectionseekpos = sectionbasepos;
		bpmseekpos = bpmbasepos;
		stopseekpos = stopbasepos;
	}

	public void mark(int time) {
		for (; sectionbasepos < sections.length - 1 && sections[sectionbasepos + 1].getTime() > time; sectionbasepos++)
			;
		for (; sectionbasepos > 0 && sections[sectionbasepos].getTime() < time; sectionbasepos--)
			;
		for (; bpmbasepos < bpms.length - 1 && bpms[bpmbasepos + 1].getTime() > time; bpmbasepos++)
			;
		for (; bpmbasepos > 0 && bpms[bpmbasepos].getTime() < time; bpmbasepos--)
			;
		for (; stopbasepos < stops.length - 1 && stops[stopbasepos + 1].getTime() > time; stopbasepos++)
			;
		for (; stopbasepos > 0 && stops[stopbasepos].getTime() < time; stopbasepos--)
			;
		sectionseekpos = sectionbasepos;
		bpmseekpos = bpmbasepos;
		stopseekpos = stopbasepos;
	}
	

}
