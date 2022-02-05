package bms.model;

public class BMSModelUtils {

	public static final int TOTALNOTES_ALL = 0;
	public static final int TOTALNOTES_KEY = 1;
	public static final int TOTALNOTES_LONG_KEY = 2;
	public static final int TOTALNOTES_SCRATCH = 3;
	public static final int TOTALNOTES_LONG_SCRATCH = 4;
	public static final int TOTALNOTES_MINE = 5;

	/**
	 * 総ノート数を返す。
	 * 
	 * @return 総ノート数
	 */
	public static int getTotalNotes(BMSModel model) {
		return getTotalNotes(model, 0, Integer.MAX_VALUE);
	}

	public static int getTotalNotes(BMSModel model, int type) {
		return getTotalNotes(model, 0, Integer.MAX_VALUE, type);
	}

	/**
	 * 指定の時間範囲の総ノート数を返す
	 * 
	 * @param start
	 *            開始時間(ms)
	 * @param end
	 *            終了時間(ms)
	 * @return 指定の時間範囲の総ノート数
	 */
	public static int getTotalNotes(BMSModel model, int start, int end) {
		return getTotalNotes(model, start, end, TOTALNOTES_ALL);
	}

	/**
	 * 指定の時間範囲、指定の種類のノートの総数を返す
	 * 
	 * @param start
	 *            開始時間(ms)
	 * @param end
	 *            終了時間(ms)
	 * @param type
	 *            ノートの種類
	 * @return 指定の時間範囲、指定の種類のの総ノート数
	 */
	public static int getTotalNotes(BMSModel model, int start, int end, int type) {
		return getTotalNotes(model, start, end, type, 0);
	}

	/**
	 * 指定の時間範囲、指定の種類、指定のプレイサイドのノートの総数を返す
	 * 
	 * @param start
	 *            開始時間(ms)
	 * @param end
	 *            終了時間(ms)
	 * @param type
	 *            ノートの種類
	 * @param side
	 *            プレイサイド(0:両方, 1:1P側, 2:2P側)
	 * @return 指定の時間範囲、指定の種類のの総ノート数
	 */
	public static int getTotalNotes(BMSModel model, int start, int end, int type, int side) {
		Mode mode = model.getMode();
		if(mode.player == 1 && side == 2) {
			return 0;
		}
		int[] slane = new int[mode.scratchKey.length / (side == 0 ? 1 : mode.player)];
		for(int i = (side == 2 ? slane.length: 0), index = 0;index < slane.length;i++) {
			slane[index] = mode.scratchKey[i];
			index++;
		}		
		int[] nlane = new int[(mode.key - mode.scratchKey.length) / (side == 0 ? 1 : mode.player)];
		for(int i = 0, index = 0;index < nlane.length;i++) {
			if(!mode.isScratchKey(i)) {
				nlane[index] = i;
				index++;				
			}
		}

		int count = 0;
		for (TimeLine tl : model.getAllTimeLines()) {
			if (tl.getTime() >= start && tl.getTime() < end) {
				switch (type) {
				case TOTALNOTES_ALL:
					count += tl.getTotalNotes(model.getLntype());
					break;
				case TOTALNOTES_KEY:
					for (int lane : nlane) {
						if (tl.existNote(lane) && (tl.getNote(lane) instanceof NormalNote)) {
							count++;
						}
					}
					break;
				case TOTALNOTES_LONG_KEY:
					for (int lane : nlane) {
						if (tl.existNote(lane) && (tl.getNote(lane) instanceof LongNote)) {
							LongNote ln = (LongNote) tl.getNote(lane);
							if (ln.getType() == LongNote.TYPE_CHARGENOTE
									|| ln.getType() == LongNote.TYPE_HELLCHARGENOTE
									|| (ln.getType() == LongNote.TYPE_UNDEFINED && model.getLntype() != BMSModel.LNTYPE_LONGNOTE)
									|| !ln.isEnd()) {
								count++;
							}
						}
					}
					break;
				case TOTALNOTES_SCRATCH:
					for (int lane : slane) {
						if (tl.existNote(lane) && (tl.getNote(lane) instanceof NormalNote)) {
							count++;
						}
					}
					break;
				case TOTALNOTES_LONG_SCRATCH:
					for (int lane : slane) {
						final Note n = tl.getNote(lane);
						if (n instanceof LongNote) {
							final LongNote ln = (LongNote) n;
							if (ln.getType() == LongNote.TYPE_CHARGENOTE
									|| ln.getType() == LongNote.TYPE_HELLCHARGENOTE
									|| (ln.getType() == LongNote.TYPE_UNDEFINED && model.getLntype() != BMSModel.LNTYPE_LONGNOTE)
									|| !ln.isEnd()) {
								count++;
							}
						}
					}
					break;
				case TOTALNOTES_MINE:
					for (int lane : nlane) {
						if (tl.existNote(lane) && (tl.getNote(lane) instanceof MineNote)) {
							count++;
						}
					}
					for (int lane : slane) {
						if (tl.existNote(lane) && (tl.getNote(lane) instanceof MineNote)) {
							count++;
						}
					}
					break;
				}
			}
		}
		return count;
	}

	public double getAverageNotesPerTime(BMSModel model, int start, int end) {
		return (double) this.getTotalNotes(model, start, end) * 1000 / (end - start);
	}

	public static void changeFrequency(BMSModel model, float freq) {
		model.setBpm(model.getBpm() * freq);
		for (TimeLine tl : model.getAllTimeLines()) {
			tl.setBPM(tl.getBPM() * freq);
			tl.setStop((long) (tl.getMicroStop() / freq));
			tl.setMicroTime((long) (tl.getMicroTime() / freq));
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
	
	public static long setStartNoteTime(BMSModel model, long starttime) {
		long marginTime = 0;
		for (TimeLine tl : model.getAllTimeLines()) {
			if(tl.getMilliTime() >= starttime) {
				break;
			}
			if(tl.existNote()) {
				marginTime = starttime - tl.getMilliTime();
				break;
			}
		}
		
		if(marginTime > 0) {
			double marginSection = marginTime * model.getAllTimeLines()[0].getBPM() / 240000;
			for (TimeLine tl : model.getAllTimeLines()) {
				tl.setSection(tl.getSection() + marginSection);
				tl.setMicroTime(tl.getMicroTime() + marginTime * 1000);
			}
			
			TimeLine[] tl2 = new TimeLine[model.getAllTimeLines().length + 1];
			tl2[0] = new TimeLine(0, 0, model.getMode().key);
			tl2[0].setBPM(model.getBpm());
			for(int i = 1;i < tl2.length;i++) {
				tl2[i] = model.getAllTimeLines()[i - 1];
			}
			model.setAllTimeLine(tl2);
		}
		
		return marginTime;
	}
}
