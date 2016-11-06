package bms.model;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Logger;

import sun.misc.IOUtils;

/**
 * BMSファイルをBMSModelにデコードするクラス
 * 
 * @author exch
 */
public class BMSDecoder {

	// TODO bug:RANDOM構文を厳密解釈する必要あり
	// TODO 構文解析エラーログの取得

	private final CommandWord[] reserve;

	private int lntype;

	private List<DecodeLog> log = new ArrayList<DecodeLog>();

	private int lnobj;

	private final int[] wm = new int[36 * 36];
	private final int[] bm = new int[36 * 36];

	private BMSGenerator generator;

	public BMSDecoder() {
		this(BMSModel.LNTYPE_LONGNOTE);
	}

	public BMSDecoder(int lntype) {
		this.lntype = lntype;
		// 予約語の登録
		List<CommandWord> reserve = new ArrayList<CommandWord>();
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
					Logger.getGlobal().warning(model.getTitle() + ":BMSファイルの解析中の例外:#RANK :" + arg);
				}
			}
		});
		reserve.add(new CommandWord("TOTAL") {
			public void execute(BMSModel model, String arg) {
				try {
					model.setTotal(Double.parseDouble(arg));
				} catch (NumberFormatException e) {
					log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#TOTALに数字が定義されていません"));
					Logger.getGlobal().warning(model.getTitle() + ":BMSファイルの解析中の例外:#TOTAL :" + arg);
				}
			}
		});
		reserve.add(new CommandWord("VOLWAV") {
			public void execute(BMSModel model, String arg) {
				try {
					model.setVolwav(Integer.parseInt(arg));
				} catch (NumberFormatException e) {
					log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#VOLWAVに数字が定義されていません"));
					Logger.getGlobal().warning(model.getTitle() + ":BMSファイルの解析中の例外:#VOLWAV :" + arg);
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
				lnobj = Integer.parseInt(arg.toUpperCase(), 36);
			}
		});
		reserve.add(new CommandWord("DIFFICULTY") {
			public void execute(BMSModel model, String arg) {
				try {
					model.setDifficulty(Integer.parseInt(arg));
				} catch (NumberFormatException e) {
					log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#DIFFICULTYに数字が定義されていません"));
					Logger.getGlobal().warning(model.getTitle() + ":BMSファイルの解析中の例外:#DIFFICULTY :" + arg);
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

		this.reserve = reserve.toArray(new CommandWord[reserve.size()]);
	}

	public BMSModel decode(File f) {
		return decode(f.toPath());
	}

	public BMSModel decode(Path f) {
		Logger.getGlobal().fine("BMSファイル解析開始 :" + f.toString());
		try {
			BMSModel model = this.decode(Files.readAllBytes(f), f.toString().toLowerCase().endsWith(".pms"), null);
			if (model == null) {
				return null;
			}
			model.setPath(f.toAbsolutePath().toString());
			Logger.getGlobal().fine("BMSファイル解析完了 :" + f.toString() + " - TimeLine数:" + model.getAllTimes().length);
			return model;
		} catch (IOException e) {
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
	public BMSModel decode(byte[] data, boolean ispms, int[] random) {
		log.clear();
		lnobj = -1;
		final long time = System.currentTimeMillis();
		BMSModel model = new BMSModel();
		Map<Integer, Double> stoptable = new TreeMap<Integer, Double>();
		Map<Integer, Double> bpmtable = new TreeMap<Integer, Double>();

		// BMS読み込み、ハッシュ値取得
		BufferedReader br = null;
		try {
			MessageDigest md5digest = MessageDigest.getInstance("MD5");
			MessageDigest sha256digest = MessageDigest.getInstance("SHA-256");
			if (ispms) {
				model.setUseKeys(9);
			}
			// Logger.getGlobal().info(
			// "BMSデータ読み込み時間(ms) :" + (System.currentTimeMillis() - time));

			br = new BufferedReader(new InputStreamReader(new DigestInputStream(new DigestInputStream(
					new ByteArrayInputStream(data), md5digest), sha256digest), "MS932"));
			String line = null;
			Map<Integer, String> wavmap = new HashMap<Integer, String>();
			Map<Integer, String> bgamap = new HashMap<Integer, String>();

			Map<Integer, List<String>> lines = new TreeMap<Integer, List<String>>();

			List<Integer> randoms = new ArrayList<Integer>();
			List<Integer> srandoms = new ArrayList<Integer>();
			int crandom = 0;
			int maxsec = 0;

			boolean skip = false;
			while ((line = br.readLine()) != null) {
				if (line.length() >= 2 && line.charAt(0) == '#') {
					line = line.substring(1, line.length());
					// RANDOM制御系
					if (matchesReserveWord(line, "RANDOM")) {
						try {
							final int r = Integer.parseInt(line.substring(7, line.length()));
							randoms.add(r);
							if (random != null) {
								crandom = random[randoms.size() - 1];
							} else {
								crandom = (int) (Math.random() * r) + 1;
								srandoms.add(crandom);
							}
						} catch (NumberFormatException e) {
							log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#RANDOMに数字が定義されていません"));
							Logger.getGlobal()
									.warning(model.getTitle() + ":BMSファイルの解析中の例外:#RANDOMに数字が定義されていません" + line);
						}
					}

					if (matchesReserveWord(line, "IF")) {
						// RANDOM分岐開始
						skip = (crandom != Integer.parseInt(line.substring(3, line.length())));
					} else if (matchesReserveWord(line, "ENDIF")) {
						skip = false;
					}

					if (!skip) {
						final char c = line.charAt(0);
						if ('0' <= c && c <= '9' && line.length() > 5) {
							// line = line.toUpperCase();
							// 楽譜
							try {
								final int bar_index = Integer.parseInt(line.substring(0, 3));
								List<String> l = lines.get(bar_index);
								if (l == null) {
									l = new ArrayList<String>();
									lines.put(bar_index, l);
								}
								l.add(line);
								maxsec = (maxsec > bar_index) ? maxsec : bar_index;
							} catch (NumberFormatException e) {
								log.add(new DecodeLog(DecodeLog.STATE_WARNING, "小節に数字が定義されていません : " + line));
								Logger.getGlobal().warning(model.getTitle() + ":BMSファイルの解析中の例外:" + line);
							}
						} else if (matchesReserveWord(line, "BPM")) {
							if (line.charAt(3) == ' ') {
								final String arg = line.substring(4, line.length());
								// BPMは小数点のケースがある(FREEDOM DiVE)
								try {
									model.setBpm(Double.parseDouble(arg));
								} catch (NumberFormatException e) {
									log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#BPMに数字が定義されていません : " + line));
									Logger.getGlobal().warning(model.getTitle() + ":BMSファイルの解析中の例外:#BPM :" + arg);
								}
							} else {
								String id = line.substring(3, 5);
								String bpm = line.substring(6, line.length());
								try {
									bpmtable.put(Integer.parseInt(id, 36), Double.parseDouble(bpm));
								} catch (NumberFormatException e) {
									log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#BPMxxに数字が定義されていません : " + line));
									Logger.getGlobal().warning(
											model.getTitle() + ":BMSファイルの解析中の例外:#BPM" + id + " :" + bpm);
								}
							}
						} else if (matchesReserveWord(line, "WAV")) {
							// 音源ファイル
							if (line.length() >= 7) {
								final String id = line.substring(3, 5);
								final String file_name = line.substring(6, line.length()).replace('\\', '/');
								try {
									wavmap.put(Integer.parseInt(id, 36), file_name);
								} catch (NumberFormatException e) {
									log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#WAVxxは不十分な定義です : " + line));
									Logger.getGlobal().warning(
											model.getTitle() + ":BMSファイルの解析中の例外:#WAVxxは不正な定義です : " + line);
								}
							} else {
								log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#WAVxxは不十分な定義です : " + line));
								Logger.getGlobal().warning(
										model.getTitle() + ":BMSファイルの解析中の例外:#WAVxxは不十分な定義です : " + line);
							}
						} else if (matchesReserveWord(line, "BMP")) {
							// BGAファイル
							if (line.length() >= 7) {
								final String id = line.substring(3, 5);
								final String file_name = line.substring(6, line.length()).replace('\\', '/');
								try {
									bgamap.put(Integer.parseInt(id, 36), file_name);
								} catch (NumberFormatException e) {
									log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#WAVxxは不十分な定義です : " + line));
									Logger.getGlobal().warning(
											model.getTitle() + ":BMSファイルの解析中の例外:#WAVxxは不正な定義です : " + line);
								}
							} else {
								log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#BMPxxは不十分な定義です : " + line));
								Logger.getGlobal().warning(
										model.getTitle() + ":BMSファイルの解析中の例外:#BMPxxは不十分な定義です : " + line);
							}
						} else if (matchesReserveWord(line, "STOP")) {
							if (line.length() >= 8) {
								String id = line.substring(4, 6);
								String stop = line.substring(7, line.length());
								try {
									stoptable.put(Integer.parseInt(id, 36), Double.parseDouble(stop) / 192);
								} catch (NumberFormatException e) {
									log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#STOPxxに数字が定義されていません : " + line));
									Logger.getGlobal().warning(
											model.getTitle() + ":BMSファイルの解析中の例外:#STOP" + id + " :" + stop);
								}
							} else {
								log.add(new DecodeLog(DecodeLog.STATE_WARNING, "#STOPxxは不十分な定義です : " + line));
								Logger.getGlobal().warning(
										model.getTitle() + ":BMSファイルの解析中の例外:#STOPxxは不十分な定義です : " + line);
							}
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
			}
			String[] wavlist = new String[wavmap.keySet().size()];
			Arrays.fill(wm, -2);
			int id = 0;
			for (int key : wavmap.keySet()) {
				wavlist[id] = wavmap.get(key);
				wm[key] = id;
				id++;
			}
			String[] bgalist = new String[bgamap.keySet().size()];
			Arrays.fill(bm, -2);
			id = 0;
			for (int key : bgamap.keySet()) {
				bgalist[id] = bgamap.get(key);
				bm[key] = id;
				id++;
			}
			model.setWavList(wavlist);
			model.setBgaList(bgalist);

			List<Section> sections = new ArrayList<Section>();
			Section prev = null;
			for (int i = 0; i <= maxsec; i++) {
				Section newsec = new Section(model, prev, lines.get(i) != null ? lines.get(i).toArray(
						new String[lines.get(i).size()]) : new String[0], bpmtable, stoptable);
				sections.add(newsec);
				prev = newsec;
			}
			// Logger.getGlobal().info(
			// "Section生成時間(ms) :" + (System.currentTimeMillis() - time));

			for (Section s : sections) {
				s.makeTimeLines(wm, bm, lnobj);
				log.addAll(s.getDecodeLog());
			}
			final int[] lastlnstatus = sections.get(sections.size() - 1).getEndLNStatus(sections.get(sections.size() - 1));
			for(int i = 0;i < 18;i++) {
				if(lastlnstatus[i] != 0) {
					log.add(new DecodeLog(DecodeLog.STATE_WARNING, "曲の終端までにLN終端定義されていないLNがあります。lane:" + (i + 1)));
					Logger.getGlobal().warning(
							model.getTitle() + ":曲の終端までにLN終端定義されていないLNがあります。lane:" + (i + 1));
					final TimeLine[] tl = model.getAllTimeLines();
					for(int index = tl.length - 1;index >= 0;index--) {
						final Note n = tl[index].getNote(i);
						if(n != null && n instanceof LongNote && ((LongNote)n).getEnd() == null) {
							tl[index].setNote(i, null);
							break;
						}
					}
				}
			}
			
			model.setLntype(lntype);
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
			model.setMD5(convertHexString(md5digest.digest()));
			model.setSHA256(convertHexString(sha256digest.digest()));
			Logger.getGlobal().fine("BMSデータ解析時間(ms) :" + (System.currentTimeMillis() - time));

			if (random == null) {
				random = new int[randoms.size()];
				for (int i = 0; i < random.length; i++) {
					random[i] = randoms.get(i);
				}
				generator = new BMSGenerator(data, ispms, random);
			}
			random = new int[srandoms.size()];
			for (int i = 0; i < random.length; i++) {
				random[i] = srandoms.get(i);
			}
			model.setRandom(random);
			return model;
		} catch (IOException e) {
			log.add(new DecodeLog(DecodeLog.STATE_ERROR, "BMSファイルへのアクセスに失敗しました"));
			Logger.getGlobal().severe(
					model.getTitle() + ":BMSファイル解析失敗: " + e.getClass().getName() + " - " + e.getMessage());
		} catch (Exception e) {
			log.add(new DecodeLog(DecodeLog.STATE_ERROR, "何らかの異常によりBMS解析に失敗しました"));
			Logger.getGlobal().severe(
					model.getTitle() + ":BMSファイル解析失敗: " + e.getClass().getName() + " - " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (br != null) {
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

	public BMSGenerator getBMSGenerator() {
		return generator;
	}

	/**
	 * バイトデータを16進数文字列表現に変換する
	 * 
	 * @param data
	 *            バイトデータ
	 * @returnバイトデータの16進数文字列表現
	 */
	public static String convertHexString(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (byte b : data) {
			sb.append(Character.forDigit(b >> 4 & 0xf, 16));
			sb.append(Character.forDigit(b & 0xf, 16));
		}
		return sb.toString();
	}

	public DecodeLog[] getDecodeLog() {
		return log.toArray(new DecodeLog[log.size()]);
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
