package bms.model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Logger;

import bms.model.bmson.*;
import bms.model.bmson.Note;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
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

	public BMSONDecoder(int lntype) {
		this.lntype = lntype;
	}

	private final int[] JUDGERANK = { 40, 60, 80, 100, 120 };

	public BMSModel decode(File f) {
		return decode(f.toPath());
	}

	public BMSModel decode(Path f) {
		Logger.getGlobal().fine("BMSONファイル解析開始 :" + f.toString());
		log.clear();
		tlcache.clear();
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
					+ (bmson.info.chart_name != null && bmson.info.chart_name.length() > 0 ? "["
							+ bmson.info.chart_name + "]" : ""));
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
				Logger.getGlobal().warning(
						"judge_rankの定義が仕様通りでない可能性があるため、修正されました。judge_rank = " + oldjudgerank + " -> "
								+ model.getJudgerank());
				log.add(new DecodeLog(DecodeLog.STATE_WARNING, "judge_rankの定義が仕様通りでない可能性があるため、修正されました。judge_rank = "
						+ oldjudgerank + " -> " + model.getJudgerank()));
			}
			model.setTotal(bmson.info.total);
			model.setBpm(bmson.info.init_bpm);
			model.setPlaylevel(String.valueOf(bmson.info.level));
			model.setMode(Mode.BEAT_7K);
			for(Mode mode : Mode.values()) {
				if(mode.hint.equals(bmson.info.mode_hint)) {
					model.setMode(mode);
					break;
				}
			}
//			if (bmson.info.mode_hint != null) {
//				
//				switch (bmson.info.mode_hint.toLowerCase()) {
//				case "beat-5k":
//					model.setUseKeys(5);
//					break;
//				case "beat-7k":
//					model.setUseKeys(7);
//					break;
//				case "beat-10k":
//					model.setUseKeys(10);
//					break;
//				case "beat-14k":
//					model.setUseKeys(14);
//					break;
//				case "popn-5k":
//					model.setUseKeys(9);
//					assign = TimeLine.NOTEASSIGN_POPN;
//					break;
//				case "popn-9k":
//					model.setUseKeys(9);
//					assign = TimeLine.NOTEASSIGN_POPN;
//					break;
//				case "keyboard-24k":
//				case "keyboard-24k-single":
//					model.setUseKeys(24);
//					assign = TimeLine.NOTEASSIGN_KB_24KEY;
//					break;
//				}
//			}
			model.setLntype(lntype);

			model.setBanner(bmson.info.banner_image);
			model.setBackbmp(bmson.info.back_image);
			model.setStagefile(bmson.info.eyecatch_image);
			model.setPreview(bmson.info.preview_music);
			if (bmson.bpm_events == null) {
				bmson.bpm_events = new BpmEvent[0];
			}
			if (bmson.stop_events == null) {
				bmson.stop_events = new StopEvent[0];
			}

			final float resolution = bmson.info.resolution > 0 ? bmson.info.resolution * 4 : 960;

			int stoppos = 0;
			// bpmNotes, stopNotes処理
			for (BpmEvent n : bmson.bpm_events) {
				while (stoppos < bmson.stop_events.length && bmson.stop_events[stoppos].y <= n.y) {
					final TimeLine tl = getTimeLine(bmson.stop_events[stoppos].y, resolution);
					tl.setStop((int) ((1000.0 * 60 * 4 * bmson.stop_events[stoppos].duration) / (tl.getBPM() * resolution)));
					stoppos++;
				}
				getTimeLine(n.y, resolution).setBPM(n.bpm);
			}
			while (stoppos < bmson.stop_events.length) {
				final TimeLine tl = getTimeLine(bmson.stop_events[stoppos].y, resolution);
				tl.setStop((int) ((1000.0 * 60 * 4 * bmson.stop_events[stoppos].duration) / (tl.getBPM() * resolution)));
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
			final Comparator<Note> comparator = new Comparator<Note>() {
				@Override
				public int compare(Note n1, Note n2) {
					return n1.y - n2.y;
				}
			};
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
					if (next != null && next.c) {
						duration = getTimeLine(next.y, resolution).getTime() - getTimeLine(n.y, resolution).getTime();
					}
					if (n.x == 0) {
						// BGノート
						TimeLine tl = getTimeLine(n.y, resolution);
						tl.addBackGroundNote(new NormalNote(id, starttime, duration));
					} else {
						final int key = n.x - 1;
						if (n.l > 0) {
							// ロングノート
							TimeLine start = getTimeLine(n.y, resolution);
							TimeLine end = getTimeLine(n.y + n.l, resolution);
							LongNote ln = new LongNote(id, starttime);
							ln.setDuration(duration);
							if (start.getNote(key) != null) {
								bms.model.Note en = start.getNote(key);
								if (en instanceof LongNote && end.getNote(key) == en) {
									en.addLayeredNote(ln);
								} else {
									Logger.getGlobal().warning("同一の位置にノートが複数定義されています - x :  " + n.x + " y : " + n.y);
									log.add(new DecodeLog(DecodeLog.STATE_WARNING, "同一の位置にノートが複数定義されています - x :  " + n.x
											+ " y : " + n.y));
								}
							} else {
								start.setNote(key, ln);
								// ln.setDuration(end.getTime() -
								// start.getTime());
								end.setNote(key, ln);
								ln.setType(n.t);
							}
						} else {
							// 通常ノート
							final TimeLine tl = getTimeLine(n.y, resolution);
							if (tl.existNote(key)) {
								if (tl.getNote(key) instanceof NormalNote) {
									tl.getNote(key).addLayeredNote(new NormalNote(id, starttime, duration));
								} else {
									Logger.getGlobal().warning("同一の位置にノートが複数定義されています - x :  " + n.x + " y : " + n.y);
									log.add(new DecodeLog(DecodeLog.STATE_WARNING, "同一の位置にノートが複数定義されています - x :  " + n.x
											+ " y : " + n.y));
								}
							} else {
								tl.setNote(key, new NormalNote(id, starttime, duration));
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
						TimeLine tl = getTimeLine(n.y, resolution);
						tl.setBGA(idmap.get(n.id));
					}
				}
				if (bmson.bga.layer_events != null) {
					for (BNote n : bmson.bga.layer_events) {
						TimeLine tl = getTimeLine(n.y, resolution);
						tl.setLayer(idmap.get(n.id));
					}
				}
				if (bmson.bga.poor_events != null) {
					for (BNote n : bmson.bga.poor_events) {
						TimeLine tl = getTimeLine(n.y, resolution);
						tl.setPoor(new int[] { idmap.get(n.id) });
					}
				}
				model.setBgaList(bgamap);
			}

			Logger.getGlobal().fine(
					"BMSONファイル解析完了 :" + f.toString() + " - TimeLine数:" + tlcache.size() + " 時間(ms):"
							+ (System.currentTimeMillis() - currnttime));
			model.setPath(f.toAbsolutePath().toString());
			return model;
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private final TreeMap<Integer, TimeLine> tlcache = new TreeMap<Integer, TimeLine>();

	private TimeLine getTimeLine(int y, float resolution) {
		final TimeLine tlc = tlcache.get(y);
		if (tlc != null) {
			return tlc;
		}
		double bpm = model.getBpm();
		double time = 0;
		double section = 0;
				
		if (tlcache.size() == 0) {
			TimeLine tl = model.getTimeLine(0, 0);
			tl.setBPM(bpm);
			tlcache.put(0, tl);
		}

		for (TimeLine tl : tlcache.values()) {
			if (tl.getSection() > y / resolution) {
				time += (240000.0 * (y / resolution - section)) / bpm;
				break;
			} else {
				time += (240000.0 * (tl.getSection() - section)) / bpm;
				bpm = tl.getBPM();
				section = tl.getSection();
			}
			time += tl.getStop();
		}
		if (tlcache.lastEntry().getValue().getSection() < y / resolution) {
			time += (240000.0 * (y / resolution - section)) / bpm;
		}
		
		TimeLine tl = model.getTimeLine(y / resolution, (int) time);
		tl.setBPM(bpm);
		tlcache.put(y, tl);
		// System.out.println("y = " + y + " , bpm = " + bpm + " , time = " +
		// tl.getTime());
		return tl;
	}
}
