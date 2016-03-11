package bms.model;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
		BMSModel model = new BMSModel();
		try {
			ObjectMapper mapper = new ObjectMapper();
			Bmson bmson = mapper.readValue(f, Bmson.class);
			model.setTitle(bmson.info.title);
			model.setArtist(bmson.info.artist);
			model.setGenre(bmson.info.genre);
			model.setJudgerank(bmson.info.judgeRank);
			model.setTotal(bmson.info.total);
			model.setBpm(bmson.info.initBPM);
			model.setPlaylevel(String.valueOf(bmson.info.level));
			model.setUseKeys(7);
			model.setLntype(lntype);
			double nowbpm = model.getBpm();
			// TODO BPM変化に伴うTime算出
			// bpmNotes処理
			for (EventNote n : bmson.bpmNotes) {
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
			for (EventNote n : bmson.stopNotes) {
				TimeLine tl = model.getTimeLine(n.y / 960f,
						(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * 960)));
				tl.setStop((int) ((1000.0 * 60 * 4 * n.v) / (nowbpm * 960)));
				tl.setBPM(nowbpm);
			}

			List<String> wavmap = new ArrayList<String>();
			int id = 0;
			for (SoundChannel sc : bmson.soundChannel) {
				wavmap.add(sc.name);
				for (bms.model.bmson.Note n : sc.notes) {
					if (n.x == 0) {
						TimeLine tl = model
								.getTimeLine(
										n.y / 960f,
										(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * 960)));
						tl.addBackGroundNote(new NormalNote(id));
						tl.setBPM(nowbpm);
					} else {
						if (n.l > 0) {
							// ロングノート
							TimeLine start = model
									.getTimeLine(
											n.y / 960f,
											(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * 960)));
							LongNote ln = new LongNote(id, start);
							start.addNote(n.x - 1, ln);
							start.setBPM(nowbpm);
							TimeLine end = model
									.getTimeLine(
											n.y / 960f,
											(int) ((1000.0 * 60 * 4 * (n.y + n.l)) / (nowbpm * 960)));
							ln.setEnd(end);
							end.addNote(n.x - 1, ln);
							end.setBPM(nowbpm);
						} else {
							TimeLine tl = model
									.getTimeLine(
											n.y / 960f,
											(int) ((1000.0 * 60 * 4 * n.y) / (nowbpm * 960)));
							tl.addNote(n.x - 1, new NormalNote(id));
							tl.setBPM(nowbpm);
						}
					}
				}
				id++;
			}
			model.setWavList(wavmap.toArray(new String[0]));

		} catch (JsonParseException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		return model;
	}
}
