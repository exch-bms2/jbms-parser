package bms.model;

import java.util.*;
import java.util.Map.Entry;

import bms.model.BMSDecoder.TimeLineCache;
import bms.model.Layer.EventType;

import static bms.model.DecodeLog.State.*;

/**
 * 小節
 * 
 * @author exch
 */
public class Section {

	public static final int ILLEGAL = -1;
	public static final int LANE_AUTOPLAY = 1;
	public static final int SECTION_RATE = 2;
	public static final int BPM_CHANGE = 3;
	public static final int BGA_PLAY = 4;
	public static final int POOR_PLAY = 6;
	public static final int LAYER_PLAY = 7;
	public static final int BPM_CHANGE_EXTEND = 8;
	public static final int STOP = 9;

	public static final int P1_KEY_BASE = 1 * 36 + 1;
	public static final int P2_KEY_BASE = 2 * 36 + 1;
	public static final int P1_INVISIBLE_KEY_BASE = 3 * 36 + 1;
	public static final int P2_INVISIBLE_KEY_BASE = 4 * 36 + 1;
	public static final int P1_LONG_KEY_BASE = 5 * 36 + 1;
	public static final int P2_LONG_KEY_BASE = 6 * 36 + 1;
	public static final int P1_MINE_KEY_BASE = 13 * 36 + 1;
	public static final int P2_MINE_KEY_BASE = 14 * 36 + 1;

	public static final int SCROLL = 1020;
	/**
	 * 小節の拡大倍率
	 */
	private double rate = 1.0;
	/**
	 * POORアニメーション
	 */
	private int[] poor = new int[0];

	private Mode mode = Mode.BEAT_5K;

	private final BMSModel model;

	private final double sectionnum;

	private final List<DecodeLog> log;
	
	private List<String> channellines;

	public Section(BMSModel model, Section prev, List<String> lines, Map<Integer, Double> bpmtable,
			Map<Integer, Double> stoptable, Map<Integer, Double> scrolltable, List<DecodeLog> log) {
		this.model = model;
		this.log = log;
		
		channellines = new ArrayList<String>(lines.size());
		mode = model.getMode() == Mode.POPN_9K ? Mode.POPN_9K : Mode.BEAT_5K;
		if (prev != null) {
			sectionnum = prev.sectionnum + prev.rate;
		} else {
			sectionnum = 0;
		}
		for (String line : lines) {
			int channel = ChartDecoder.parseInt36(line.charAt(4), line.charAt(5));
			switch (channel) {
			case ILLEGAL:
				log.add(new DecodeLog(WARNING, "チャンネル定義が無効です : " + line));
				break;
			// BGレーン
			case LANE_AUTOPLAY:
				// BGAレーン
			case BGA_PLAY:
			// レイヤー
			case LAYER_PLAY:
				channellines.add(line);
				break;
			// 小節の拡大率
			case SECTION_RATE:
				int colon_index = line.indexOf(":");
				try {
					rate = Double.valueOf(line.substring(colon_index + 1, line.length()));					
				} catch (NumberFormatException e) {
					log.add(new DecodeLog(WARNING, "小節の拡大率が不正です : " + line));
				}
				break;
			// BPM変化
			case BPM_CHANGE:
				this.processData(line, (pos, data) -> {
					bpmchange.put(pos, (double) (data / 36) * 16 + (data % 36));
				});
				break;
			// POORアニメーション
			case POOR_PLAY:
				poor = this.splitData(line);
				// アニメーションが単一画像のみの定義の場合、0を除外する(ミスレイヤーチャンネルの定義が曖昧)
				int singleid = 0;
				for(int id : poor) {
					if(id != 0) {
						if(singleid != 0 && singleid != id) {
							singleid = -1;
							break;
						} else {
							singleid = id;
						}
					}
				}
				if(singleid != -1) {
					poor = new int[] {singleid};
				}
				break;
			// BPM変化(拡張)
			case BPM_CHANGE_EXTEND:
				this.processData(line, (pos, data) -> {
					Double bpm = bpmtable.get(data);
					if (bpm != null) {
						bpmchange.put(pos, bpm);
					} else {
						log.add(new DecodeLog(WARNING, "未定義のBPM変化を参照しています : " + data));
					}
				});
				break;
			// ストップシーケンス
			case STOP:
				this.processData(line, (pos, data) -> {
					Double st = stoptable.get(data);
					if (st != null) {
						stop.put(pos, st);
					} else {
						log.add(new DecodeLog(WARNING, "未定義のSTOPを参照しています : " + data));
					}
				});
				break;
				// scroll
			case SCROLL:
				this.processData(line, (pos, data) -> {
					Double st = scrolltable.get(data);
					if (st != null) {
						scroll.put(pos, st);
					} else {
						log.add(new DecodeLog(WARNING, "未定義のSCROLLを参照しています : " + data));
					}
				});
				break;
			}
			// 通常ノート(1P側)
			this.convert(channel, P1_KEY_BASE, line);
			// 通常ノート(2P側)
			this.convert(channel, P2_KEY_BASE, line);
			// 不可視ノート(1P側)
			this.convert(channel, P1_INVISIBLE_KEY_BASE, line);
			// 不可視ノート(2P側)
			this.convert(channel, P2_INVISIBLE_KEY_BASE, line);
			// ロングノート(1P側)
			this.convert(channel, P1_LONG_KEY_BASE, line);
			// ロングノート(2P側)
			this.convert(channel, P2_LONG_KEY_BASE, line);
			// 地雷ノート(1P側)
			this.convert(channel, P1_MINE_KEY_BASE, line);
			// 地雷ノート(2P側)
			this.convert(channel, P2_MINE_KEY_BASE, line);
		}
		if (model.getMode() == null || model.getMode().key < mode.key) {
			model.setMode(mode);
		}
	}
	
	private void convert(int channel, int ch, String line) {
		if (ch <= channel && channel <= ch + 8) {
			channel -= ch;			
			if((mode == Mode.BEAT_5K || mode == Mode.BEAT_10K) && (channel == 7 || channel == 8)) {
				this.processData(line, (pos, data) -> {
					mode = (mode == Mode.BEAT_10K) ? Mode.BEAT_14K : ((mode == Mode.BEAT_5K) ? Mode.BEAT_7K : mode);
				});
			}
			
			if((mode == Mode.BEAT_5K || mode == Mode.BEAT_7K) && (ch == P2_KEY_BASE || ch == P2_INVISIBLE_KEY_BASE || ch == P2_LONG_KEY_BASE || ch ==P2_MINE_KEY_BASE)) {
				this.processData(line, (pos, data) -> {
					mode = (mode == Mode.BEAT_7K) ? Mode.BEAT_14K : ((mode == Mode.BEAT_5K) ? Mode.BEAT_10K : mode);
				});
			}
			
			channellines.add(line);
		}
	}

	private int[] splitData(String line) {
		final int findex = line.indexOf(":") + 1;
		final int lindex = line.length();
		final int split = (lindex - findex) / 2;
		int[] result = new int[split];
		for (int i = 0; i < split; i++) {
			result[i] = ChartDecoder.parseInt36(line.charAt(findex + i * 2), line.charAt(findex + i * 2 + 1));
			if(result[i] == -1) {
				log.add(new DecodeLog(WARNING, model.getTitle() + ":チャンネル定義中の不正な値:" + line));
				result[i] = 0;
			}
		}
		return result;			
	}
	
	private void processData(String line, DataProcessor processor) {
		final int findex = line.indexOf(":") + 1;
		final int lindex = line.length();
		final int split = (lindex - findex) / 2;
		for (int i = 0; i < split; i++) {
			int result = ChartDecoder.parseInt36(line.charAt(findex + i * 2), line.charAt(findex + i * 2 + 1));
			if(result > 0) {
				processor.process((double)i / split, result);
			} else if(result == -1){
				log.add(new DecodeLog(WARNING, model.getTitle() + ":チャンネル定義中の不正な値:" + line));				
			}
		}
	}
	
	interface DataProcessor {
		public void process(double pos, int data);
	}

	private final TreeMap<Double, Double> bpmchange = new TreeMap<Double, Double>();
	private final TreeMap<Double, Double> stop = new TreeMap<Double, Double>();
	private final TreeMap<Double, Double> scroll = new TreeMap<Double, Double>();
	
	private static final int[] CHANNELASSIGN_BEAT5 = { 0, 1, 2, 3, 4, 5, -1, -1, -1, 6, 7, 8, 9, 10, 11, -1, -1, -1 };
	private static final int[] CHANNELASSIGN_BEAT7 = { 0, 1, 2, 3, 4, 7, -1, 5, 6, 8, 9, 10, 11, 12, 15, -1, 13, 14 };
	private static final int[] CHANNELASSIGN_POPN = { 0, 1, 2, 3, 4, -1,-1,-1,-1,-1, 5, 6, 7, 8,-1,-1,-1,-1 };

	private TreeMap<Double, TimeLineCache> tlcache;

	/**
	 * SectionモデルからTimeLineモデルを作成し、BMSModelに登録する
	 */
	public void makeTimeLines(int[] wavmap, int[] bgamap, TreeMap<Double, TimeLineCache> tlcache, List<LongNote>[] lnlist, LongNote[] startln) {
		final int lnobj = model.getLnobj();
		final int lnmode = model.getLnmode();
		this.tlcache = tlcache;
		final int[] cassign = model.getMode() == Mode.POPN_9K ? CHANNELASSIGN_POPN : 
			(model.getMode() == Mode.BEAT_7K || model.getMode() == Mode.BEAT_14K ? CHANNELASSIGN_BEAT7 : CHANNELASSIGN_BEAT5);
		// 小節線追加
		final TimeLine basetl = getTimeLine(sectionnum);
		basetl.setSectionLine(true);
		
		if(poor.length > 0) {
			final Layer.Sequence[] poors = new Layer.Sequence[poor.length + 1];
			final int poortime = 500;
			
			for (int i = 0; i < poor.length; i++) {
				if (bgamap[poor[i]] != -2) {
					poors[i] = new Layer.Sequence((long)(i * poortime / poor.length), bgamap[poor[i]]);
				} else {
					poors[i] = new Layer.Sequence((long)(i * poortime / poor.length), -1);
				}
			}
			poors[poors.length - 1] = new Layer.Sequence(poortime);
			basetl.setEventlayer(new Layer[] {new Layer(new Layer.Event(EventType.MISS, 1),new Layer.Sequence[][] {poors})});			
		}
		// BPM変化。ストップシーケンステーブル準備
		Iterator<Entry<Double, Double>> stops = stop.entrySet().iterator();			
		Map.Entry<Double, Double> ste = stops.hasNext() ? stops.next() : null;
		Iterator<Entry<Double, Double>> bpms = bpmchange.entrySet().iterator();			
		Map.Entry<Double, Double> bce = bpms.hasNext() ? bpms.next() : null;
		Iterator<Entry<Double, Double>> scrolls = scroll.entrySet().iterator();			
		Map.Entry<Double, Double> sce = scrolls.hasNext() ? scrolls.next() : null;
		
		while(ste != null || bce != null || sce != null) {
			final double bc = bce != null ? bce.getKey() : 2;
			final double st = ste != null ? ste.getKey() : 2;
			final double sc = sce != null ? sce.getKey() : 2;
			if(sc <= st && sc <= bc) {
				getTimeLine(sectionnum + sc * rate).setScroll(sce.getValue());
				sce = scrolls.hasNext() ? scrolls.next() : null;
			} else if(bc <= st) {
				getTimeLine(sectionnum + bc * rate).setBPM(bce.getValue());
				bce = bpms.hasNext() ? bpms.next() : null;
			} else if(st <= 1){
				final TimeLine tl = getTimeLine(sectionnum + ste.getKey() * rate);
				tl.setStop((long) (1000.0 * 1000 * 60 * 4 * ste.getValue() / (tl.getBPM())));
				ste = stops.hasNext() ? stops.next() : null;
			}
		}
		
		for(String line : channellines) {
			int channel = ChartDecoder.parseInt36(line.charAt(4), line.charAt(5));
			int tmpkey = 0;
			if(channel >= P1_KEY_BASE && channel < P1_KEY_BASE + 9) {
				tmpkey = cassign[channel - P1_KEY_BASE];
				channel = P1_KEY_BASE;
			} else if(channel >= P2_KEY_BASE && channel < P2_KEY_BASE + 9) {
				tmpkey = cassign[channel - P2_KEY_BASE + 9];
				channel = P1_KEY_BASE;
			} else if(channel >= P1_INVISIBLE_KEY_BASE && channel < P1_INVISIBLE_KEY_BASE + 9) {
				tmpkey = cassign[channel - P1_INVISIBLE_KEY_BASE];
				channel = P1_INVISIBLE_KEY_BASE;
			} else if(channel >= P2_INVISIBLE_KEY_BASE && channel < P2_INVISIBLE_KEY_BASE + 9) {
				tmpkey = cassign[channel - P2_INVISIBLE_KEY_BASE + 9];
				channel = P1_INVISIBLE_KEY_BASE;
			} else if(channel >= P1_LONG_KEY_BASE && channel < P1_LONG_KEY_BASE + 9) {
				tmpkey = cassign[channel - P1_LONG_KEY_BASE];
				channel = P1_LONG_KEY_BASE;
			} else if(channel >= P2_LONG_KEY_BASE && channel < P2_LONG_KEY_BASE + 9) {
				tmpkey = cassign[channel - P2_LONG_KEY_BASE + 9];
				channel = P1_LONG_KEY_BASE;
			} else if(channel >= P1_MINE_KEY_BASE && channel < P1_MINE_KEY_BASE + 9) {
				tmpkey = cassign[channel - P1_MINE_KEY_BASE];
				channel = P1_MINE_KEY_BASE;
			} else if(channel >= P2_MINE_KEY_BASE && channel < P2_MINE_KEY_BASE + 9) {
				tmpkey = cassign[channel - P2_MINE_KEY_BASE + 9];
				channel = P1_MINE_KEY_BASE;
			}
			final int key = tmpkey;
			if(key == -1) {
				continue;
			}
			switch (channel) {
			case P1_KEY_BASE:
				this.processData(line, (pos, data) -> {
					// normal note, lnobj
					final TimeLine tl = getTimeLine(sectionnum + rate * pos);
					if (tl.existNote(key)) {
						log.add(new DecodeLog(WARNING, "通常ノート追加時に衝突が発生しました : " + (key + 1) + ":"
								+ tl.getTime()));
					}
					if (data == lnobj) {
						// LN終端処理
						// TODO 高速化のために直前のノートを記録しておく
						for (Map.Entry<Double, TimeLineCache> e : tlcache.descendingMap().entrySet()) {
							if(e.getKey() >= tl.getSection() ) {
								continue;
							}
							final TimeLine tl2 = e.getValue().timeline;
							if (tl2.existNote(key)) {
								final Note note = tl2.getNote(key);
								if (note instanceof NormalNote) {
									// LNOBJの直前のノートをLNに差し替える
									LongNote ln = new LongNote(note.getWav());
									ln.setType(lnmode);
									tl2.setNote(key, ln);
									LongNote lnend = new LongNote(-2);
									tl.setNote(key, lnend);
									ln.setPair(lnend);
									
									if (lnlist[key] == null) {
										lnlist[key] = new ArrayList<LongNote>();
									}
									lnlist[key].add(ln);
									break;
								} else if (note instanceof LongNote && ((LongNote) note).getPair() == null) {
									log.add(new DecodeLog(WARNING,
											"LNレーンで開始定義し、LNオブジェクトで終端定義しています。レーン: " + key + " - Time(ms):"
													+ tl2.getTime()));
									tl.setNote(key, note);
									break;
								} else {
									log.add(new DecodeLog(WARNING, "LNオブジェクトの対応が取れません。レーン: " + key
											+ " - Time(ms):" + tl2.getTime()));
									break;
								}
							}
						}
					} else {
						tl.setNote(key, new NormalNote(wavmap[data]));
					}							
				});
				break;				
			case P1_INVISIBLE_KEY_BASE:
				this.processData(line, (pos, data) -> {
					getTimeLine(sectionnum + rate * pos).setHiddenNote(key, new NormalNote(wavmap[data]));
				});
				break;				
			case P1_LONG_KEY_BASE:
				this.processData(line, (pos, data) -> {
					// long note
					final TimeLine tl = getTimeLine(sectionnum + rate * pos);
					boolean insideln = false;
					if (!insideln && lnlist[key] != null) {
						final double section = tl.getSection();
						for (LongNote ln : lnlist[key]) {
							if (ln.getSection() <= section && section <= ln.getPair().getSection()) {
								insideln = true;
								break;
							}
						}
					}

					if(!insideln) {
						// LN処理
						if (startln[key] == null) {
							if(tl.existNote(key)) {
								Note note = tl.getNote(key);
								log.add(new DecodeLog(WARNING, "LN開始位置に通常ノートが存在します。レーン: "
										+ (key + 1) + " - Time(ms):" + tl.getTime()));
								if(note instanceof NormalNote && note.getWav() != wavmap[data]) {
									tl.addBackGroundNote(note);
								}								
							}
							LongNote ln = new LongNote(wavmap[data]);
							tl.setNote(key, ln);
							startln[key] = ln;
						} else if(startln[key].getSection() == Double.MIN_VALUE){
							startln[key] = null;
						} else {
							// LN終端処理
							for (Map.Entry<Double, TimeLineCache> e : tlcache.descendingMap().entrySet()) {
								if(e.getKey() >= tl.getSection()) {
									continue;
								}
								
								final TimeLine tl2 = e.getValue().timeline;									
								if(tl2.getSection() == startln[key].getSection()) {
									Note note = startln[key];
									((LongNote)note).setType(lnmode);
									LongNote noteend = new LongNote(startln[key].getWav() != wavmap[data] ? wavmap[data] : -2);
									tl.setNote(key, noteend);
									((LongNote)note).setPair(noteend);
									if (lnlist[key] == null) {
										lnlist[key] = new ArrayList<LongNote>();
									}
									lnlist[key].add((LongNote) note);											
									
									startln[key] = null;
									break;										
								} else if(tl2.existNote(key)){
									Note note = tl2.getNote(key);
									log.add(new DecodeLog(WARNING, "LN内に通常ノートが存在します。レーン: "
											+ (key + 1) + " - Time(ms):" + tl2.getTime()));
									tl2.setNote(key, null);
									if(note instanceof NormalNote) {
										tl2.addBackGroundNote(note);
									}
								}										
							}
						}								
					} else {
						if (startln[key] == null) {
							LongNote ln = new LongNote(wavmap[data]);
							ln.setSection(Double.MIN_VALUE);
							startln[key] = ln;
							log.add(new DecodeLog(WARNING, "LN内にLN開始ノートを定義しようとしています : "
									+ (key + 1) + " - Section : " + tl.getSection() + " - Time(ms):" + tl.getTime()));
						} else {
							if(startln[key].getSection() != Double.MIN_VALUE) {
								tlcache.get(startln[key].getSection()).timeline.setNote(key,  null);
							}
							startln[key] = null;										
							log.add(new DecodeLog(WARNING, "LN内にLN終端ノートを定義しようとしています : "
									+ (key + 1) + " - Section : " + tl.getSection() + " - Time(ms):" + tl.getTime()));
						}
					}
				});
				break;				
			case P1_MINE_KEY_BASE:
				// mine note
				this.processData(line, (pos, data) -> {
					final TimeLine tl = getTimeLine(sectionnum + rate * pos);
					boolean insideln = tl.existNote(key);
					if (!insideln && lnlist[key] != null) {
						final double section = tl.getSection();
						for (LongNote ln : lnlist[key]) {
							if (ln.getSection() <= section && section <= ln.getPair().getSection()) {
								insideln = true;
								break;
							}
						}
					}

					if(!insideln) {
						tl.setNote(key, new MineNote(wavmap[0], data));								
					} else {
						log.add(new DecodeLog(WARNING, "地雷ノート追加時に衝突が発生しました : " + (key + 1) + ":"
								+ tl.getTime()));								
					}
				});
				break;
			case LANE_AUTOPLAY:
				// BGレーン
				this.processData(line, (pos, data) -> {
					getTimeLine(sectionnum + rate * pos).addBackGroundNote(new NormalNote(wavmap[data]));
				});
				break;
			case BGA_PLAY:
				this.processData(line, (pos, data) -> {
					getTimeLine(sectionnum + rate * pos).setBGA(bgamap[data]);
				});
				break;
			case LAYER_PLAY:
				this.processData(line, (pos, data) -> {
					getTimeLine(sectionnum + rate * pos).setLayer(bgamap[data]);
				});
				break;

			}
		}
	}
	
	private TimeLine getTimeLine(double section) {
		final TimeLineCache tlc = tlcache.get(section);
		if (tlc != null) {
			return tlc.timeline;
		}
		
		Entry<Double, TimeLineCache> le = tlcache.lowerEntry(section);
		double scroll = le.getValue().timeline.getScroll();
		double bpm = le.getValue().timeline.getBPM();
		double time = le.getValue().time + le.getValue().timeline.getMicroStop() + (240000.0 * 1000 * (section  - le.getKey())) / bpm;			
		
		TimeLine tl = new TimeLine(section, (long)time, model.getMode().key);
		tl.setBPM(bpm);
		tl.setScroll(scroll);
		tlcache.put(section, new TimeLineCache(time, tl));
		return tl;
	}
}
