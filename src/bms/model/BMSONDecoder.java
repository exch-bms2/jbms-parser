package bms.model;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import bms.model.bmson.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * bmsonデコーダー
 * 
 * @author exch
 */
public class BMSONDecoder {

	private int lntype;

	private List<DecodeLog> log = new ArrayList<DecodeLog>();

	public BMSONDecoder(int lntype) {
		this.lntype = lntype;
	}

	private final int[] JUDGERANK = { 40, 70, 90, 100 };

	public BMSModel decode(File f) {
		Logger.getGlobal().info("BMSONファイル解析開始 :" + f.getName());
		log.clear();
		try {
			// BMS読み込み、ハッシュ値取得
			BMSModel model = new BMSModel();
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] data = IOUtils.toByteArray(new DigestInputStream(new FileInputStream(f), digest));
			model.setSHA256(BMSDecoder.convertHexString(digest.digest()));
			ObjectMapper mapper = new ObjectMapper();
			Bmson bmson = mapper.readValue(new ByteArrayInputStream(data), Bmson.class);
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
			model.setLntype(lntype);

			model.setBanner(bmson.info.banner_image);
			model.setBackbmp(bmson.info.back_image);
			model.setStagefile(bmson.info.eyecatch_image);

			float resolution = 960;
			if (bmson.info.resolution > 0) {
				resolution = bmson.info.resolution * 4;
			}
			double nowbpm = model.getBpm();
			// TODO BPM変化に伴うTime算出
			// bpmNotes処理
			for (EventNote n : bmson.bpm_events) {
				model.getTimeLine(n.y / 960f, (int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * resolution))).setBPM(n.v);
			}

			// lines処理(小節線)
			for (BarLine bl : bmson.lines) {
				TimeLine tl = model.getTimeLine(bl.y / resolution,
						(int) ((1000.0 * 60 * 4 * bl.y) / (nowbpm * resolution)));
				tl.setSectionLine(true);
				tl.setBPM(nowbpm);
			}
			// stopNotes処理
			for (EventNote n : bmson.stop_events) {
				TimeLine tl = model.getTimeLine(n.y / resolution,
						(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * resolution)));
				tl.setStop((int) ((1000.0 * 60 * 4 * n.v) / (nowbpm * resolution)));
				tl.setBPM(nowbpm);
			}

			List<String> wavmap = new ArrayList<String>();
			int id = 0;
			int starttime = 0;
			for (SoundChannel sc : bmson.sound_channels) {
				wavmap.add(sc.name);
				for (int i = 0; i < sc.notes.length; i++) {
					bms.model.bmson.Note n = sc.notes[i];
					int duration = Integer.MAX_VALUE;
					if (!n.c) {
						starttime = 0;
					}
					if (i < sc.notes.length - 1) {
						duration = (int) (1000.0 * 60 * 4 * (sc.notes[i + 1].y - n.y) / (nowbpm * resolution));
					}
					if (n.x == 0) {
						// BGノート
						TimeLine tl = model.getTimeLine(n.y / resolution,
								(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * resolution)));
						tl.addBackGroundNote(new NormalNote(id, starttime, duration));
						tl.setBPM(nowbpm);
					} else {
						if (n.l > 0) {
							// ロングノート
							TimeLine start = model.getTimeLine(n.y / resolution,
									(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * resolution)));
							LongNote ln = new LongNote(id, starttime, start);
							start.setNote(n.x - 1, ln);
							start.setBPM(nowbpm);
							TimeLine end = model.getTimeLine((n.y + n.l) / resolution,
									(int) ((1000.0 * 60 * 4 * (n.y + n.l)) / (nowbpm * resolution)));
							ln.setEnd(end);
							end.setNote(n.x - 1, ln);
							end.setBPM(nowbpm);
						} else {
							// 通常ノート
							TimeLine tl = model.getTimeLine(n.y / resolution,
									(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * resolution)));
							if (tl.existNote(n.x - 1)) {
								Logger.getGlobal().warning("同一の位置にノートが複数定義されています - x :  " + n.x + " y : " + n.y);
								log.add(new DecodeLog(DecodeLog.STATE_WARNING, "同一の位置にノートが複数定義されています - x :  " + n.x
										+ " y : " + n.y));
							}
							tl.setNote(n.x - 1, new NormalNote(id, starttime, duration));
							tl.setBPM(nowbpm);
						}
					}
					starttime += duration;
				}
				id++;
			}
			// TODO BGA処理
			model.setWavList(wavmap.toArray(new String[0]));
			Logger.getGlobal().info("BMSONファイル解析完了 :" + f.getName() + " - TimeLine数:" + model.getAllTimes().length);
			return model;
		} catch (JsonParseException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return null;
	}
}
