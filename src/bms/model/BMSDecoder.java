package bms.model;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

/**
 * BMSファイルをBMSModelにデコードするクラス
 * 
 * @author exch
 */
public class BMSDecoder {

	// TODO bug:RANDOM構文を厳密解釈する必要あり
	// TODO 構文解析エラーログの取得

	private List<CommandWord> reserve = new ArrayList<CommandWord>();

	private int lntype;

	private List<DecodeLog> log = new ArrayList<DecodeLog>();

	public BMSDecoder() {
		this(BMSModel.LNTYPE_LONGNOTE);
	}

	public BMSDecoder(int lntype) {
		this.lntype = lntype;
		// 予約語の登録
		reserve.add(new CommandWord("PLAYER") {
			public void execute(BMSModel model, String arg) {
				try {
					model.setPlayer(Integer.parseInt(arg));
				} catch (NumberFormatException e) {
					log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#PLAYERに数字が定義されていません"));
					Logger.getGlobal().warning("BMSファイルの解析中の例外:#PLAYER :" + arg);
				}
			}
		});
		reserve.add(new CommandWord("GENRE") {
			public void execute(BMSModel model, String arg) {
				model.setGenre(arg);
			}
		});
		reserve.add(new CommandWord("TITLE") {
			public void execute(BMSModel model, String arg) {
				model.setTitle(arg);
			}
		});
		reserve.add(new CommandWord("SUBTITLE") {
			public void execute(BMSModel model, String arg) {
				model.setSubTitle(arg);
			}
		});
		reserve.add(new CommandWord("ARTIST") {
			public void execute(BMSModel model, String arg) {
				model.setArtist(arg);
			}
		});
		reserve.add(new CommandWord("SUBARTIST") {
			public void execute(BMSModel model, String arg) {
				model.setSubArtist(arg);
			}
		});
		reserve.add(new CommandWord("PLAYLEVEL") {
			public void execute(BMSModel model, String arg) {
				model.setPlaylevel(arg);
			}
		});
		reserve.add(new CommandWord("RANK") {
			public void execute(BMSModel model, String arg) {
				try {
					model.setJudgerank(Integer.parseInt(arg));
				} catch (NumberFormatException e) {
					log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#RANKに数字が定義されていません"));
					Logger.getGlobal().warning("BMSファイルの解析中の例外:#RANK :" + arg);
				}
			}
		});
		reserve.add(new CommandWord("TOTAL") {
			public void execute(BMSModel model, String arg) {
				try {
					model.setTotal(Double.parseDouble(arg));
				} catch (NumberFormatException e) {
					log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#TOTALに数字が定義されていません"));
					Logger.getGlobal().warning("BMSファイルの解析中の例外:#TOTAL :" + arg);
				}
			}
		});
		reserve.add(new CommandWord("VOLWAV") {
			public void execute(BMSModel model, String arg) {
				try {
					model.setVolwav(Integer.parseInt(arg));
				} catch (NumberFormatException e) {
					log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#VOLWAVに数字が定義されていません"));
					Logger.getGlobal().warning("BMSファイルの解析中の例外:#VOLWAV :" + arg);
				}
			}
		});
		reserve.add(new CommandWord("STAGEFILE") {
			public void execute(BMSModel model, String arg) {
				model.setStagefile(arg);
			}
		});
		reserve.add(new CommandWord("BACKBMP") {
			public void execute(BMSModel model, String arg) {
				model.setBackbmp(arg);
			}
		});
		reserve.add(new CommandWord("LNOBJ") {
			public void execute(BMSModel model, String arg) {
				model.setLNObject(arg.toUpperCase());
			}
		});
		reserve.add(new CommandWord("DIFFICULTY") {
			public void execute(BMSModel model, String arg) {
				try {
					model.setDifficulty(Integer.parseInt(arg));
				} catch (NumberFormatException e) {
					log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#DIFFICULTYに数字が定義されていません"));
					Logger.getGlobal().warning("BMSファイルの解析中の例外:#DIFFICULTY :" + arg);
				}
			}
		});
		reserve.add(new CommandWord("BACKBMP") {
			public void execute(BMSModel model, String arg) {
				model.setBackbmp(arg);
			}
		});
		reserve.add(new CommandWord("BANNER") {
			public void execute(BMSModel model, String arg) {
				model.setBanner(arg);
			}
		});
		reserve.add(new CommandWord("RANDOM") {
			public void execute(BMSModel model, String arg) {
				try {
					model.setRandom(Integer.parseInt(arg));
				} catch (NumberFormatException e) {
					log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#RANDOMに数字が定義されていません"));
					Logger.getGlobal().warning("BMSファイルの解析中の例外:#RANDOM :" + arg);
				}
			}
		});
	}

	public BMSModel decode(File f) {
		log.clear();
		Logger.getGlobal().info("BMSファイル解析開始 :" + f.getName());
		try {
			BMSModel model = this.decode(new FileInputStream(f), f.getName().toLowerCase().endsWith(".pms"));
			if (model == null) {
				return null;
			}
			model.setPath(f.getAbsolutePath());
			Logger.getGlobal().info("BMSファイル解析完了 :" + f.getName() + " - TimeLine数:" + model.getAllTimes().length);
			return model;
		} catch (FileNotFoundException e) {
			log.add(new DecodeLog(DecodeLog.STATE_ERROR, "BMSファイルが見つかりません"));
			Logger.getGlobal().severe("BMSファイル解析中の例外 : " + e.getClass().getName() + " - " + e.getMessage());
		}
		return null;
	}

	/**
	 * 指定したBMSファイルをモデルにデコードする
	 * 
	 * @param f
	 * @return
	 */
	public BMSModel decode(InputStream is, boolean ispms) {
		log.clear();
		long time = System.currentTimeMillis();
		byte[] data = null;
		BMSModel model = new BMSModel();

		// BMS読み込み、ハッシュ値取得
		try {
			MessageDigest md5digest = MessageDigest.getInstance("MD5");
			MessageDigest sha256digest = MessageDigest.getInstance("SHA-256");
			data = IOUtils.toByteArray(new DigestInputStream(new DigestInputStream(is, md5digest), sha256digest));
			model.setMD5(convertHexString(md5digest.digest()));
			model.setSHA256(convertHexString(sha256digest.digest()));			
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if(data == null) {
			return null;
		}
		
		ByteArrayInputStream bias = new ByteArrayInputStream(data);
		BufferedReader br = null;
		try {
			if (ispms) {
				model.setUseKeys(9);
			}
			// Logger.getGlobal().info(
			// "BMSデータ読み込み時間(ms) :" + (System.currentTimeMillis() - time));
			br = new BufferedReader(new InputStreamReader(bias, "MS932"));
			String line = null;
			Map<String, String> wavmap = new HashMap<String, String>();
			Map<String, String> bgamap = new HashMap<String, String>();

			Map<Integer, Map<Integer, List<String>>> lines = new HashMap<Integer, Map<Integer, List<String>>>();
			int random = 0;
			int maxsec = 0;
			lines.put(0, new HashMap<Integer, List<String>>());
			lines.put(1, new HashMap<Integer, List<String>>());
			while ((line = br.readLine()) != null) {
				if (line.length() >= 1 && line.charAt(0) == '#') {
					line = line.substring(1, line.length());
					char c = line.charAt(0);
					if ('0' <= c && c <= '9') {
						line = line.toUpperCase();
						// 楽譜
						try {
							int bar_index = Integer.parseInt(line.substring(0, 3));
							List<String> l = lines.get(random).get(bar_index);
							if (l == null) {
								l = new ArrayList<String>();
								lines.get(random).put(bar_index, l);
							}
							l.add(line);
							maxsec = (maxsec > bar_index) ? maxsec : bar_index;
						} catch (NumberFormatException e) {
							log.add(new DecodeLog(DecodeLog.STATE_WARNING, "小節に数字が定義されていません : " + line));
							Logger.getGlobal().warning("BMSファイルの解析中の例外:" + line);
						}
					} else if (matchesReserveWord(line, "BPM")) {
						if (line.charAt(3) == ' ') {
							String arg = line.substring(4, line.length());
							// BPMは小数点のケースがある(FREEDOM DiVE)
							try {
								model.setBpm(Double.parseDouble(arg));
							} catch (NumberFormatException e) {
								log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#BPMに数字が定義されていません : " + line));
								Logger.getGlobal().warning("BMSファイルの解析中の例外:#BPM :" + arg);
							}
						} else {
							String id = line.substring(3, 5).toUpperCase();
							String bpm = line.substring(6, line.length());
							try {
								model.putBPM(id, Double.parseDouble(bpm));
							} catch (NumberFormatException e) {
								log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#BPMxxに数字が定義されていません : " + line));
								Logger.getGlobal().warning("BMSファイルの解析中の例外:#BPM" + id + " :" + bpm);
							}
						}
					} else if (matchesReserveWord(line, "WAV")) {
						// 音源ファイル
						String id = line.substring(3, 5).toUpperCase();
						String file_name = line.substring(6, line.length());
						wavmap.put(id, file_name);
					} else if (matchesReserveWord(line, "BMP")) {
						// BGAファイル
						String id = line.substring(3, 5).toUpperCase();
						String file_name = line.substring(6, line.length());
						bgamap.put(id, file_name);
					} else if (matchesReserveWord(line, "STOP")) {
						String id = line.substring(4, 6).toUpperCase();
						String stop = line.substring(7, line.length());
						try {
							model.putStop(id, Double.parseDouble(stop) / 192);
						} catch (NumberFormatException e) {
							log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#STOPxxに数字が定義されていません : " + line));
							Logger.getGlobal().warning("BMSファイルの解析中の例外:#STOP" + id + " :" + stop);
						}
					} else if (matchesReserveWord(line, "IF ")) {
						// RANDOM分岐開始
						random = Integer.parseInt(line.substring(3, line.length()));
						if (lines.get(random) == null) {
							lines.put(random, new HashMap<Integer, List<String>>());
						}
					} else if (matchesReserveWord(line, "ENDIF")) {
						// RANDOM分岐終了
						random = 0;
					} else {
						for (CommandWord cw : reserve) {
							if (line.toUpperCase().matches(cw.str + "\\s.+")) {
								String arg = line.substring(cw.str.length() + 1);
								while (arg.length() > 0 && arg.charAt(arg.length() - 1) == ' ') {
									arg = arg.substring(0, arg.length() - 1);
								}
								cw.execute(model, arg);
								break;
							}
						}
					}
				}
			}
			String[] wavlist = new String[wavmap.keySet().size()];
			Map<String, Integer> wm = new HashMap<String, Integer>();
			int id = 0;
			for (String key : wavmap.keySet()) {
				wavlist[id] = wavmap.get(key);
				wm.put(key, id);
				id++;
			}
			String[] bgalist = new String[bgamap.keySet().size()];
			Map<String, Integer> bm = new HashMap<String, Integer>();
			id = 0;
			for (String key : bgamap.keySet()) {
				bgalist[id] = bgamap.get(key);
				bm.put(key, id);
				id++;
			}
			model.setWavList(wavlist);
			model.setBgaList(bgalist);

			for (int rand = 1; rand <= model.getRandom(); rand++) {
				model.setSelectedIndexOfTimeLines(rand);
				List<Section> sections = new ArrayList<Section>();
				Section prev = null;
				for (int i = 0; i <= maxsec; i++) {
					List<String> ln = new ArrayList<String>();
					List<String> commonln = lines.get(0).get(i);
					if (commonln != null) {
						ln.addAll(commonln);
					}

					List<String> randomln = lines.get(rand).get(i);
					if (randomln != null) {
						ln.addAll(randomln);
					}
					Section newsec = new Section(model, prev, ln.toArray(new String[0]));
					sections.add(newsec);
					prev = newsec;
				}
				// Logger.getGlobal().info(
				// "Section生成時間(ms) :" + (System.currentTimeMillis() - time));

				for (Section s : sections) {
					s.makeTimeLines(wm, bm);
					log.addAll(s.getDecodeLog());
				}
			}
			model.setSelectedIndexOfTimeLines(1);
			model.setLntype(lntype);
			Logger.getGlobal().info("BMSデータ解析時間(ms) :" + (System.currentTimeMillis() - time));
			if (model.getTotal() <= 60.0) {
				log.add(new DecodeLog(DecodeLog.STATE_WARNING, "TOTALが未定義か、値が少なすぎます"));
			}
			if (model.getAllTimeLines().length > 0) {
				int[] times = model.getAllTimes();
				if (times[times.length - 1] >= model.getLastTime() + 30000) {
					log.add(new DecodeLog(DecodeLog.STATE_WARNING, "最後のノート定義から30秒以上の余白があります"));
				}
			}
			br.close();
			return model;
		} catch (IOException e) {
			log.add(new DecodeLog(DecodeLog.STATE_ERROR, "BMSファイルへのアクセスに失敗しました"));
			Logger.getGlobal().severe("BMSファイル解析失敗: " + e.getClass().getName() + " - " + e.getMessage());
		} catch (Exception e) {
			log.add(new DecodeLog(DecodeLog.STATE_ERROR, "何らかの異常によりBMS解析に失敗しました"));
			Logger.getGlobal().severe("BMSファイル解析失敗: " + e.getClass().getName() + " - " + e.getMessage());
			e.printStackTrace();
		} finally {
			if(br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private boolean matchesReserveWord(String line, String s) {
		return line.length() >= s.length() && line.substring(0, s.length()).toUpperCase().compareTo(s) == 0;
	}

	/**
	 * バイトデータを16進数文字列表現に変換する
	 * 
	 * @param data
	 *            バイトデータ
	 * @returnバイトデータの16進数文字列表現
	 */
	public static String convertHexString(byte[] data) {
		StringBuffer sb = new StringBuffer();
		for (byte b : data) {
			sb.append(Character.forDigit(b >> 4 & 0xf, 16));
			sb.append(Character.forDigit(b & 0xf, 16));
		}
		return sb.toString();
	}

	public DecodeLog[] getDecodeLog() {
		return log.toArray(new DecodeLog[0]);
	}
}

/**
 * 予約語
 * 
 * @author exch
 */
abstract class CommandWord {

	public final String str;

	public CommandWord(String s) {
		str = s;
	}

	public abstract void execute(BMSModel model, String arg);

}
