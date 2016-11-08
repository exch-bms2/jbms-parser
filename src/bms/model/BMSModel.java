package bms.model;

import java.util.*;

/**
 * BMSモデル
 * 
 * @author exch
 */
public class BMSModel implements Comparable {

	/**
	 * BMSのファイルパス
	 */
	private String path;
	/**
	 * プレイヤー数
	 */
	private int player;
	/**
	 * 使用するキー数
	 */
	private int usekeys;
	/**
	 * タイトル名
	 */
	private String title = "";
	/**
	 * サブタイトル名
	 */
	private String subTitle = "";
	/**
	 * ジャンル名
	 */
	private String genre = "";
	/**
	 * アーティスト
	 */
	private String artist = "";
	/**
	 * サブアーティスト
	 */
	private String subartist = "";

	/**
	 * バナー
	 */
	private String banner = "";
	/**
	 * ステージ画像
	 */
	private String stagefile = "";
	private String backbmp = "";
	/**
	 * 標準BPM
	 */
	private double bpm;
	/**
	 * 表記レベル
	 */
	private String playlevel = "";
	/**
	 * 表記ランク(0:beginner, 1:normal, 2:hyper, 3:another, 4:insane)
	 */
	private int difficulty = 0;
	/**
	 * 判定ランク
	 */
	private int judgerank = 4;
	/**
	 * TOTAL値
	 */
	private double total;
	/**
	 * 標準ボリューム
	 */
	private int volwav;
	/**
	 * MD5値
	 */
	private String md5 = "";
	/**
	 * SHA256値
	 */
	private String sha256 = "";
	/**
	 * WAV定義のIDとファイル名のマップ
	 */
	private String[] wavmap = new String[0];
	/**
	 * BGA定義のIDとファイル名のマップ
	 */
	private String[] bgamap = new String[0];

	private int lntype;

	public static final int LNTYPE_LONGNOTE = 0;
	public static final int LNTYPE_CHARGENOTE = 1;
	public static final int LNTYPE_HELLCHARGENOTE = 2;

	/**
	 * 時間とTimeLineのマッピング
	 */
	private Map<Float, TimeLine> timelines = new TreeMap<Float, TimeLine>();

	private int[] random;

	public static final int TOTALNOTES_ALL = 0;
	public static final int TOTALNOTES_KEY = 1;
	public static final int TOTALNOTES_LONG_KEY = 2;
	public static final int TOTALNOTES_SCRATCH = 3;
	public static final int TOTALNOTES_LONG_SCRATCH = 4;
	public static final int TOTALNOTES_MINE = 5;

	public BMSModel() {
	}

	public int getPlayer() {
		return player;
	}

	public void setPlayer(int player) {
		this.player = player;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		if (title == null) {
			this.title = "";
			return;
		}
		this.title = title;
	}

	public String getSubTitle() {
		return subTitle;
	}

	public void setSubTitle(String subTitle) {
		if (subTitle == null) {
			this.subTitle = "";
			return;
		}
		this.subTitle = subTitle;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		if (genre == null) {
			this.genre = "";
			return;
		}
		this.genre = genre;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		if (artist == null) {
			this.artist = "";
			return;
		}
		this.artist = artist;
	}

	public String getSubArtist() {
		return subartist;
	}

	public void setSubArtist(String artist) {
		if (artist == null) {
			this.subartist = "";
			return;
		}
		this.subartist = artist;
	}

	public void setBanner(String banner) {
		if (banner == null) {
			this.banner = "";
			return;
		}
		this.banner = banner;
	}

	public String getBanner() {
		return banner;
	}

	public double getBpm() {
		return bpm;
	}

	public void setBpm(double bpm) {
		;
		this.bpm = bpm;
	}

	public String getPlaylevel() {
		return playlevel;
	}

	public void setPlaylevel(String playlevel) {
		this.playlevel = playlevel;
	}

	public int getJudgerank() {
		return judgerank;
	}

	public void setJudgerank(int judgerank) {
		this.judgerank = judgerank;
	}

	public double getTotal() {
		return total;
	}

	public void setTotal(double total) {
		this.total = total;
	}

	public int getVolwav() {
		return volwav;
	}

	public void setVolwav(int volwav) {
		this.volwav = volwav;
	}

	public double getMinBPM() {
		double bpm = this.getBpm();
		for (TimeLine time : timelines.values()) {
			final double d = time.getBPM();
			bpm = (bpm <= d) ? bpm : d;
		}
		return bpm;
	}

	public double getMaxBPM() {
		double bpm = this.getBpm();
		for (TimeLine time : timelines.values()) {
			final double d = time.getBPM();
			bpm = (bpm >= d) ? bpm : d;
		}
		return bpm;
	}

	/**
	 * 総ノート数を返す。
	 * 
	 * @return 総ノート数
	 */
	public int getTotalNotes() {
		return this.getTotalNotes(0, Integer.MAX_VALUE);
	}

	public int getTotalNotes(int type) {
		return this.getTotalNotes(0, Integer.MAX_VALUE, type);
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
	public int getTotalNotes(int start, int end) {
		return this.getTotalNotes(start, end, TOTALNOTES_ALL);
	}

	private static final int[] BMS_NORMALLANE = { 0, 1, 2, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14, 15 };
	private static final int[] BMS_NORMALLANE_1P = { 0, 1, 2, 3, 4, 5, 6 };
	private static final int[] BMS_NORMALLANE_2P = { 9, 10, 11, 12, 13, 14, 15 };
	private static final int[] PMS_NORMALLANE = { 0, 1, 2, 3, 4, 10, 11, 12, 13 };
	private static final int[] BMS_SCRATCHLANE = { 7, 16 };
	private static final int[] BMS_SCRATCHLANE_1P = { 7 };
	private static final int[] BMS_SCRATCHLANE_2P = { 16 };
	private static final int[] PMS_SCRATCHLANE = {};

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
	public int getTotalNotes(int start, int end, int type) {
		return this.getTotalNotes(start, end, type, 0);
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
	public int getTotalNotes(int start, int end, int type, int side) {
		int[] nlane = BMS_NORMALLANE;
		int[] slane = BMS_SCRATCHLANE;
		if (getUseKeys() == 9) {
			nlane = PMS_NORMALLANE;
			slane = PMS_SCRATCHLANE;
		} else if (side == 1) {
			nlane = BMS_NORMALLANE_1P;
			slane = BMS_SCRATCHLANE_1P;
		} else if (side == 2) {
			nlane = BMS_NORMALLANE_2P;
			slane = BMS_SCRATCHLANE_2P;
		}

		int count = 0;
		for (TimeLine tl : this.getAllTimeLines()) {
			if (tl.getTime() >= start && tl.getTime() < end) {
				switch (type) {
				case TOTALNOTES_ALL:
					count += tl.getTotalNotes(lntype);
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
									|| (ln.getType() == LongNote.TYPE_UNDEFINED && lntype != LNTYPE_LONGNOTE)
									|| ln.getSection() == tl.getSection()) {
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
						if (tl.existNote(lane) && (tl.getNote(lane) instanceof LongNote)) {
							LongNote ln = (LongNote) tl.getNote(lane);
							if (ln.getType() == LongNote.TYPE_CHARGENOTE
									|| ln.getType() == LongNote.TYPE_HELLCHARGENOTE
									|| (ln.getType() == LongNote.TYPE_UNDEFINED && lntype != LNTYPE_LONGNOTE)
									|| ln.getSection() == tl.getSection()) {
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

	public double getAverageNotesPerTime(int start, int end) {
		return (double) this.getTotalNotes(start, end) * 1000 / (end - start);
	}

	public double getMaxNotesPerTime(int range) {
		Integer[] times = timelines.keySet().toArray(new Integer[0]);
		Arrays.sort(times);

		int maxnotes = 0;
		for (int i = 0; i < times.length; i++) {
			int notes = 0;
			for (int j = i; times[j] < times[i] + range; j++) {
				notes += timelines.get(times[j]).getTotalNotes(lntype);
			}
			maxnotes = (maxnotes < notes) ? notes : maxnotes;
		}
		return maxnotes;
	}

	public TimeLine getTimeLine(float section, int time) {
		TimeLine tl = timelines.get(section);
		if (tl == null) {
			tl = new TimeLine(time);
			tl.setSection(section);
			timelines.put(section, tl);
		}
		return tl;
	}

	public TimeLine[] getAllTimeLines() {
		return timelines.values().toArray(new TimeLine[timelines.size()]);
	}

	public int[] getAllTimes() {
		TimeLine[] times = getAllTimeLines();
		int[] result = new int[times.length];
		for (int i = 0; i < times.length; i++) {
			result[i] = times[i].getTime();
		}
		return result;
	}

	public int getLastTime() {
		TimeLine[] times = getAllTimeLines();

		for (int i = times.length - 1; i > 0; i--) {
			for (int lane = 0; lane < 18; lane++) {
				if (times[i].existNote(lane) || times[i].getHiddenNote(lane) != null
						|| times[i].getBackGroundNotes().length > 0 || times[i].getBGA() != -1
						|| times[i].getLayer() != -1) {
					return times[i].getTime();
				}
			}
		}
		return 0;
	}

	public int getLastNoteTime() {
		TimeLine[] times = getAllTimeLines();
		for (int i = times.length - 1; i > 0; i--) {
			for (int lane = 0; lane < 18; lane++) {
				if (times[i].existNote(lane)) {
					return times[i].getTime();
				}
			}
		}
		return 0;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}

	public int compareTo(Object arg0) {
		return this.title.compareTo(((BMSModel) arg0).title);
	}

	public String getFullTitle() {
		return title + (subTitle != null && subTitle.length() > 0 ? " " + subTitle : "");
	}

	public String getFullArtist() {
		return artist + (subartist != null && subartist.length() > 0 ? " " + subartist : "");
	}

	public void setMD5(String hash) {
		this.md5 = hash;
	}

	public String getMD5() {
		return md5;
	}

	public String getSHA256() {
		return sha256;
	}

	public void setSHA256(String sha256) {
		this.sha256 = sha256;
	}

	public void setUseKeys(int keys) {
		usekeys = keys;
	}

	public int getUseKeys() {
		return usekeys;
	}

	public String[] getWavList() {
		return wavmap;
	}

	public void setWavList(String[] wavmap) {
		this.wavmap = wavmap;
	}

	public String[] getBgaList() {
		return bgamap;
	}

	public void setBgaList(String[] bgamap) {
		this.bgamap = bgamap;
	}

	public int[] getRandom() {
		return random;
	}

	public void setRandom(int[] random) {
		this.random = random;
	}

	public String getStagefile() {
		return stagefile;
	}

	public void setStagefile(String stagefile) {
		if (stagefile == null) {
			this.stagefile = "";
			return;
		}
		this.stagefile = stagefile;
	}

	public String getBackbmp() {
		return backbmp;
	}

	public void setBackbmp(String backbmp) {
		if (backbmp == null) {
			this.backbmp = "";
			return;
		}
		this.backbmp = backbmp;
	}

	public int getLntype() {
		return lntype;
	}

	public void setLntype(int lntype) {
		this.lntype = lntype;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean containsUndefinedLongNote() {
		for (TimeLine tl : this.getAllTimeLines()) {
			for (int i = 0; i < 18; i++) {
				if (tl.getNote(i) != null && tl.getNote(i) instanceof LongNote
						&& ((LongNote) tl.getNote(i)).getType() == LongNote.TYPE_UNDEFINED) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean containsLongNote() {
		for (TimeLine tl : this.getAllTimeLines()) {
			for (int i = 0; i < 18; i++) {
				if (tl.getNote(i) != null && tl.getNote(i) instanceof LongNote) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean containsMineNote() {
		for (TimeLine tl : this.getAllTimeLines()) {
			for (int i = 0; i < 18; i++) {
				if (tl.getNote(i) != null && tl.getNote(i) instanceof MineNote) {
					return true;
				}
			}
		}
		return false;
	}

}
