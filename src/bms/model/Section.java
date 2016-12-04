package bms.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 小節
 * 
 * @author exch
 */
public class Section {

	public static final int LANE_AUTOPLAY = 1;
	public static final int SECTION_RATE = 2;
	public static final int BPM_CHANGE = 3;
	public static final int BGA_PLAY = 4;
	public static final int POOR_PLAY = 6;
	public static final int LAYER_PLAY = 7;
	public static final int BPM_CHANGE_EXTEND = 8;
	public static final int STOP = 9;

	public static final int P1_KEY_BASE = 11;
	public static final int P2_KEY_BASE = 21;
	public static final int P1_INVISIBLE_KEY_BASE = 31;
	public static final int P2_INVISIBLE_KEY_BASE = 41;
	public static final int P1_LONG_KEY_BASE = 51;
	public static final int P2_LONG_KEY_BASE = 61;
	public static final int P1_MINE_KEY_BASE = 131;
	public static final int P2_MINE_KEY_BASE = 141;

	/**
	 * 小節の拡大倍率
	 */
	private float rate = 1.0f;
	/**
	 * ストップシーケンス
	 */
	private double[] stop = new double[0];
	/**
	 * BPM変更
	 */
	private double[] bpm_change = new double[0];
	/**
	 * BGレーン
	 */
	private final int[][] auto;
	/**
	 * BGA
	 */
	private int[] bga;
	/**
	 * レイヤー
	 */
	private int[] layer;
	/**
	 * POORアニメーション
	 */
	private int[] poor = new int[0];
	/**
	 * 1P通常ノート
	 */
	private int[][] play_1 = new int[9][];
	/**
	 * 1P不可視ノート
	 */
	private int[][] play_1_invisible = new int[9][];
	/**
	 * 1Pロングノート
	 */
	private int[][] play_1_ln = new int[9][];
	/**
	 * 1P地雷ノート
	 */
	private int[][] play_1_mine = new int[9][];
	/**
	 * 2P通常ノート
	 */
	private int[][] play_2 = new int[9][];
	/**
	 * 2P不可視ノート
	 */
	private int[][] play_2_invisible = new int[9][];
	/**
	 * 2Pロングノート
	 */
	private int[][] play_2_ln = new int[9][];
	/**
	 * 2P地雷ノート
	 */
	private int[][] play_2_mine = new int[9][];
	/**
	 * 前の小節
	 */
	private final Section prev;

	private int usekeys = 5;

	private final BMSModel model;

	private float sectionnum;

	private List<DecodeLog> log = new ArrayList<DecodeLog>();

	public Section(BMSModel model, Section prev, String[] lines, Map<Integer, Double> bpmtable,
			Map<Integer, Double> stoptable) {
		this.model = model;
		final  List<int[]> auto = new ArrayList<int[]>();
		if (model.getUseKeys() == 9) {
			usekeys = 9;
		} else {
			usekeys = 5;
		}
		this.prev = prev;
		if (prev != null) {
			sectionnum = prev.sectionnum + prev.rate;
		}
		for (String line : lines) {
			int channel = 0;
			try {
				channel = Integer.parseInt(String.valueOf(line.charAt(3)), 16) * 10
						+ Integer.parseInt(String.valueOf(line.charAt(4)), 16);
			} catch (NumberFormatException e) {
				log.add(new DecodeLog(DecodeLog.STATE_WARNING, "チャンネル定義が無効です : " + line));
				Logger.getGlobal().warning(model.getTitle() + ":BMSファイルの解析中の例外:チャンネル定義が無効です - " + line);
			}
			switch (channel) {
			// BGレーン
			case LANE_AUTOPLAY:
				auto.add(this.splitData(line));
				break;
			// 小節の拡大率
			case SECTION_RATE:
				int colon_index = line.indexOf(":");
				line = line.substring(colon_index + 1, line.length());
				rate = Float.valueOf(line);
				break;
			// BPM変化
			case BPM_CHANGE:
				int[] datas = this.splitData(line);
				double[] d = new double[datas.length];
				for (int j = 0; j < datas.length; j++) {
					if (datas[j] != 0) {
						d[j] = (double) (datas[j] / 36) * 16 + (datas[j] % 36);
					} else {
						d[j] = 0;
					}
				}
				mergeBPMChange(d);
				break;
			// BGAレーン
			case BGA_PLAY:
				bga = this.splitData(line);
				break;
			// POORアニメーション
			case POOR_PLAY:
				poor = this.splitData(line);
				break;
			// レイヤー
			case LAYER_PLAY:
				layer = this.splitData(line);
				break;
			// BPM変化(拡張)
			case BPM_CHANGE_EXTEND:
				int[] bpmdatas = this.splitData(line);
				double[] d2 = new double[bpmdatas.length];
				for (int j = 0; j < bpmdatas.length; j++) {
					if (bpmdatas[j] != 0) {
						Double bpm = bpmtable.get(bpmdatas[j]);
						if (bpm != null) {
							d2[j] = bpm;
						} else {
							log.add(new DecodeLog(DecodeLog.STATE_WARNING, "未定義のBPM変化を参照しています : " + bpmdatas[j]));
							Logger.getGlobal().warning(model.getTitle() + ":BMSファイルの解析中の例外:未定義のBPM変化を参照しています :  " + bpmdatas[j]);
							d2[j] = 0;
						}
					} else {
						d2[j] = 0;
					}
				}
				mergeBPMChange(d2);
				break;
			// ストップシーケンス
			case STOP:
				int[] stopdatas = this.splitData(line);
				stop = new double[stopdatas.length];
				for (int j = 0; j < stopdatas.length; j++) {
					if (stopdatas[j] != 0) {
						Double st = stoptable.get(stopdatas[j]);
						if (st != null) {
							stop[j] = st;
						} else {
							log.add(new DecodeLog(DecodeLog.STATE_WARNING, "未定義のSTOPを参照しています : " + stopdatas[j]));
							Logger.getGlobal().warning(model.getTitle() + ":BMSファイルの解析中の例外:未定義のSTOPを参照しています :  " + stopdatas[j]);
							stop[j] = 0;
						}
					} else {
						stop[j] = 0;
					}
				}
				break;
			}
			// 通常ノート(1P側)
			this.convert(channel, P1_KEY_BASE, play_1, line);
			// 通常ノート(2P側)
			this.convert(channel, P2_KEY_BASE, play_2, line);
			// 不可視ノート(1P側)
			this.convert(channel, P1_INVISIBLE_KEY_BASE, play_1_invisible, line);
			// 不可視ノート(2P側)
			this.convert(channel, P2_INVISIBLE_KEY_BASE, play_2_invisible, line);
			// ロングノート(1P側)
			this.convert(channel, P1_LONG_KEY_BASE, play_1_ln, line);
			// ロングノート(2P側)
			this.convert(channel, P2_LONG_KEY_BASE, play_2_ln, line);
			// 地雷ノート(1P側)
			this.convert(channel, P1_MINE_KEY_BASE, play_1_mine, line);
			// 地雷ノート(2P側)
			this.convert(channel, P2_MINE_KEY_BASE, play_2_mine, line);
		}
		if (auto.size() == 0) {
			auto.add(new int[] { 0 });
		}
		this.auto = auto.toArray(new int[auto.size()][]);
		if (model.getUseKeys() < usekeys) {
			model.setUseKeys(usekeys);
		}
	}

	private void convert(int channel, int ch, int[][] notes, String line) {
		if (ch <= channel && channel <= ch + 8) {
			channel -= ch;
			if (channel == 5 || channel == 6) {
				channel += 2;
			} else if (channel == 7 || channel == 8) {
				if (usekeys == 5 || usekeys == 10) {
					usekeys = usekeys * 7 / 5;
				}
				channel -= 2;
			}
			
			int[] split = this.splitData(line);
			if((usekeys == 5 || usekeys == 7) && (ch == P2_KEY_BASE || ch == P2_INVISIBLE_KEY_BASE || ch == P2_LONG_KEY_BASE || ch ==P2_MINE_KEY_BASE)) {
				for(int i : split) {
					if(i != 0) {
						usekeys = usekeys * 2;
						break;
					}
				}
			}
			notes[channel] = this.mergeData(notes[channel], split);
		}
	}

	/**
	 * BPM変化のマージ処理を行う
	 * 
	 * @param b
	 *            マージするBPM変化
	 */
	private void mergeBPMChange(double[] b) {
		if (bpm_change.length == 0) {
			bpm_change = b;
		}
		int d = (bpm_change.length % b.length == 0 ? b.length : (b.length % bpm_change.length == 0 ? bpm_change.length
				: 1));

		double[] result = new double[bpm_change.length * b.length / d];
		Arrays.fill(result, 0.0);
		for (int i = 0; i < bpm_change.length; i++) {
			if (bpm_change[i] != 0.0) {
				result[i * b.length / d] = bpm_change[i];
			}
		}
		for (int i = 0; i < b.length; i++) {
			if (b[i] != 0.0) {
				result[i * bpm_change.length / d] = b[i];
			}
		}
		bpm_change = result;
	}

	/**
	 * ノーツのマージ処理を行う
	 * 
	 * @param b
	 *            マージするノーツ
	 */
	private int[] mergeData(int[] a, int[] b) {
		if (a == null || a.length == 0) {
			return b;
		}
		if (b == null || b.length == 0) {
			return a;
		}
		int d = (a.length % b.length == 0 ? b.length : (b.length % a.length == 0 ? a.length : 1));
		int[] result = new int[a.length * b.length / d];
		Arrays.fill(result, 0);
		for (int i = 0; i < a.length; i++) {
			if (a[i] != 0) {
				result[i * b.length / d] = a[i];
			}
		}
		for (int i = 0; i < b.length; i++) {
			if (b[i] != 0) {
				result[i * a.length / d] = b[i];
			}
		}
		return result;
	}

	private int[] splitData(String line) {
		final int findex = line.indexOf(":") + 1;
		final int lindex = line.length();
		final int split = (lindex - findex) / 2;
		int[] result = new int[split];
		try {
			for (int i = 0; i < split; i++) {
				result[i] = Integer.parseInt(line.substring(findex + i * 2, findex + i * 2 + 2), 36);
			}
		} catch(NumberFormatException e) {
			Logger.getGlobal().warning(model.getTitle() + ":チャンネル定義中の不正な値:" + line);
		}
		return result;			
	}

	public double getSectionRate() {
		return rate;
	}

	private double _lastbpm = 0.0;

	public double getStartBPM() {
		if (_lastbpm != 0.0) {
			return _lastbpm;
		}
		if (prev != null) {
			// 前小節の最後のBPMを返す
			double result = prev.getStartBPM();
			for (int i = 0; i < prev.bpm_change.length; i++) {
				if (prev.bpm_change[i] != 0.0) {
					result = prev.bpm_change[i];
				}
			}
			_lastbpm = result;
			return result;
		}
		// 開始時のBPMを返す
		return model.getBpm();
	}

	/**
	 * 小説開始時間(キャッシュ)
	 */
	private int _basetime = -1;

	/**
	 * 小説開始時間を取得する
	 * 
	 * @return 小説開始時間
	 */
	private int getStartTime() {
		if (_basetime != -1) {
			return _basetime;
		}
		if (prev != null) {
			int result = prev.getStartTime();

			double dt = 0.0;
			// 最終BPM取得
			double nowbpm = prev.getStartBPM();
			for (int i = 0; i < prev.bpm_change.length; i++) {
				if (prev.bpm_change[i] != 0.0) {
					nowbpm = prev.bpm_change[i];
				}
				for (int j = 0; j < prev.stop.length; j++) {
					if (((double) j / prev.stop.length >= (double) i / prev.bpm_change.length)
							&& ((double) j / prev.stop.length < (double) (i + 1) / prev.bpm_change.length)) {
						dt += prev.stop[j] * (1000 * 60 * 4 / nowbpm);
					}
				}
				dt += 1000 * 60 * 4 * prev.rate * (1.0 / prev.bpm_change.length) / nowbpm;
			}
			if (prev.bpm_change.length == 0) {
				for (int j = 0; j < prev.stop.length; j++) {
					dt += prev.stop[j] * (1000 * 60 * 4 / nowbpm);
				}
				dt += 1000 * 60 * 4 * prev.rate / nowbpm;
			}
			_basetime = (int) (result + dt);
			return _basetime;
		}
		return 0;
	}

	/**
	 * 小節開始時のLN状態(キャッシュ)
	 */
	private int[] _lnstatus = null;

	/**
	 * 小節開始時のLN状態を取得する
	 * 
	 * @return
	 */
	private int[] getStartLNStatus() {
		if (_lnstatus != null) {
			return _lnstatus;
		}
		if (prev != null) {
			_lnstatus = getEndLNStatus(prev);
			return _lnstatus;
		}
		return new int[18];
	}
	
	protected int[] getEndLNStatus(Section sec) {
		int[] _lnstatus = new int[18];
		int[] result = sec.getStartLNStatus();
		for (int i = 0; i < 9; i++) {
			int nowln = result[i];
			final int[] play1 = sec.play_1_ln[i];
			if(play1 != null) {
				final int play1length = play1.length;
				for (int j = 0; j < play1length; j++) {
					if (play1[j] != 0) {
						if (nowln == 0) {
							nowln = play1[j];
						} else if (nowln == play1[j]) {
							nowln = 0;
						} else {
							log.add(new DecodeLog(DecodeLog.STATE_WARNING, "LNの対応が取れていません : " + nowln + " - "
									+ play1[j]));
							Logger.getGlobal().warning(model.getTitle() + ":LNの対応が取れていません:" + nowln + " - " + play1[j]);
							nowln = 0;
						}
					}
				}					
			}
			_lnstatus[i] = nowln;

			nowln = result[i + 9];
			final int[] play2 = sec.play_2_ln[i];
			if(play2 != null) {
				final int play2length = play2.length;
				for (int j = 0; j < play2length; j++) {
					if (play2[j] != 0) {
						if (nowln == 0) {
							nowln = play2[j];
						} else if (nowln == play2[j]) {
							nowln = 0;
						} else {
							log.add(new DecodeLog(DecodeLog.STATE_WARNING, "LNの対応が取れていません : " + nowln + " - "
									+ play2[j]));
							Logger.getGlobal().warning(model.getTitle() + ":LNの対応が取れていません:" + nowln + " - " + play2[j]);
							nowln = 0;
						}
					}
				}					
			}
			_lnstatus[i + 9] = nowln;
		}
		return _lnstatus;
	}

	/**
	 * SectionモデルからTimeLineモデルを作成し、BMSModelに登録する
	 */
	public void makeTimeLines(int[] wavmap, int[] bgamap, int lnobj) {
		final int base = this.getStartTime();
		final int[] startln = this.getStartLNStatus().clone();
		// 小節線追加
		model.getTimeLine(sectionnum, base).setSectionLine(true);
		model.getTimeLine(sectionnum, base).setBPM(this.getStartBPM());
		final int[] poors = new int[poor.length];
		for (int i = 0; i < poors.length; i++) {
			if (bgamap[poor[i]] != -2) {
				poors[i] = bgamap[poor[i]];
			} else {
				poors[i] = -1;
			}
		}
		model.getTimeLine(sectionnum, base).setPoor(poors);
		// BPM変化。ストップシーケンステーブル準備
		final Map<Double, Double> bpmchange = new HashMap<Double, Double>();
		final Map<Double, Double> stop = new HashMap<Double, Double>();
		final List<Double> l = new ArrayList<Double>();
		for (int i = 0; i < bpm_change.length; i++) {
			if (bpm_change[i] != 0.0) {
				bpmchange.put((double) i / bpm_change.length, bpm_change[i]);
				l.add((double) i / bpm_change.length);
			}
		}
		final Double[] bk = (Double[]) l.toArray(new Double[l.size()]);
		l.clear();
		for (int i = 0; i < this.stop.length; i++) {
			if (this.stop[i] != 0.0) {
				stop.put((double) i / this.stop.length, this.stop[i]);
				l.add((double) i / this.stop.length);
			}
		}
		final Double[] st = (Double[]) l.toArray(new Double[l.size()]);
		// 通常ノート配置
		final int size = 74 + auto.length;
		for (int key = 0; key < size; key++) {
			int[] s = null;
			if (key >= 0 && key < 9) {
				s = this.play_1[key % 9];
			} else if (key >= 9 && key < 18) {
				s = this.play_2[key % 9];
			} else if (key >= 18 && key < 27) {
				s = this.play_1_invisible[key % 9];
			} else if (key >= 27 && key < 36) {
				s = this.play_2_invisible[key % 9];
			} else if (key >= 36 && key < 45) {
				s = this.play_1_ln[key % 9];
			} else if (key >= 45 && key < 54) {
				s = this.play_2_ln[key % 9];
			} else if (key >= 54 && key < 63) {
				s = this.play_1_mine[key % 9];
			} else if (key >= 63 && key < 72) {
				s = this.play_2_mine[key % 9];
			} else if (key == 72) {
				s = this.bga;
			} else if (key == 73) {
				s = this.layer;
			} else if (key >= 74) {
				s = this.auto[key - 74];
			}
			if(s == null) {
				continue;
			}
			double nowbpm = this.getStartBPM();
			double dt = 0.0;
			final int slength = s.length;
			for (int i = 0; i < slength; i++) {
				if (s[i] != 0) {
					final TimeLine tl = model.getTimeLine(sectionnum + (float) (rate * i / slength), base
							+ (int) (dt * rate));
					if (key >= 0 && key < 18) {
						if (tl.existNote(key % 18)) {
							log.add(new DecodeLog(DecodeLog.STATE_WARNING, "通常ノート追加時に衝突が発生しました : " + (key + 1) + ":"
									+ (base + (int) (dt * rate))));
							Logger.getGlobal().warning(
									model.getTitle() + ":通常ノート追加時に衝突が発生しました。" + (key + 1) + ":"
											+ (base + (int) (dt * rate)));
						}
						if (s[i] == lnobj) {
							// LN終端処理
							TimeLine[] tl2 = model.getAllTimeLines();
							for (int t = tl2.length - 1; t >= 0; t--) {
								if (base + (int) (dt * rate) > tl2[t].getTime() && tl2[t].existNote(key % 18)) {
									final Note note = tl2[t].getNote(key % 18);
									if (note instanceof NormalNote) {
										// LNOBJの直前のノートをLNに差し替える
										LongNote ln = new LongNote(note.getWav());
										tl2[t].setNote(key % 18, ln);
										tl.setNote(key % 18, ln);
										tl.setBPM(nowbpm);
										break;
									} else if (note instanceof LongNote && ((LongNote) note).getSection() == tl2[t].getSection()) {
										log.add(new DecodeLog(DecodeLog.STATE_WARNING,
												"LNレーンで開始定義し、LNオブジェクトで終端定義しています。レーン: " + key + " - Time(ms):"
														+ tl2[t].getTime()));
										Logger.getGlobal().warning(
												model.getTitle() + ":LNレーンで開始定義し、LNオブジェクトで終端定義しています。レーン:" + key
														+ " - Time(ms):" + tl2[t].getTime());
										tl.setNote(key % 18, note);
										tl.setBPM(nowbpm);
										break;
									} else {
										log.add(new DecodeLog(DecodeLog.STATE_WARNING, "LNオブジェクトの対応が取れません。レーン: " + key
												+ " - Time(ms):" + tl2[t].getTime()));
										Logger.getGlobal().warning(
												model.getTitle() + ":LNオブジェクトの対応が取れません。レーン:" + key + " - Time(ms):"
														+ tl2[t].getTime());
										tl.setBPM(nowbpm);
										break;
									}
								}
							}
						} else {
							tl.setNote(key % 18, new NormalNote(wavmap[s[i]]));
							tl.setBPM(nowbpm);
						}
					} else if (key >= 18 && key < 36) {
						// Logger.getGlobal().warning(model.getTitle() +
						// "隠しノート追加"
						// + (key - 17) + ":"
						// + (base + (int) (dt * rate)));

						tl.setHiddenNote(key % 18, new NormalNote(wavmap[s[i]]));

						tl.setBPM(nowbpm);
					} else if (key >= 36 && key < 54) {
						// LN処理
						if (startln[key % 18] == 0) {
							tl.setNote(key % 18, new LongNote(wavmap[s[i]]));
							tl.setBPM(nowbpm);
							startln[key % 18] = s[i];
						} else {
							// LN終端処理
							TimeLine[] tl2 = model.getAllTimeLines();
							for (int t = tl2.length - 1; t >= 0; t--) {
								if (base + (int) (dt * rate) > tl2[t].getTime() && tl2[t].existNote(key % 18)) {
									Note note = tl2[t].getNote(key % 18);
									if (note instanceof LongNote) {
										tl.setNote(key % 18, note);
										tl.setBPM(nowbpm);
										if(startln[key % 18] != s[i]) {
											((LongNote)note).getEndnote().setWav(wavmap[s[i]]);
										}
										startln[key % 18] = 0;
										break;
									} else {
										log.add(new DecodeLog(DecodeLog.STATE_WARNING, "LN内に通常ノートが存在します。レーン: "
												+ (key - 35) + " - Time(ms):" + tl2[t].getTime()));
										Logger.getGlobal().warning(
												model.getTitle() + ":LN内に通常ノートが存在します!" + (key - 35) + ":"
														+ tl2[t].getTime());
									}
								}
							}
						}
					} else if (key >= 54 && key < 72) {
						// 地雷ノート処理
						if (tl.existNote(key % 18)) {
							log.add(new DecodeLog(DecodeLog.STATE_WARNING, "地雷ノート追加時に衝突が発生しました : " + (key - 53) + ":"
									+ (base + (int) (dt * rate))));
							Logger.getGlobal().warning(
									model.getTitle() + ":地雷ノート追加時に衝突が発生しました。" + (key - 53) + ":"
											+ (base + (int) (dt * rate)));
						}
						tl.setNote(key % 18, new MineNote(wavmap[0], s[i]));
						tl.setBPM(nowbpm);
					} else if (key == 72) {
						tl.setBGA(bgamap[s[i]]);
						tl.setBPM(nowbpm);
					} else if (key == 73) {
						tl.setLayer(bgamap[s[i]]);
						tl.setBPM(nowbpm);
					} else if (key >= 74) {
						tl.addBackGroundNote(new NormalNote(wavmap[s[i]]));
						tl.setBPM(nowbpm);
					}
				}
				// BPM変化,ストップを考慮したタイム加算
				double se = 0.0;
				for (int j = 0; j < bk.length; j++) {
					// タイムラインにbpm変化を反映
					if (bk[j] >= (double) i / slength && bk[j] < (double) (i + 1) / slength) {
						for (int k = 0; k < st.length; k++) {
							// ストップ
							if (st[k] >= (double) i / slength + se && st[k] < bk[j]) {
								dt += 1000 * 60 * 4 * (st[k] - (double) i / slength - se) / nowbpm;
								se = st[k] - (double) i / slength;
								model.getTimeLine(sectionnum + st[k].floatValue() * rate, base + (int) (dt * rate))
										.setBPM(nowbpm);
								model.getTimeLine(sectionnum + st[k].floatValue() * rate, base + (int) (dt * rate))
										.setStop((int) (stop.get(st[k]) * (1000 * 60 * 4 / nowbpm)));
								// System.out
								// .println("STOP (BPM変化中) : "
								// + (stop.get(st[k]) * (1000 * 60 * 4 /
								// nowbpm))
								// + " - bpm " + nowbpm
								// + " - key - " + key);
								dt += stop.get(st[k]) * (1000 * 60 * 4 / nowbpm) / rate;
							}
						}
						dt += 1000 * 60 * 4 * (bk[j] - (double) i / slength - se) / nowbpm;
						se = bk[j] - (double) i / slength;
						nowbpm = bpmchange.get(bk[j]);
						// if (model.getTimeLine(base + (int) (dt * rate))
						// .getBPM() != nowbpm) {
						// System.out.println("登録するBPMが異なる可能性があります。Time " + (
						// base + (int) (dt * rate)) + " section : "
						// + (sectionnum + bk[j].floatValue()) + " BPM : " +
						// model.getTimeLine(
						// base + (int) (dt * rate)).getBPM()
						// + " → " + nowbpm);
						// }
						model.getTimeLine(sectionnum + bk[j].floatValue() * rate, base + (int) (dt * rate)).setBPM(
								nowbpm);
						// Logger.getGlobal().info(
						// "BPM変化:" + nowbpm + "  time:"
						// + (base + (int) (dt * rate)));
					}
				}
				for (int k = 0; k < st.length; k++) {
					// ストップ
					if (st[k] >= (double) i / slength + se && st[k] < (double) (i + 1) / slength) {
						dt += 1000 * 60 * 4 * (st[k] - (double) i / slength - se) / nowbpm;
						se = st[k] - (double) i / slength;
						model.getTimeLine(sectionnum + st[k].floatValue() * rate, base + (int) (dt * rate)).setBPM(
								nowbpm);
						model.getTimeLine(sectionnum + st[k].floatValue() * rate, base + (int) (dt * rate)).setStop(
								(int) (stop.get(st[k]) * (1000 * 60 * 4 / nowbpm)));
						// System.out.println("STOP : "
						// + (stop.get(st[k]) * (1000 * 60 * 4 / nowbpm))
						// + " - bpm " + nowbpm + " - key - " + key);
						dt += stop.get(st[k]) * (1000 * 60 * 4 / nowbpm) / rate;
						// if (model.getTimeLine(base + (int) (dt * rate))
						// .getBPM() != nowbpm) {
						// System.out.println("登録するBPMが異なる可能性があります。Time " + (
						// base + (int) (dt * rate)) + " section : "
						// + (sectionnum + st[k].floatValue()) + " BPM : " +
						// model.getTimeLine(
						// base + (int) (dt * rate)).getBPM()
						// + " → " + nowbpm);
						// }
					}
				}
				final double dd = 1000 * 60 * 4 * (1.0 / slength - se) / nowbpm;
				// if (dd * rate < 1.0) {
				// Logger.getGlobal().warning(
				// "時間軸:" + (base + (int) (dt * rate))
				// + "において時間加算が1ms以下で、TimeLine衝突発生");
				// //dt += 1.0 / rate;
				// }
				dt += dd;
			}
		}
	}

	public List<DecodeLog> getDecodeLog() {
		return log;
	}
}
