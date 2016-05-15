package bms.model;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Logger;

import javax.imageio.stream.FileImageInputStream;

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

	public BMSONDecoder(int lntype) {
		this.lntype = lntype;
	}

	public BMSModel decode(File f) {
		Logger.getGlobal().info("BMSONファイル解析開始 :" + f.getName());
		BMSModel model = new BMSModel();
		
		try {
			// BMS読み込み、ハッシュ値取得
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] data = IOUtils.toByteArray(new DigestInputStream(new FileInputStream(f), digest));
			model.setHash(BMSDecoder.convertHexString(digest.digest()));
			ObjectMapper mapper = new ObjectMapper();
			Bmson bmson = mapper.readValue(new ByteArrayInputStream(data), Bmson.class);
			model.setTitle(bmson.info.title);
			model.setArtist(bmson.info.artist);
			model.setGenre(bmson.info.genre);
			model.setJudgerank(bmson.info.judge_rank);
			model.setTotal(bmson.info.total);
			model.setBpm(bmson.info.init_bpm);
			model.setPlaylevel(String.valueOf(bmson.info.level));
			model.setUseKeys(7);
			model.setLntype(lntype);
			double nowbpm = model.getBpm();
			// TODO BPM変化に伴うTime算出
			// bpmNotes処理
			for (EventNote n : bmson.bpm_events) {
				model.getTimeLine(n.y / 960f,
						(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * 960)))
						.setBPM(n.v);
			}

			// lines処理(小節線)
			for (BarLine bl : bmson.lines) {
				TimeLine tl = model.getTimeLine(bl.y / 960f,
						(int) ((1000.0 * 60 * 4 * bl.y) / (nowbpm * 960)));
				tl.setSectionLine(true);
				tl.setBPM(nowbpm);
			}
			// stopNotes処理
			for (EventNote n : bmson.stop_events) {
				TimeLine tl = model.getTimeLine(n.y / 960f,
						(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * 960)));
				tl.setStop((int) ((1000.0 * 60 * 4 * n.v) / (nowbpm * 960)));
				tl.setBPM(nowbpm);
			}

			List<String> wavmap = new ArrayList<String>();
			int id = 0;
			int starttime = 0;
			for (SoundChannel sc : bmson.sound_channels) {
				wavmap.add(sc.name);
				for (int i = 0 ; i < sc.notes.length;i++) {
					bms.model.bmson.Note n = sc.notes[i];
					int duration = Integer.MAX_VALUE;
					if(!n.c) {
						starttime = 0;
					}
					if(i < sc.notes.length - 1) {
						duration = (int) (1000.0 * 60 * 4 * (sc.notes[i + 1].y - n.y) / (nowbpm * 960));
					}
					if (n.x == 0) {
						TimeLine tl = model
								.getTimeLine(
										n.y / 960f,
										(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * 960)));
						tl.addBackGroundNote(new NormalNote(id, starttime, duration));
						tl.setBPM(nowbpm);
					} else {
						if (n.l > 0) {
							// ロングノート
							TimeLine start = model
									.getTimeLine(
											n.y / 960f,
											(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * 960)));
							LongNote ln = new LongNote(id, starttime, start);
							start.setNote(n.x - 1, ln);
							start.setBPM(nowbpm);
							TimeLine end = model
									.getTimeLine(
											(n.y + n.l) / 960f,
											(int) ((1000.0 * 60 * 4 * (n.y + n.l)) / (nowbpm * 960)));
							ln.setEnd(end);
							end.setNote(n.x - 1, ln);
							end.setBPM(nowbpm);
						} else {
							TimeLine tl = model
									.getTimeLine(
											n.y / 960f,
											(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * 960)));
							tl.setNote(n.x - 1, new NormalNote(id));
							tl.setBPM(nowbpm);
						}
					}
				}
				id++;
			}
			model.setWavList(wavmap.toArray(new String[0]));
			Logger.getGlobal().info(
					"BMSONファイル解析完了 :" + f.getName() + " - TimeLine数:"
							+ model.getAllTimes().length);

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
		return model;
	}
}
