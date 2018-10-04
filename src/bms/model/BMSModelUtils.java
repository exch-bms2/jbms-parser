package bms.model;

public class BMSModelUtils {

	public static void changeFrequency(BMSModel model, float freq) {
		model.setBpm(model.getBpm() * freq);
		for (TimeLine tl : model.getAllTimeLines()) {
			tl.setBPM(tl.getBPM() * freq);
			tl.setStop((long) (tl.getMicroStop() / freq));
			tl.setTime((long) (tl.getMicroTime() / freq));
		}
	}

	public static double getMaxNotesPerTime(BMSModel model, int range) {
		int maxnotes = 0;
		TimeLine[] tl = model.getAllTimeLines();
		for (int i = 0; i < tl.length; i++) {
			int notes = 0;
			for (int j = i; j < tl.length && tl[j].getTime() < tl[i].getTime() + range; j++) {
				notes += tl[j].getTotalNotes(model.getLntype());
			}
			maxnotes = (maxnotes < notes) ? notes : maxnotes;
		}
		return maxnotes;
	}
	
	public static void setStartNoteSection(BMSModel model, double startsection) {
		boolean existNote = false;
		for (TimeLine tl : model.getAllTimeLines()) {
			if(tl.getSection() >= startsection) {
				break;
			}
			if(tl.existNote()) {
				existNote = true;
				break;
			}
		}
		
		if(existNote) {
			double marginSection = 1.0;
			for(;marginSection < startsection; marginSection += 1.0);
			long marginTime = (long) (marginSection * 240000000 / model.getBpm());
			for (TimeLine tl : model.getAllTimeLines()) {
				tl.setSection(tl.getSection() + marginSection);
				tl.setTime(tl.getMicroTime() + marginTime);
			}
			
			TimeLine[] tl2 = new TimeLine[model.getAllTimeLines().length + 1];
			tl2[0] = new TimeLine(0, 0, model.getMode().key);
			tl2[0].setBPM(model.getBpm());
			for(int i = 1;i < tl2.length;i++) {
				tl2[i] = model.getAllTimeLines()[i - 1];
			}
			model.setAllTimeLine(tl2);
		}
	}
}
