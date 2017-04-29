package bms.model;

import java.util.*;
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

	private Mode mode = Mode.BEAT_5K;

	private final BMSModel model;

	private final float sectionnum;

	private final List<DecodeLog> log;

	public Section(BMSModel model, Section prev, List<String> lines, Map<Integer, Double> bpmtable,
			Map<Integer, Double> stoptable, List<DecodeLog> log) {
		this.model = model;
		this.log = log;
		final  List<int[]> auto = new ArrayList<int[]>();
		mode = model.getMode() == Mode.POPN_9K ? Mode.POPN_9K : Mode.BEAT_5K;
		this.prev = prev;
		if (prev != null) {
			sectionnum = prev.sectionnum + prev.rate;
		} else {
			sectionnum = 0;
		}
		for (String line : lines) {
			int channel = 0;
			try {
				final char c1 = line.charAt(4);
				if (c1 >= '0' && c1 <= '9') {
					channel = (c1 - '0') * 10;
				} else if (c1 >= 'a' && c1 <= 'z') {
					channel = ((c1 - 'a') + 10) * 10;
				} else if (c1 >= 'A' && c1 <= 'Z') {
					channel = ((c1 - 'A') + 10) * 10;
				} else {
					throw new NumberFormatException();
				}

				final char c2 = line.charAt(5);
				if (c2 >= '0' && c2 <= '9') {
					channel += (c2 - '0');
				} else {
					throw new NumberFormatException();
				}

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
				for (int j = 0; j < datas.length; j++) {
					if (datas[j] != 0) {
						bpmchange.put((double) j / datas.length, (double) (datas[j] / 36) * 16 + (datas[j] % 36));
					}
				}
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
				for (int j = 0; j < bpmdatas.length; j++) {
					if (bpmdatas[j] != 0) {
						Double bpm = bpmtable.get(bpmdatas[j]);
						if (bpm != null) {
							bpmchange.put((double) j / bpmdatas.length, bpm);
						} else {
							log.add(new DecodeLog(DecodeLog.STATE_WARNING, "未定義のBPM変化を参照しています : " + bpmdatas[j]));
							Logger.getGlobal().warning(model.getTitle() + ":BMSファイルの解析中の例外:未定義のBPM変化を参照しています :  " + bpmdatas[j]);
						}
					}
				}
				break;
			// ストップシーケンス
			case STOP:
				int[] stopdatas = this.splitData(line);
				for (int j = 0; j < stopdatas.length; j++) {
					if (stopdatas[j] != 0) {
						Double st = stoptable.get(stopdatas[j]);
						if (st != null) {
							stop.put((double) j / stopdatas.length, st);
						} else {
							log.add(new DecodeLog(DecodeLog.STATE_WARNING, "未定義のSTOPを参照しています : " + stopdatas[j]));
							Logger.getGlobal().warning(model.getTitle() + ":BMSファイルの解析中の例外:未定義のSTOPを参照しています :  " + stopdatas[j]);
						}
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
		if (model.getMode() == null || model.getMode().key < mode.key) {
			model.setMode(mode);
		}
	}
	
	private void convert(int channel, int ch, int[][] notes, String line) {
		if (ch <= channel && channel <= ch + 8) {
			channel -= ch;
			if (channel == 5 || channel == 6) {
				channel += 2;
			} else if (channel == 7 || channel == 8) {
				if (mode == Mode.BEAT_5K) {
					mode = Mode.BEAT_7K;
				} else if(mode == Mode.BEAT_10K){
					mode = Mode.BEAT_14K;
				}
				channel -= 2;
			}
			
			int[] split = this.splitData(line);
			if((mode == Mode.BEAT_5K || mode == Mode.BEAT_7K) && (ch == P2_KEY_BASE || ch == P2_INVISIBLE_KEY_BASE || ch == P2_LONG_KEY_BASE || ch ==P2_MINE_KEY_BASE)) {
				for(int i : split) {
					if(i != 0) {
						if (mode == Mode.BEAT_5K) {
							mode = Mode.BEAT_10K;
						} else if(mode == Mode.BEAT_7K){
							mode = Mode.BEAT_14K;
						}
						break;
					}
				}
			}
			notes[channel] = this.mergeData(notes[channel], split);
		}
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
				result[i] = BMSDecoder.parseInt36(line, findex + i * 2);
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
			_lastbpm = prev.bpmchange.size() > 0 ? prev.bpmchange.lastEntry().getValue() : prev.getStartBPM();
			return _lastbpm;
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
			double prevsection = 0;
			Iterator<Map.Entry<Double, Double>> stops = prev.stop.entrySet().iterator();
			Map.Entry<Double, Double> stop = stops.hasNext() ? stops.next() : null;
			for (Map.Entry<Double, Double> bpm : prev.bpmchange.entrySet()) {
				for (; stop != null ; stop = stops.hasNext() ? stops.next() : null) {
					if (stop.getKey() < bpm.getKey()) {
						dt += stop.getValue() * (1000 * 60 * 4 / nowbpm);
					} else {
						break;
					}
				}
				dt += 1000 * 60 * 4 * prev.rate * (bpm.getKey() - prevsection) / nowbpm;
				nowbpm = bpm.getValue();
				prevsection = bpm.getKey();
			}
			
			for (; stop != null ; stop = stops.hasNext() ? stops.next() : null) {
				dt += stop.getValue() * (1000 * 60 * 4 / nowbpm);
			}
			dt += 1000 * 60 * 4 * prev.rate * (1.0 - prevsection) / nowbpm;
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

	private final TreeMap<Double, Double> bpmchange = new TreeMap<Double, Double>();
	private final TreeMap<Double, Double> stop = new TreeMap<Double, Double>();
	
	public static final int[] NOTEASSIGN_BEAT5 = { 0, 1, 2, 3, 4, -1, -1, 5, -1, 6, 7, 8, 9, 10, -1, -1, 11, -1 };
	public static final int[] NOTEASSIGN_BEAT7 = { 0, 1, 2, 3, 4, 5, 6, 7, -1, 8, 9, 10, 11, 12, 13, 14, 15, -1 };
	public static final int[] NOTEASSIGN_POPN = { 0, 1, 2, 3, 4, -1,-1,-1,-1,-1, 5, 6, 7, 8,-1,-1,-1,-1 };

	/**
	 * SectionモデルからTimeLineモデルを作成し、BMSModelに登録する
	 */
	public void makeTimeLines(int[] wavmap, int[] bgamap, int lnobj, TreeMap<Float, TimeLine> tlcache) {
		final int base = this.getStartTime();
		final int[] startln = this.getStartLNStatus().clone();
		final int[] assign = model.getMode() == Mode.POPN_9K ? NOTEASSIGN_POPN : 
			(model.getMode() == Mode.BEAT_7K || model.getMode() == Mode.BEAT_14K ? NOTEASSIGN_BEAT7 : NOTEASSIGN_BEAT5);
		// 小節線追加
		final TimeLine basetl = getTimeLine(sectionnum, base, tlcache);
		basetl.setSectionLine(true);
		basetl.setBPM(this.getStartBPM());
		final int[] poors = new int[poor.length];
		for (int i = 0; i < poors.length; i++) {
			if (bgamap[poor[i]] != -2) {
				poors[i] = bgamap[poor[i]];
			} else {
				poors[i] = -1;
			}
		}
		basetl.setPoor(poors);
		// BPM変化。ストップシーケンステーブル準備
		final Double[] bk = bpmchange.keySet().toArray(new Double[bpmchange.size()]);;
		final Double[] st = stop.keySet().toArray(new Double[stop.size()]);
		// 通常ノート配置
		final int size = 74 + auto.length;
		for (int keys = 0; keys < size; keys++) {
			int[] s = null;
			if (keys >= 0 && keys < 9) {
				s = this.play_1[keys];
			} else if (keys >= 9 && keys < 18) {
				s = this.play_2[keys - 9];
			} else if (keys >= 18 && keys < 27) {
				s = this.play_1_invisible[keys - 18];
			} else if (keys >= 27 && keys < 36) {
				s = this.play_2_invisible[keys - 27];
			} else if (keys >= 36 && keys < 45) {
				s = this.play_1_ln[keys - 36];
			} else if (keys >= 45 && keys < 54) {
				s = this.play_2_ln[keys - 45];
			} else if (keys >= 54 && keys < 63) {
				s = this.play_1_mine[keys - 54];
			} else if (keys >= 63 && keys < 72) {
				s = this.play_2_mine[keys - 63];
			} else if (keys == 72) {
				s = this.bga;
			} else if (keys == 73) {
				s = this.layer;
			} else if (keys >= 74) {
				s = this.auto[keys - 74];
			}
			if(s == null) {
				continue;
			}
			double nowbpm = this.getStartBPM();
			double dt = 0.0;
			final int slength = s.length;
			for (int i = 0; i < slength; i++) {
				if (s[i] != 0) {
					final TimeLine tl = getTimeLine(sectionnum + (float) (rate * i / slength), base + (int) (dt * rate), tlcache);
					if (keys < 18) {
						final int key = assign[keys];
						if(key != -1) {
							if (tl.existNote(key)) {
								log.add(new DecodeLog(DecodeLog.STATE_WARNING, "通常ノート追加時に衝突が発生しました : " + (key + 1) + ":"
										+ (base + (int) (dt * rate))));
								Logger.getGlobal().warning(
										model.getTitle() + ":通常ノート追加時に衝突が発生しました。" + (key + 1) + ":"
												+ (base + (int) (dt * rate)));
							}
							if (s[i] == lnobj) {
								// LN終端処理
								// TODO 高速化のために直前のノートを記録しておく
								for (Map.Entry<Float, TimeLine> e : tlcache.descendingMap().entrySet()) {
									if(e.getKey() >= tl.getSection() ) {
										continue;
									}
									final TimeLine tl2 = e.getValue();
									if (tl2.existNote(key)) {
										final Note note = tl2.getNote(key);
										if (note instanceof NormalNote) {
											// LNOBJの直前のノートをLNに差し替える
											LongNote ln = new LongNote(note.getWav());
											tl2.setNote(key, ln);
											tl.setNote(key, ln);
											tl.setBPM(nowbpm);
											break;
										} else if (note instanceof LongNote && ((LongNote) note).getSection() == tl2.getSection()) {
											log.add(new DecodeLog(DecodeLog.STATE_WARNING,
													"LNレーンで開始定義し、LNオブジェクトで終端定義しています。レーン: " + key + " - Time(ms):"
															+ tl2.getTime()));
											Logger.getGlobal().warning(
													model.getTitle() + ":LNレーンで開始定義し、LNオブジェクトで終端定義しています。レーン:" + key
															+ " - Time(ms):" + tl2.getTime());
											tl.setNote(key, note);
											tl.setBPM(nowbpm);
											break;
										} else {
											log.add(new DecodeLog(DecodeLog.STATE_WARNING, "LNオブジェクトの対応が取れません。レーン: " + key
													+ " - Time(ms):" + tl2.getTime()));
											Logger.getGlobal().warning(
													model.getTitle() + ":LNオブジェクトの対応が取れません。レーン:" + key + " - Time(ms):"
															+ tl2.getTime());
											tl.setBPM(nowbpm);
											break;
										}
									}
								}
							} else {
								tl.setNote(key, new NormalNote(wavmap[s[i]]));
								tl.setBPM(nowbpm);
							}							
						}
					} else if (keys >= 18 && keys < 36) {
						// Logger.getGlobal().warning(model.getTitle() +
						// "隠しノート追加"
						// + (key - 17) + ":"
						// + (base + (int) (dt * rate)));

						final int key = assign[keys - 18];
						if(key != -1) {
							tl.setHiddenNote(key, new NormalNote(wavmap[s[i]]));							
							tl.setBPM(nowbpm);
						}
					} else if (keys >= 36 && keys < 54) {
						final int key = assign[keys - 36];
						if(key != -1) {
							// LN処理
							if (startln[keys - 36] == 0) {
								tl.setNote(key, new LongNote(wavmap[s[i]]));
								tl.setBPM(nowbpm);
								startln[keys - 36] = s[i];
							} else {
								// LN終端処理
								for (Map.Entry<Float, TimeLine> e : tlcache.descendingMap().entrySet()) {
									if(e.getKey() >= tl.getSection()) {
										continue;
									}
									final TimeLine tl2 = e.getValue();
									if (tl2.existNote(key)) {
										Note note = tl2.getNote(key);
										if (note instanceof LongNote) {
											tl.setNote(key, note);
											tl.setBPM(nowbpm);
											if(startln[keys - 36] != s[i]) {
												((LongNote)note).getEndnote().setWav(wavmap[s[i]]);
											}
											startln[keys - 36] = 0;
											break;
										} else {
											log.add(new DecodeLog(DecodeLog.STATE_WARNING, "LN内に通常ノートが存在します。レーン: "
													+ (key + 1) + " - Time(ms):" + tl2.getTime()));
											Logger.getGlobal().warning(
													model.getTitle() + ":LN内に通常ノートが存在します!" + (key + 1) + ":"
															+ tl2.getTime());
										}
									}
								}
							}							
						}
					} else if (keys >= 54 && keys < 72) {
						final int key = assign[keys - 54];
						if(key != -1) {
							// 地雷ノート処理
							if (tl.existNote(key)) {
								log.add(new DecodeLog(DecodeLog.STATE_WARNING, "地雷ノート追加時に衝突が発生しました : " + (key + 1) + ":"
										+ (base + (int) (dt * rate))));
								Logger.getGlobal().warning(
										model.getTitle() + ":地雷ノート追加時に衝突が発生しました。" + (key + 1) + ":"
												+ (base + (int) (dt * rate)));
							}
							tl.setNote(key, new MineNote(wavmap[0], s[i]));
							tl.setBPM(nowbpm);							
						}
					} else if (keys == 72) {
						tl.setBGA(bgamap[s[i]]);
						tl.setBPM(nowbpm);
					} else if (keys == 73) {
						tl.setLayer(bgamap[s[i]]);
						tl.setBPM(nowbpm);
					} else if (keys >= 74) {
						tl.addBackGroundNote(new NormalNote(wavmap[s[i]]));
						tl.setBPM(nowbpm);
					}
				}
				// BPM変化,ストップを考慮したタイム加算
				double se = 0.0;
				for (double bc : bk) {
					// タイムラインにbpm変化を反映
					if (bc >= (double) i / slength && bc < (double) (i + 1) / slength) {
						for (double stopt :st) {
							// ストップ
							if (stopt >= (double) i / slength + se && stopt < bc) {
								dt += 1000 * 60 * 4 * (stopt - (double) i / slength - se) / nowbpm;
								se = stopt - (double) i / slength;
								final TimeLine tl =  getTimeLine(sectionnum + (float)stopt * rate, base + (int) (dt * rate), tlcache);
								tl.setBPM(nowbpm);
								tl.setStop((int) (stop.get(stopt) * (1000 * 60 * 4 / nowbpm)));
								// System.out
								// .println("STOP (BPM変化中) : "
								// + (stop.get(st[k]) * (1000 * 60 * 4 /
								// nowbpm))
								// + " - bpm " + nowbpm
								// + " - key - " + key);
								dt += stop.get(stopt) * (1000 * 60 * 4 / nowbpm) / rate;
							}
						}
						dt += 1000 * 60 * 4 * (bc - (double) i / slength - se) / nowbpm;
						se = bc - (double) i / slength;
						nowbpm = bpmchange.get(bc);
						// if (model.getTimeLine(base + (int) (dt * rate))
						// .getBPM() != nowbpm) {
						// System.out.println("登録するBPMが異なる可能性があります。Time " + (
						// base + (int) (dt * rate)) + " section : "
						// + (sectionnum + bk[j].floatValue()) + " BPM : " +
						// model.getTimeLine(
						// base + (int) (dt * rate)).getBPM()
						// + " → " + nowbpm);
						// }
						getTimeLine(sectionnum + (float)bc * rate, base + (int) (dt * rate), tlcache).setBPM(nowbpm);
						// Logger.getGlobal().info(
						// "BPM変化:" + nowbpm + "  time:"
						// + (base + (int) (dt * rate)));
					}
				}
				for (double stopt : st) {
					// ストップ
					if (stopt >= (double) i / slength + se && stopt < (double) (i + 1) / slength) {
						dt += 1000 * 60 * 4 * (stopt - (double) i / slength - se) / nowbpm;
						se = stopt - (double) i / slength;
						final TimeLine tl = getTimeLine(sectionnum + (float)stopt * rate, base + (int) (dt * rate), tlcache);
						tl.setBPM(nowbpm);
						tl.setStop((int) (stop.get(stopt) * (1000 * 60 * 4 / nowbpm)));
						// System.out.println("STOP : "
						// + (stop.get(st[k]) * (1000 * 60 * 4 / nowbpm))
						// + " - bpm " + nowbpm + " - key - " + key);
						dt += stop.get(stopt) * (1000 * 60 * 4 / nowbpm) / rate;
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
	
	private TimeLine getTimeLine(float section, int time, TreeMap<Float, TimeLine> tlcache) {
		final TimeLine tlc = tlcache.get(section);
		if (tlc != null) {
			return tlc;
		}
		TimeLine tl = new TimeLine(section, time, model.getMode().key);
		tlcache.put(section, tl);
		return tl;
	}
}
