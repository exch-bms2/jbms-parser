package bms.model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

import bms.model.bmson.*;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * bmsonデコーダー
 * 
 * @author exch
 */
public class BMSONDecoder {

	private final ObjectMapper mapper = new ObjectMapper();

	private BMSModel model;

	private int lntype;

	private List<DecodeLog> log = new ArrayList<DecodeLog>();

	private final TreeMap<Integer, TimeLine> tlcache = new TreeMap<Integer, TimeLine>();
	private final TreeMap<Integer, Double> timecache = new TreeMap<Integer, Double>();

	private final int[] JUDGERANK = { 40, 60, 80, 100, 120 };

	public BMSONDecoder(int lntype) {
		this.lntype = lntype;
	}

	public BMSModel decode(File f) {
		return decode(f.toPath());
	}

	public BMSModel decode(Path f) {
		Logger.getGlobal().fine("BMSONファイル解析開始 :" + f.toString());
		log.clear();
		tlcache.clear();
		timecache.clear();
		try {
			final long currnttime = System.currentTimeMillis();
			// BMS読み込み、ハッシュ値取得
			model = new BMSModel();
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final Bmson bmson = mapper.readValue(
					new DigestInputStream(new BufferedInputStream(Files.newInputStream(f)), digest), Bmson.class);
			model.setSHA256(BMSDecoder.convertHexString(digest.digest()));
			model.setTitle(bmson.info.title);
			model.setSubTitle((bmson.info.subtitle != null ? bmson.info.subtitle : "")
					+ (bmson.info.subtitle != null && bmson.info.subtitle.length() > 0 && bmson.info.chart_name != null
							&& bmson.info.chart_name.length() > 0 ? " " : "")
					+ (bmson.info.chart_name != null && bmson.info.chart_name.length() > 0
							? "[" + bmson.info.chart_name + "]" : ""));
			model.setArtist(bmson.info.artist);
			StringBuilder subartist = new StringBuilder();
			for (String s : bmson.info.subartists) {
				subartist.append((subartist.length() > 0 ? "," : "") + s);
			}
			model.setSubArtist(subartist.toString());
			model.setGenre(bmson.info.genre);
			model.setJudgerank(bmson.info.judge_rank);
			if (model.getJudgerank() < 5) {
				int oldjudgerank = model.getJudgerank();
				if (model.getJudgerank() < 0) {
					model.setJudgerank(100);
				} else {
					model.setJudgerank(JUDGERANK[model.getJudgerank()]);
				}
				Logger.getGlobal().warning("judge_rankの定義が仕様通りでない可能性があるため、修正されました。judge_rank = " + oldjudgerank + " -> "
						+ model.getJudgerank());
				log.add(new DecodeLog(DecodeLog.STATE_WARNING, "judge_rankの定義が仕様通りでない可能性があるため、修正されました。judge_rank = "
						+ oldjudgerank + " -> " + model.getJudgerank()));
			}
			model.setTotal(bmson.info.total);
			model.setBpm(bmson.info.init_bpm);
			model.setPlaylevel(String.valueOf(bmson.info.level));
			model.setMode(Mode.BEAT_7K);
			for (Mode mode : Mode.values()) {
				if (mode.hint.equals(bmson.info.mode_hint)) {
					model.setMode(mode);
					break;
				}
			}
			List<LongNote>[] lnlist = new List[model.getMode().key];
			model.setLntype(lntype);

			model.setBanner(bmson.info.banner_image);
			model.setBackbmp(bmson.info.back_image);
			model.setStagefile(bmson.info.eyecatch_image);
			model.setPreview(bmson.info.preview_music);
			final TimeLine basetl = new TimeLine(0, 0, model.getMode().key);
			basetl.setBPM(model.getBpm());
			tlcache.put(0, basetl);
			timecache.put(0, 0.0);

			if (bmson.bpm_events == null) {
				bmson.bpm_events = new BpmEvent[0];
			}
			if (bmson.stop_events == null) {
				bmson.stop_events = new StopEvent[0];
			}

			final float resolution = bmson.info.resolution > 0 ? bmson.info.resolution * 4 : 960;
			final Comparator<BMSONObject> comparator = new Comparator<BMSONObject>() {
				@Override
				public int compare(BMSONObject n1, BMSONObject n2) {
					return n1.y - n2.y;
				}
			};

			int stoppos = 0;
			// bpmNotes, stopNotes処理
			Arrays.sort(bmson.bpm_events, comparator);
			Arrays.sort(bmson.stop_events, comparator);

			for (BpmEvent n : bmson.bpm_events) {
				while (stoppos < bmson.stop_events.length && bmson.stop_events[stoppos].y <= n.y) {
					final TimeLine tl = getTimeLine(bmson.stop_events[stoppos].y, resolution);
					tl.setStop((int) ((1000.0 * 60 * 4 * bmson.stop_events[stoppos].duration)
							/ (tl.getBPM() * resolution)));
					stoppos++;
				}
				getTimeLine(n.y, resolution).setBPM(n.bpm);
			}
			while (stoppos < bmson.stop_events.length) {
				final TimeLine tl = getTimeLine(bmson.stop_events[stoppos].y, resolution);
				tl.setStop(
						(int) ((1000.0 * 60 * 4 * bmson.stop_events[stoppos].duration) / (tl.getBPM() * resolution)));
				stoppos++;
			}
			// lines処理(小節線)
			if (bmson.lines != null) {
				for (BarLine bl : bmson.lines) {
					getTimeLine(bl.y, resolution).setSectionLine(true);
				}
			}

			List<String> wavmap = new ArrayList<String>(bmson.sound_channels.length);
			int id = 0;
			int starttime = 0;
			for (SoundChannel sc : bmson.sound_channels) {
				wavmap.add(sc.name);
				Arrays.sort(sc.notes, comparator);
				final int length = sc.notes.length;
				for (int i = 0; i < length; i++) {
					final bms.model.bmson.Note n = sc.notes[i];
					bms.model.bmson.Note next = null;
					for (int j = i + 1; j < length; j++) {
						if (sc.notes[j].y > n.y) {
							next = sc.notes[j];
							break;
						}
					}
					int duration = 0;
					if (!n.c) {
						starttime = 0;
					}
					TimeLine tl = getTimeLine(n.y, resolution);
					if (next != null && next.c) {
						duration = getTimeLine(next.y, resolution).getTime() - tl.getTime();
					}
					if (n.x == 0) {
						// BGノート
						tl.addBackGroundNote(new NormalNote(id, starttime, duration));
					} else {
						final int key = n.x - 1;

						boolean insideln = false;
						if (lnlist[key] != null) {
							final float section = (n.y / resolution);
							for (LongNote ln : lnlist[key]) {
								if (ln.getSection() < section && section <= ln.getEndnote().getSection()) {
									insideln = true;
									break;
								}
							}
						}

						if (insideln) {
							Logger.getGlobal().warning("LN内にノートを定義しています - x :  " + n.x + " y : " + n.y);
							log.add(new DecodeLog(DecodeLog.STATE_WARNING,
									"LN内にノートを定義しています - x :  " + n.x + " y : " + n.y));
							tl.addBackGroundNote(new NormalNote(id, starttime, duration));
						} else {
							if (n.l > 0) {
								// ロングノート
								TimeLine end = getTimeLine(n.y + n.l, resolution);
								LongNote ln = new LongNote(id, starttime);
								ln.setDuration(duration);
								if (tl.getNote(key) != null) {
									bms.model.Note en = tl.getNote(key);
									if (en instanceof LongNote && end.getNote(key) == en) {
										en.addLayeredNote(ln);
									} else {
										Logger.getGlobal()
												.warning("同一の位置にノートが複数定義されています - x :  " + n.x + " y : " + n.y);
										log.add(new DecodeLog(DecodeLog.STATE_WARNING,
												"同一の位置にノートが複数定義されています - x :  " + n.x + " y : " + n.y));
									}
								} else {
									boolean existNote = false;
									for(TimeLine tl2 : tlcache.subMap(n.y, false, n.y + n.l, true).values()) {
										if(tl2.existNote(key)) {
											existNote = true;
											break;
										}
									}
									if(existNote) {
										Logger.getGlobal().warning("LN内にノートを定義しています - x :  " + n.x + " y : " + n.y);
										log.add(new DecodeLog(DecodeLog.STATE_WARNING,
												"LN内にノートを定義しています - x :  " + n.x + " y : " + n.y));
										tl.addBackGroundNote(new NormalNote(id, starttime, duration));										
									} else {
										tl.setNote(key, ln);
										// ln.setDuration(end.getTime() -
										// start.getTime());
										end.setNote(key, ln);
										ln.setType(n.t);
										if (lnlist[key] == null) {
											lnlist[key] = new ArrayList<LongNote>();
										}
										lnlist[key].add(ln);										
									}
								}
							} else {
								// 通常ノート
								if (tl.existNote(key)) {
									if (tl.getNote(key) instanceof NormalNote) {
										tl.getNote(key).addLayeredNote(new NormalNote(id, starttime, duration));
									} else {
										Logger.getGlobal()
												.warning("同一の位置にノートが複数定義されています - x :  " + n.x + " y : " + n.y);
										log.add(new DecodeLog(DecodeLog.STATE_WARNING,
												"同一の位置にノートが複数定義されています - x :  " + n.x + " y : " + n.y));
									}
								} else {
									tl.setNote(key, new NormalNote(id, starttime, duration));
								}
							}
						}
					}
					starttime += duration;
				}
				id++;
			}
			model.setWavList(wavmap.toArray(new String[wavmap.size()]));
			// BGA処理
			if (bmson.bga != null && bmson.bga.bga_header != null) {
				final String[] bgamap = new String[bmson.bga.bga_header.length];
				final Map<Integer, Integer> idmap = new HashMap<Integer, Integer>(bmson.bga.bga_header.length);
				for (int i = 0; i < bmson.bga.bga_header.length; i++) {
					BGAHeader bh = bmson.bga.bga_header[i];
					bgamap[i] = bh.name;
					idmap.put(bh.id, i);
				}
				if (bmson.bga.bga_events != null) {
					for (BNote n : bmson.bga.bga_events) {
						getTimeLine(n.y, resolution).setBGA(idmap.get(n.id));
					}
				}
				if (bmson.bga.layer_events != null) {
					for (BNote n : bmson.bga.layer_events) {
						getTimeLine(n.y, resolution).setLayer(idmap.get(n.id));
					}
				}
				if (bmson.bga.poor_events != null) {
					for (BNote n : bmson.bga.poor_events) {
						getTimeLine(n.y, resolution).setPoor(new int[] { idmap.get(n.id) });
					}
				}
				model.setBgaList(bgamap);
			}
			model.setAllTimeLine(tlcache.values().toArray(new TimeLine[tlcache.size()]));

			Logger.getGlobal().fine("BMSONファイル解析完了 :" + f.toString() + " - TimeLine数:" + tlcache.size() + " 時間(ms):"
					+ (System.currentTimeMillis() - currnttime));
			model.setPath(f.toAbsolutePath().toString());
			return model;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	private TimeLine getTimeLine(int y, float resolution) {
		// Timeをus単位にする場合はこのメソッド内部だけ変更すればOK
		final TimeLine tlc = tlcache.get(y);
		if (tlc != null) {
			return tlc;
		}

		Entry<Integer, TimeLine> le = tlcache.lowerEntry(y);
		double bpm = le.getValue().getBPM();
		double time = timecache.get(le.getKey()) + le.getValue().getStop() + (240000.0 * ((y  - le.getKey()) / resolution)) / bpm;			

		TimeLine tl = new TimeLine(y / resolution, (int) time, model.getMode().key);
		tl.setBPM(bpm);
		tlcache.put(y, tl);
		timecache.put(y, time);
		// System.out.println("y = " + y + " , bpm = " + bpm + " , time = " +
		// tl.getTime());
		return tl;
	}
}
