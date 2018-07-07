package bms.model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;
import static bms.model.DecodeLog.State.*;

import bms.model.BMSDecoder.TimeLineCache;
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

	private final TreeMap<Integer, TimeLineCache> tlcache = new TreeMap<Integer, TimeLineCache>();

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
		final long currnttime = System.currentTimeMillis();
		// BMS読み込み、ハッシュ値取得
		model = new BMSModel();
		Bmson bmson = null;
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			bmson = mapper.readValue(new DigestInputStream(new BufferedInputStream(Files.newInputStream(f)), digest),
					Bmson.class);
			model.setSHA256(BMSDecoder.convertHexString(digest.digest()));
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
			return null;
		}
		
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
			log.add(new DecodeLog(WARNING, "judge_rankの定義が仕様通りでない可能性があります。judge_rank = " + model.getJudgerank()));
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
		if (bmson.info.ln_type > 0 && bmson.info.ln_type <= 3) {
			model.setLnmode(bmson.info.ln_type);
		}
		final int[] keyassign;
		switch (model.getMode()) {
		case BEAT_5K:
			keyassign = new int[] { 0, 1, 2, 3, 4, -1, -1, 5 };
			break;
		case BEAT_10K:
			keyassign = new int[] { 0, 1, 2, 3, 4, -1, -1, 5, 6, 7, 8, 9, 10, -1, -1, 11 };
			break;
		default:
			keyassign = new int[model.getMode().key];
			for (int i = 0; i < keyassign.length; i++) {
				keyassign[i] = i;
			}
		}
		List<LongNote>[] lnlist = new List[model.getMode().key];
		Map<bms.model.bmson.Note, LongNote> lnup = new HashMap();
		model.setLntype(lntype);

		model.setBanner(bmson.info.banner_image);
		model.setBackbmp(bmson.info.back_image);
		model.setStagefile(bmson.info.eyecatch_image);
		model.setPreview(bmson.info.preview_music);
		final TimeLine basetl = new TimeLine(0, 0, model.getMode().key);
		basetl.setBPM(model.getBpm());
		tlcache.put(0, new TimeLineCache(0.0, basetl));

		if (bmson.bpm_events == null) {
			bmson.bpm_events = new BpmEvent[0];
		}
		if (bmson.stop_events == null) {
			bmson.stop_events = new StopEvent[0];
		}

		final double resolution = bmson.info.resolution > 0 ? bmson.info.resolution * 4 : 960;
		final Comparator<BMSONObject> comparator = new Comparator<BMSONObject>() {
			@Override
			public int compare(BMSONObject n1, BMSONObject n2) {
				return n1.y - n2.y;
			}
		};

		int bpmpos = 0;
		int stoppos = 0;
		// bpmNotes, stopNotes処理
		Arrays.sort(bmson.bpm_events, comparator);
		Arrays.sort(bmson.stop_events, comparator);

		while (bpmpos < bmson.bpm_events.length || stoppos < bmson.stop_events.length) {
			final int bpmy = bpmpos < bmson.bpm_events.length ? bmson.bpm_events[bpmpos].y : Integer.MAX_VALUE;
			final int stopy = stoppos < bmson.stop_events.length ? bmson.stop_events[stoppos].y : Integer.MAX_VALUE;
			if (bpmy <= stopy) {
				getTimeLine(bpmy, resolution).setBPM(bmson.bpm_events[bpmpos].bpm);
				bpmpos++;
			} else if (stopy != Integer.MAX_VALUE) {
				final TimeLine tl = getTimeLine(stopy, resolution);
				tl.setStop((long) ((1000.0 * 1000 * 60 * 4 * bmson.stop_events[stoppos].duration)
						/ (tl.getBPM() * resolution)));
				stoppos++;
			}
		}
		// lines処理(小節線)
		if (bmson.lines != null) {
			for (BarLine bl : bmson.lines) {
				getTimeLine(bl.y, resolution).setSectionLine(true);
			}
		}

		String[] wavmap = new String[bmson.sound_channels.length];
		int id = 0;
		long starttime = 0;
		for (SoundChannel sc : bmson.sound_channels) {
			wavmap[id] = sc.name;
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
				long duration = 0;
				if (!n.c) {
					starttime = 0;
				}
				TimeLine tl = getTimeLine(n.y, resolution);
				if (next != null && next.c) {
					duration = getTimeLine(next.y, resolution).getMicroTime() - tl.getMicroTime();
				}

				final int key = n.x > 0 && n.x <= keyassign.length ? keyassign[n.x - 1] : -1;
				if (key < 0) {
					// BGノート
					tl.addBackGroundNote(new NormalNote(id, starttime, duration));
				} else if (n.up) {
					// LN終端音定義
					boolean assigned = false;
					if (lnlist[key] != null) {
						final double section = (n.y / resolution);
						for (LongNote ln : lnlist[key]) {
							if (section == ln.getPair().getSection()) {
								ln.getPair().setWav(id);
								ln.getPair().setStarttime(starttime);
								ln.getPair().setDuration(duration);
								assigned = true;
								break;
							}
						}
						if(!assigned) {
							lnup.put(n, new LongNote(id, starttime, duration));
						}
					}
				} else {
					boolean insideln = false;
					if (lnlist[key] != null) {
						final double section = (n.y / resolution);
						for (LongNote ln : lnlist[key]) {
							if (ln.getSection() < section && section <= ln.getPair().getSection()) {
								insideln = true;
								break;
							}
						}
					}

					if (insideln) {
						log.add(new DecodeLog(WARNING,
								"LN内にノートを定義しています - x :  " + n.x + " y : " + n.y));
						tl.addBackGroundNote(new NormalNote(id, starttime, duration));
					} else {
						if (n.l > 0) {
							// ロングノート
							TimeLine end = getTimeLine(n.y + n.l, resolution);
							LongNote ln = new LongNote(id, starttime, duration);
							if (tl.getNote(key) != null) {
								// レイヤーノート判定
								bms.model.Note en = tl.getNote(key);
								if (en instanceof LongNote && end.getNote(key) == ((LongNote) en).getPair()) {
									en.addLayeredNote(ln);
								} else {
									log.add(new DecodeLog(WARNING,
											"同一の位置にノートが複数定義されています - x :  " + n.x + " y : " + n.y));
								}
							} else {
								boolean existNote = false;
								for (TimeLineCache tl2 : tlcache.subMap(n.y, false, n.y + n.l, true).values()) {
									if (tl2.timeline.existNote(key)) {
										existNote = true;
										break;
									}
								}
								if (existNote) {
									log.add(new DecodeLog(WARNING,
											"LN内にノートを定義しています - x :  " + n.x + " y : " + n.y));
									tl.addBackGroundNote(new NormalNote(id, starttime, duration));
								} else {
									tl.setNote(key, ln);
									// ln.setDuration(end.getTime() -
									// start.getTime());
									LongNote lnend = null;
									for (Entry<bms.model.bmson.Note, LongNote> up : lnup.entrySet()) {
										if (up.getKey().y == n.y + n.l && up.getKey().x == n.x) {
											lnend = up.getValue();
											break;
										}
									}
									if(lnend == null) {
										lnend = new LongNote(-2);
									}

									end.setNote(key, lnend);
									ln.setType(n.t > 0 && n.t <= 3 ? n.t : model.getLnmode());
									ln.setPair(lnend);
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
									log.add(new DecodeLog(WARNING,
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
		model.setWavList(wavmap);
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
		TimeLine[] tl = new TimeLine[tlcache.size()];
		int tlcount = 0;
		for(TimeLineCache tlc : tlcache.values()) {
			tl[tlcount] = tlc.timeline;
			tlcount++;
		}
		model.setAllTimeLine(tl);

		Logger.getGlobal().fine("BMSONファイル解析完了 :" + f.toString() + " - TimeLine数:" + tlcache.size() + " 時間(ms):"
				+ (System.currentTimeMillis() - currnttime));
		model.setPath(f.toAbsolutePath().toString());
		return model;
	}

	private TimeLine getTimeLine(int y, double resolution) {
		// Timeをus単位にする場合はこのメソッド内部だけ変更すればOK
		final TimeLineCache tlc = tlcache.get(y);
		if (tlc != null) {
			return tlc.timeline;
		}

		Entry<Integer, TimeLineCache> le = tlcache.lowerEntry(y);
		double bpm = le.getValue().timeline.getBPM();
		double time = le.getValue().time + le.getValue().timeline.getMicroStop()
				+ (240000.0 * 1000 * ((y - le.getKey()) / resolution)) / bpm;

		TimeLine tl = new TimeLine(y / resolution, (long) time, model.getMode().key);
		tl.setBPM(bpm);
		tlcache.put(y, new TimeLineCache(time, tl));
		// System.out.println("y = " + y + " , bpm = " + bpm + " , time = " +
		// tl.getTime());
		return tl;
	}	
}
