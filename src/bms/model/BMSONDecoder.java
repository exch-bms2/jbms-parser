package bms.model;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

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

	private BMSModel model;

	private int lntype;

	private List<DecodeLog> log = new ArrayList<DecodeLog>();

	public BMSONDecoder(int lntype) {
		this.lntype = lntype;
	}

	private final int[] JUDGERANK = { 40, 70, 90, 100 };

	public BMSModel decode(File f) {
		Logger.getGlobal().info("BMSONファイル解析開始 :" + f.getName());
		log.clear();
		tlcache.clear();
		try {
			final long currnttime = System.currentTimeMillis();
			// BMS読み込み、ハッシュ値取得
			model = new BMSModel();
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final byte[] data = IOUtils.toByteArray(new DigestInputStream(new FileInputStream(f), digest));
			model.setSHA256(BMSDecoder.convertHexString(digest.digest()));
			final Bmson bmson = new ObjectMapper().readValue(new ByteArrayInputStream(data), Bmson.class);
			model.setTitle(bmson.info.title);
			model.setSubTitle(bmson.info.subtitle);
			model.setArtist(bmson.info.artist);
			StringBuilder subartist = new StringBuilder();
			for (String s : bmson.info.subartists) {
				subartist.append((subartist.length() > 0 ? "," : "") + s);
			}
			model.setSubArtist(subartist.toString());
			model.setGenre(bmson.info.genre);
			model.setJudgerank(bmson.info.judge_rank);
			if (model.getJudgerank() < 4) {
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
			model.setUseKeys(7);
			int[] assign = TimeLine.NOTEASSIGN_BEAT;
			if(bmson.info.mode_hint != null) {
				switch(bmson.info.mode_hint.toLowerCase()) {
				case "beat-5k":
					model.setUseKeys(5);
					break;
				case "beat-7k":
					model.setUseKeys(7);
					break;
				case "beat-10k":
					model.setUseKeys(10);					
					break;
				case "beat-14k":
					model.setUseKeys(14);					
					break;
				case "popn-5k":
					model.setUseKeys(9);
					assign = TimeLine.NOTEASSIGN_POPN;
					break;
				case "popn-9k":
					model.setUseKeys(9);
					assign = TimeLine.NOTEASSIGN_POPN;
					break;
				}
			}
			model.setLntype(lntype);

			model.setBanner(bmson.info.banner_image);
			model.setBackbmp(bmson.info.back_image);
			model.setStagefile(bmson.info.eyecatch_image);

			final float resolution = bmson.info.resolution > 0 ? bmson.info.resolution * 4 : 960;

			int stoppos = 0;
			// bpmNotes, stopNotes処理
			for (EventNote n : bmson.bpm_events) {
				while (stoppos < bmson.stop_events.length && bmson.stop_events[stoppos].y <= n.y) {
					TimeLine tl = getTimeLine(bmson.stop_events[stoppos].y, resolution);
					tl.setStop((int) ((1000.0 * 60 * 4 * bmson.stop_events[stoppos].v) / (tl.getBPM() * resolution)));
					stoppos++;
				}
				getTimeLine(n.y, resolution).setBPM(n.v);
			}
			while (stoppos < bmson.stop_events.length) {
				TimeLine tl = getTimeLine(bmson.stop_events[stoppos].y, resolution);
				tl.setStop((int) ((1000.0 * 60 * 4 * bmson.stop_events[stoppos].v) / (tl.getBPM() * resolution)));
				stoppos++;
			}
			// lines処理(小節線)
			for (BarLine bl : bmson.lines) {
				getTimeLine(bl.y, resolution).setSectionLine(true);
			}

			List<String> wavmap = new ArrayList<String>();
			int id = 0;
			int starttime = 0;
			for (SoundChannel sc : bmson.sound_channels) {
				wavmap.add(sc.name);
				Arrays.sort(sc.notes, new Comparator<bms.model.bmson.Note>() {
					@Override
					public int compare(Note n1, Note n2) {
						return n1.y - n2.y;
					}
				});
				for (int i = 0; i < sc.notes.length; i++) {
					final bms.model.bmson.Note n = sc.notes[i];
					bms.model.bmson.Note next = null;
					for(int j = i + 1; j < sc.notes.length; j++) {
						if(sc.notes[j].y > n.y) {
							next = sc.notes[j];
							break;
						}
					}
					int duration = 0;
					if (!n.c) {
						starttime = 0;
					}
					if (next != null && next.c) {
						duration = getTimeLine(next.y, resolution).getTime()
								- getTimeLine(n.y, resolution).getTime();
					}
					if (n.x == 0) {
						// BGノート
						TimeLine tl = getTimeLine(n.y, resolution);
						tl.addBackGroundNote(new NormalNote(id, starttime, duration));
					} else {
						final int key = assign[n.x - 1];
						if (n.l > 0) {
							// ロングノート
							TimeLine start = getTimeLine(n.y, resolution);
							LongNote ln = new LongNote(id, starttime, start);
							start.setNote(key, ln);
							TimeLine end = getTimeLine(n.y + n.l, resolution);
							ln.setEnd(end);
							ln.setDuration(end.getTime() - start.getTime());
							end.setNote(key, ln);
							ln.setType(n.t);
						} else {
							// 通常ノート
							TimeLine tl = getTimeLine(n.y, resolution);
							if (tl.existNote(key)) {
								Logger.getGlobal().warning("同一の位置にノートが複数定義されています - x :  " + n.x + " y : " + n.y);
								log.add(new DecodeLog(DecodeLog.STATE_WARNING, "同一の位置にノートが複数定義されています - x :  " + n.x
										+ " y : " + n.y));
							}
							tl.setNote(key, new NormalNote(id, starttime, duration));
						}
					}
					starttime += duration;
				}
				id++;
			}
			model.setWavList(wavmap.toArray(new String[0]));
			// BGA処理
			List<String> bgamap = new ArrayList<String>();
			Map<Integer, Integer> idmap = new HashMap<Integer, Integer>();
			if (bmson.bga != null && bmson.bga.bga_header != null) {
				for (int i = 0; i < bmson.bga.bga_header.length; i++) {
					BGAHeader bh = bmson.bga.bga_header[i];
					bgamap.add(bh.name);
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
			}
			model.setBgaList(bgamap.toArray(new String[0]));

			Logger.getGlobal().info(
					"BMSONファイル解析完了 :" + f.getName() + " - TimeLine数:" + model.getAllTimes().length + " 時間(ms):"
							+ (System.currentTimeMillis() - currnttime));
			model.setPath(f.getAbsolutePath());
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
	
	private final Map<Integer, TimeLine> tlcache = new HashMap<Integer, TimeLine>(); 

	private TimeLine getTimeLine(int y, float resolution) {
		if(tlcache.containsKey(y)) {
			return tlcache.get(y);
		}
		double bpm = model.getBpm();
		double time = 0;
		double section = 0;
		TimeLine[] timelines = model.getAllTimeLines();
		for (TimeLine tl : timelines) {
			if (tl.getSection() > y / resolution) {
				time += (1000.0 * 60 * 4 * (y / resolution - section)) / bpm;
				break;
			} else {
				time += (1000.0 * 60 * 4 * (tl.getSection() - section)) / bpm;
				bpm = tl.getBPM();
				section = tl.getSection();
			}
			time += tl.getStop();
		}
		if (timelines.length > 0 && timelines[timelines.length - 1].getSection() < y / resolution) {
			time += (1000.0 * 60 * 4 * (y / resolution - section)) / bpm;
		}
		TimeLine tl = model.getTimeLine(y / resolution, (int) time);
		tl.setBPM(bpm);
		tlcache.put(y, tl);
		return tl;
	}
}
