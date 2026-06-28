package bms.model;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import static bms.model.DecodeLog.State.*;
import bms.model.TimeLineTreeMap.TimeLineEntry;

/**
 * BMSファイルをBMSModelにデコードするクラス
 * 
 * @author exch
 */
public class BMSDecoder extends ChartDecoder {

	final List<String> wavlist = new ArrayList<String>(62 * 62);
	private final int[] wm = new int[62 * 62];

	final List<String> bgalist = new ArrayList<String>(62 * 62);
	private final int[] bm = new int[62 * 62];

	public BMSDecoder() {
		this(BMSModel.LNTYPE_LONGNOTE);
	}

	public BMSDecoder(int lntype) {
		this.lntype = lntype;
		// 予約語の登録
	}

	public BMSModel decode(Path f) {
		Logger.getGlobal().fine("BMSファイル解析開始 :" + f.toString());
		try {
			BMSModel model = this.decode(f, Files.readAllBytes(f), f.toString().toLowerCase().endsWith(".pms"), null);
			if (model == null) {
				return null;
			}
			Logger.getGlobal().fine("BMSファイル解析完了 :" + f.toString() + " - TimeLine数:" + model.getAllTimes().length);
			return model;
		} catch (IOException e) {
			log.add(new DecodeLog(ERROR, "BMSファイルが見つかりません"));
			Logger.getGlobal().severe("BMSファイル解析中の例外 : " + e.getClass().getName() + " - " + e.getMessage());
		}
		return null;
	}
	
	public BMSModel decode(ChartInformation info) {
		try {
			this.lntype = info.lntype;
			return decode(info.path, Files.readAllBytes(info.path), info.path.toString().toLowerCase().endsWith(".pms"), info.selectedRandoms);
		} catch (IOException e) {
			log.add(new DecodeLog(ERROR, "BMSファイルが見つかりません"));
			Logger.getGlobal().severe("BMSファイル解析中の例外 : " + e.getClass().getName() + " - " + e.getMessage());
		}
		return null;
	}


	private final List<String>[] lines = new List[1000];

	private final IntDoubleTreeMap scrolltable = new IntDoubleTreeMap();
	private final IntDoubleTreeMap stoptable = new IntDoubleTreeMap();
	private final IntDoubleTreeMap bpmtable = new IntDoubleTreeMap();
	private final IntDoubleTreeMap speedtable = new IntDoubleTreeMap();
	private final Deque<Integer> randoms = new ArrayDeque<Integer>();
	private final Deque<Integer> srandoms = new ArrayDeque<Integer>();
	private final Deque<Integer> crandom = new ArrayDeque<Integer>();
	private final Deque<Boolean> skip = new ArrayDeque<Boolean>();
	
	private static final CommandWord[] commandWords = CommandWord.values();

	/**
	 * 指定したBMSファイルをモデルにデコードする
	 *
	 * @param data
	 * @return
	 */
	public BMSModel decode(byte[] data, boolean ispms, int[] random) {
		return this.decode(null, data, ispms, random);
	}
	
	/**
	 * 指定したBMSファイルをモデルにデコードする
	 *
	 * @param data
	 * @return
	 */
	private BMSModel decode(Path path, byte[] data, boolean ispms, int[] selectedRandom) {
		log.clear();
		final long time = System.currentTimeMillis();
		BMSModel model = new BMSModel();
		scrolltable.clear();
		stoptable.clear();
		bpmtable.clear();

		MessageDigest md5digest, sha256digest;
		try {
			md5digest = MessageDigest.getInstance("MD5");
			sha256digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
			return null;
		}

		String encoding = "MS932";
		// Detect the Encoding
		try {
			encoding = detectEncoding(data, Math.min(data.length, 1024 * 64));
		} catch (Exception e) {
			log.add(new DecodeLog(ERROR, "何らかの異常によりBMS解析に失敗しました"));
			Logger.getGlobal()
					.severe(path + ":BMSファイル解析失敗: " + e.getClass().getName() + " - " + e.getMessage());
			e.printStackTrace();
			return null;
		}
		
		// ファイルの最後まで読んで先に #BASE を確定することが必須
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data),encoding))) {
			String line = null;
			
			while ((line = br.readLine()) != null) {
				if (line.length() < 2) {
					continue;
				}
				if(line.charAt(0) == '#') {
					if (matchesReserveWord(line, "BASE")) {
						if (line.charAt(5) == ' ') {
							try {
								final String arg = line.substring(6).trim();
								int base = Integer.parseInt(arg);
								if(base != 62 && base != 36 ) {
									model.setBase(36);
									log.add(new DecodeLog(WARNING, "#BASEに無効な数字が定義されています"));
								} else {
									model.setBase(base);
								}
							} catch (NumberFormatException e) {
								model.setBase(36);
								log.add(new DecodeLog(WARNING, "#BASEに数字が定義されていません"));
							}
						}
					}
				}
			}	
		} catch (IOException e) {
			model.setBase(36);
			log.add(new DecodeLog(ERROR, "BMSファイルへのアクセスに失敗しました"));
			Logger.getGlobal()
					.severe(path + ":BMSファイル解析失敗: " + e.getClass().getName() + " - " + e.getMessage());
			e.printStackTrace();
		}
		
		int maxsec = 0;
		// BMS読み込み、ハッシュ値取得
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				new DigestInputStream(new DigestInputStream(new ByteArrayInputStream(data), md5digest), sha256digest),
				encoding));) {
			model.setMode(ispms ? Mode.POPN_9K : Mode.BEAT_5K);
			// Logger.getGlobal().info(
			// "BMSデータ読み込み時間(ms) :" + (System.currentTimeMillis() - time));

			String line = null;
			wavlist.clear();
			Arrays.fill(wm, -2);
			bgalist.clear();
			Arrays.fill(bm, -2);
			for (List<String> l : lines) {
				if (l != null) {
					l.clear();
				}
			}

			randoms.clear();
			srandoms.clear();
			crandom.clear();

			skip.clear();
			while ((line = br.readLine()) != null) {
				if (line.length() < 2) {
					continue;
				}

				if(line.charAt(0) == '#') {
					// line = line.substring(1, line.length());
					// RANDOM制御系
					if (matchesReserveWord(line, "RANDOM")) {
						try {
							final int r = Integer.parseInt(line.substring(8).trim());
							randoms.add(r);
							if (selectedRandom != null) {
								crandom.add(selectedRandom[randoms.size() - 1]);
							} else {
								crandom.add((int) (Math.random() * r) + 1);
								srandoms.add(crandom.getLast());
							}
						} catch (NumberFormatException e) {
							log.add(new DecodeLog(WARNING, "#RANDOMに数字が定義されていません"));
						}
					} else if (matchesReserveWord(line, "IF")) {
						// RANDOM分岐開始
						if(!crandom.isEmpty()) {
							try {
								skip.add((crandom.getLast() != Integer.parseInt(line.substring(4).trim())));
							} catch (NumberFormatException e) {
								log.add(new DecodeLog(WARNING, "#IFに数字が定義されていません"));
							}
						} else {
							log.add(new DecodeLog(WARNING, "#IFに対応する#RANDOMが定義されていません"));
						}
					} else if (matchesReserveWord(line, "ENDIF")) {
						if (!skip.isEmpty()) {
							skip.removeLast();
						} else {
							log.add(new DecodeLog(WARNING, "ENDIFに対応するIFが存在しません: " + line));
						}
					} else if (matchesReserveWord(line, "ENDRANDOM")) {
						if (!crandom.isEmpty()) {
							crandom.removeLast();
						} else {
							log.add(new DecodeLog(WARNING, "ENDRANDOMに対応するRANDOMが存在しません: " + line));
						}
					} else if (skip.isEmpty() || !skip.getLast()) {
						final char c = line.charAt(1);
						final int base = model.getBase();
						if ('0' <= c && c <= '9' && line.length() > 6) {
							// line = line.toUpperCase();
							// 楽譜
							final char c2 = line.charAt(2);
							final char c3 = line.charAt(3);
							if ('0' <= c2 && c2 <= '9' && '0' <= c3 && c3 <= '9') {
								final int bar_index = (c - '0') * 100 + (c2 - '0') * 10 + (c3 - '0');
								List<String> l = lines[bar_index];
								if (l == null) {
									l = lines[bar_index] = new ArrayList<String>();
								}
								l.add(line);
								maxsec = (maxsec > bar_index) ? maxsec : bar_index;
							} else {
								log.add(new DecodeLog(WARNING, "小節に数字が定義されていません : " + line));
							}
						} else if (matchesReserveWord(line, "BPM")) {
							if (line.charAt(4) == ' ') {
								// BPMは小数点のケースがある(FREEDOM DiVE)
								try {
									final String arg = line.substring(5).trim();
									double bpm = Double.parseDouble(arg);
									if(bpm > 0) {
										model.setBpm(bpm);
									} else {
										log.add(new DecodeLog(WARNING, "#negative BPMはサポートされていません : " + line));
									}
								} catch (NumberFormatException e) {
									log.add(new DecodeLog(WARNING, "#BPMに数字が定義されていません : " + line));
								}
							} else {
								try {
									double bpm = Double.parseDouble(line.substring(7).trim());
									if(bpm > 0) {
										if(base == 62) {
											bpmtable.put(ChartDecoder.parseInt62(line, 4), bpm);
										} else {
											bpmtable.put(ChartDecoder.parseInt36(line, 4), bpm);
										}
									} else {
										log.add(new DecodeLog(WARNING, "#negative BPMはサポートされていません : " + line));
									}
								} catch (NumberFormatException e) {
									log.add(new DecodeLog(WARNING, "#BPMxxに数字が定義されていません : " + line));
								}
							}
						} else if (matchesReserveWord(line, "WAV")) {
							// 音源ファイル
							if (line.length() >= 8) {
								try {
									final String file_name = line.substring(7).trim().replace('\\', '/');
									if(base == 62) {
										wm[ChartDecoder.parseInt62(line, 4)] = wavlist.size();
									} else {
										wm[ChartDecoder.parseInt36(line, 4)] = wavlist.size();
									}
									wavlist.add(file_name);
								} catch (NumberFormatException e) {
									log.add(new DecodeLog(WARNING, "#WAVxxは不十分な定義です : " + line));
								}
							} else {
								log.add(new DecodeLog(WARNING, "#WAVxxは不十分な定義です : " + line));
							}
						} else if (matchesReserveWord(line, "BMP")) {
							// BGAファイル
							if (line.length() >= 8) {
								try {
									final String file_name = line.substring(7).trim().replace('\\', '/');
									if(base == 62) {
										bm[ChartDecoder.parseInt62(line, 4)] = bgalist.size();
									} else {
										bm[ChartDecoder.parseInt36(line, 4)] = bgalist.size();
									}
									bgalist.add(file_name);
								} catch (NumberFormatException e) {
									log.add(new DecodeLog(WARNING, "#BMPxxは不十分な定義です : " + line));
								}
							} else {
								log.add(new DecodeLog(WARNING, "#BMPxxは不十分な定義です : " + line));
							}
						} else if (matchesReserveWord(line, "STOP")) {
							if (line.length() >= 9) {
								try {
									double stop = Double.parseDouble(line.substring(8).trim()) / 192;
									if(stop < 0) {
										stop = Math.abs(stop);
										log.add(new DecodeLog(WARNING, "#negative STOPはサポートされていません : " + line));
									}
									if(base == 62) {
										stoptable.put(ChartDecoder.parseInt62(line, 5), stop);
									} else {
										stoptable.put(ChartDecoder.parseInt36(line, 5), stop);
									}
								} catch (NumberFormatException e) {
									log.add(new DecodeLog(WARNING, "#STOPxxに数字が定義されていません : " + line));
								}
							} else {
								log.add(new DecodeLog(WARNING, "#STOPxxは不十分な定義です : " + line));
							}
						} else if (matchesReserveWord(line, "SCROLL")) {
							if (line.length() >= 11) {
								try {
									double scroll = Double.parseDouble(line.substring(10).trim());
									if(base == 62) {
										scrolltable.put(ChartDecoder.parseInt62(line, 7), scroll);
									} else {
										scrolltable.put(ChartDecoder.parseInt36(line, 7), scroll);
									}
								} catch (NumberFormatException e) {
									log.add(new DecodeLog(WARNING, "#SCROLLxxに数字が定義されていません : " + line));
								}
							} else {
								log.add(new DecodeLog(WARNING, "#SCROLLxxは不十分な定義です : " + line));
							}
						} else {
							for (CommandWord cw : commandWords) {
								if (line.length() > cw.name().length() + 2 && matchesReserveWord(line, cw.name())) {
									DecodeLog log = cw.function.apply(model, line.substring(cw.name().length() + 2).trim());
									if (log != null) {
										this.log.add(log);
										Logger.getGlobal().warning(model.getTitle() + " - " + log.getMessage() + " : " + line);
									}
									break;
								}
							}
						}
					}
				} else if(line.charAt(0) == '%') {
					final int index = line.indexOf(' ');
					if(index > 0 && line.length() > index + 1) {
						model.getValues().put(line.substring(1, index), line.substring(index + 1));
					}
				} else if(line.charAt(0) == '@') {
					final int index = line.indexOf(' ');
					if(index > 0 && line.length() > index + 1) {
						model.getValues().put(line.substring(1, index), line.substring(index + 1));
					}
				}
			}
			
			model.setWavList(wavlist.toArray(new String[wavlist.size()]));
			model.setBgaList(bgalist.toArray(new String[bgalist.size()]));

			Section prev = null;
			Section[] sections = new Section[maxsec + 1];
			for (int i = 0; i <= maxsec; i++) {
				sections[i] = new Section(model, prev, lines[i] != null ? lines[i] : Collections.EMPTY_LIST, bpmtable,
						stoptable, scrolltable, speedtable, log);
				prev = sections[i];
			}

			final TimeLineTreeMap timelines = new TimeLineTreeMap();
			final List<LongNote>[] lnlist = new List[model.getMode().key];
			LongNote[] lnendstatus = new LongNote[model.getMode().key];
			TimeLine[] lastNoteTimeLine = new TimeLine[model.getMode().key];
			final TimeLine basetl = new TimeLine(0, 0, model.getMode().key);
			basetl.setBPM(model.getBpm());
			timelines.put(0.0, 0.0, basetl);
			for (Section section : sections) {
				section.makeTimeLines(wm, bm, timelines, lnlist, lnendstatus, lastNoteTimeLine);
			}
			// Logger.getGlobal().info(
			// "Section生成時間(ms) :" + (System.currentTimeMillis() - time));
			TimeLine[] tl = new TimeLine[timelines.size()];
			int tlcount = 0;
			for(int i = 0; i < timelines.size(); i++) {
				TimeLineEntry tlc = timelines.valueAt(i);
				tl[tlcount] = tlc.timeline;
				tlcount++;
			}
			model.setAllTimeLine(tl);

			if(tl[0].getBPM() == 0) {
				log.add(new DecodeLog(ERROR, "開始BPMが定義されていないため、BMS解析に失敗しました"));
				Logger.getGlobal().severe(path + ":BMSファイル解析失敗: 開始BPMが定義されていません");
				return null;
			}

			for (int i = 0; i < lnendstatus.length; i++) {
				if (lnendstatus[i] != null) {
					log.add(new DecodeLog(WARNING, "曲の終端までにLN終端定義されていないLNがあります。lane:" + (i + 1)));
					if(lnendstatus[i].getSection() != Double.MIN_VALUE) {
						TimeLineEntry tlc = timelines.get(lnendstatus[i].getSection());
						if(tlc != null) {
							tlc.timeline.setNote(i,  null);
						}
					}
				}
			}

			if (model.getTotalType() != BMSModel.TotalType.BMS) {
				log.add(new DecodeLog(WARNING, "TOTALが未定義です"));
			}
			if (model.getTotal() <= 60.0) {
				log.add(new DecodeLog(WARNING, "TOTAL値が少なすぎます"));
			}
			if (tl.length > 0) {
				if (tl[tl.length - 1].getTime() >= model.getLastTime() + 30000) {
					log.add(new DecodeLog(WARNING, "最後のノート定義から30秒以上の余白があります"));
				}
			}
			if (model.getPlayer() > 1 && (model.getMode() == Mode.BEAT_5K || model.getMode() == Mode.BEAT_7K)) {
				log.add(new DecodeLog(WARNING, "#PLAYER定義が2以上にもかかわらず2P側のノーツ定義が一切ありません"));
			}
			if (model.getPlayer() == 1 && (model.getMode() == Mode.BEAT_10K || model.getMode() == Mode.BEAT_14K)) {
				log.add(new DecodeLog(WARNING, "#PLAYER定義が1にもかかわらず2P側のノーツ定義が存在します"));
			}
			model.setMD5(convertHexString(md5digest.digest()));
			model.setSHA256(convertHexString(sha256digest.digest()));
			log.add(new DecodeLog(INFO, "#PLAYER定義が1にもかかわらず2P側のノーツ定義が存在します"));
			Logger.getGlobal().fine("BMSデータ解析時間(ms) :" + (System.currentTimeMillis() - time));

			if (selectedRandom == null) {
				selectedRandom = new int[srandoms.size()];
				final Iterator<Integer> ri = srandoms.iterator();
				for (int i = 0; i < selectedRandom.length; i++) {
					selectedRandom[i] = ri.next();
				}
			}
			
			model.setChartInformation(new ChartInformation(path, lntype, selectedRandom));
			printLog(path);
			return model;
		} catch (IOException e) {
			log.add(new DecodeLog(ERROR, "BMSファイルへのアクセスに失敗しました"));
			Logger.getGlobal()
					.severe(path + ":BMSファイル解析失敗: " + e.getClass().getName() + " - " + e.getMessage());
		} catch (Exception e) {
			log.add(new DecodeLog(ERROR, "何らかの異常によりBMS解析に失敗しました"));
			Logger.getGlobal()
					.severe(path + ":BMSファイル解析失敗: " + e.getClass().getName() + " - " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	private boolean matchesReserveWord(String line, String s) {
		final int len = s.length();
		if (line.length() <= len) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			final char c = line.charAt(i + 1);
			final char c2 = s.charAt(i);
			if (c != c2 && c != c2 + 32) {
				return false;
			}
		}
		return true;
	}

	/**
	 * バイトデータを16進数文字列表現に変換する
	 * 
	 * @param data
	 *            バイトデータ
	 * @returnバイトデータの16進数文字列表現
	 */
	public static String convertHexString(byte[] data) {
		final StringBuilder sb = new StringBuilder(data.length * 2);
		for (byte b : data) {
			sb.append(Character.forDigit(b >> 4 & 0xf, 16));
			sb.append(Character.forDigit(b & 0xf, 16));
		}
		return sb.toString();
	}
	
    /**
     * バイトオーダーマーク (BOM) に基づいてエンコーディングを推測します。
     * 
     * @param bytes チェックするバイト配列
     * @return BOMに対応する文字コードを示す文字列。BOMが見つからない場合は"MS932"。
     */
    public static String encodingFromBOM(byte[] bytes) {

        // BOM定義
        final byte[] bomUTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        final byte[] bomUTF16LE = {(byte) 0xFF, (byte) 0xFE};
        final byte[] bomUTF16BE = {(byte) 0xFE, (byte) 0xFF};
        final byte[] bomUTF32LE = {(byte) 0xFF, (byte) 0xFE, (byte) 0x00, (byte) 0x00};
        final byte[] bomUTF32BE = {(byte) 0x00, (byte) 0x00, (byte) 0xFE, (byte) 0xFF};

        // 4バイトチェック
        if (bytes.length >= 4) {
            byte[] prefix4 = Arrays.copyOf(bytes, 4);

            if (Arrays.equals(prefix4, bomUTF32BE)) {
                return "UTF-32BE";
            } else if (Arrays.equals(prefix4, bomUTF32LE)) {
                return "UTF-32LE";
            }
        }

        // 3バイトチェック
        if (bytes.length >= 3) {
            byte[] prefix3 = Arrays.copyOf(bytes, 3);
            
            if (Arrays.equals(prefix3, bomUTF8)) {
                return "UTF-8";
            }
        }

        // 2バイトチェック
        if (bytes.length >= 2) {
            byte[] prefix2 = Arrays.copyOf(bytes, 2);

            if (Arrays.equals(prefix2, bomUTF16BE)) {
                return "UTF-16BE";
            } else if (Arrays.equals(prefix2, bomUTF16LE)) {
                return "UTF-16LE";
            }
        }

        // BOMが見つからない場合、"MS932"を返す。
        return "MS932";
    }

    /**
     * 文字化けを調べて、Charsetを推測する。BOMがないことが前提条件。
     * @param bytes Charsetを調べるByte列。
     * @return 登録されているCharsetごとにByte列と文字列のラウンドトリップ変換して、
     * 一致するなら、そのCharsetの名称。
     * 最後まで見つからなかったら、MS932を返す。
     */
    public String encodingFromGarbled(byte[] bytes) {
    	return encodingFromGarbled(bytes, bytes.length);
    }

    public String encodingFromGarbled(byte[] bytes, int length) {
        List<Charset> encodingsToTry = new ArrayList<>();
        encodingsToTry.add(Charset.forName("EUC-KR")); // 韓国語環境のBMSEが出力するため2010年以前にしばしば見られた
        encodingsToTry.add(Charset.forName("MS932")); // SHIFT-JIS
        encodingsToTry.add(Charset.forName("UTF-8")); // UTF-8
        encodingsToTry.add(Charset.forName("UTF-16BE")); // UTF-16BE
        encodingsToTry.add(Charset.forName("UTF-16LE")); // UTF-16LE
        encodingsToTry.add(Charset.forName("UTF-32BE")); // UTF-32BE
        encodingsToTry.add(Charset.forName("UTF-32LE")); // UTF-32LE
        // ここに同じように追加すれば対応Charsetをいくらでも増やせるけど、
        // 他の本体が対応できないためUTFのみにした方がよさそう。
        
        for (Charset encoding : encodingsToTry) {
            try {
                // バイト列を文字列にデコードする
                String decodedString = new String(bytes, 0, length, encoding);

                if (!(decodedString.contains("\r\n") || decodedString.contains("\r") || decodedString.contains("\n") || decodedString.contains("#"))) {
                	// BMSファイル特有のチェック条件: 改行コードと"#"がないことはありえない
                	continue; 
                }

                byte[] newBytes = decodedString.getBytes(encoding);
                int correctCount = 0;
                int compareLength = Math.min(length, newBytes.length);

                // Byte列を比較して一致する連続個数をカウント
               for (int i = 0; i < compareLength; i++) {
                    if (bytes[i] == newBytes[i]) {
                    	correctCount++;
                    } else {
                    	break;
                    }
                }
                
                // 連続で閾値以上一致するなら、正しいエンコーディングと推測する。
                // 末端に4バイト未満の欠落や文字化けが発生しうることを考慮
                if (correctCount > length - 4) {
                    return encoding.name();
                }

            } catch (Exception e) {
                // その他のエラー
                continue;
            }
        }

        // すべて試行して見つからなかった場合、デフォルトとしてMS932を返す
        return "MS932";
    }
    
    public String detectEncoding(byte[] bytes) {
    	return detectEncoding(bytes, bytes.length);
    }
    
    public String detectEncoding(byte[] bytes, int length) {
    	String encoding = encodingFromBOM(bytes) ;
    	
    	// BOMが存在しない場合
    	if (encoding == "MS932" ){
    		encoding = encodingFromGarbled(bytes, length);
    	}
    	
    	return encoding;
    }
}

/**
 * 予約語
 *
 * @author exch
 */
enum CommandWord {

	PLAYER ((model, arg) -> {
		try {
			final int player = Integer.parseInt(arg);
			// TODO playerの許容幅は？
			if (player >= 1 && player < 3) {
				model.setPlayer(player);
			} else {
				return new DecodeLog(WARNING, "#PLAYERに規定外の数字が定義されています : " + player);
			}
		} catch (NumberFormatException e) {
			return new DecodeLog(WARNING, "#PLAYERに数字が定義されていません");
		}
		return null;
	}),
	GENRE ((model, arg) -> {
		model.setGenre(arg);
		return null;
	}),
	TITLE ((model, arg) -> {
		model.setTitle(arg);
		return null;
	}),
	SUBTITLE ((model, arg) -> {
		model.setSubTitle(arg);
		return null;
	}),
	ARTIST ((model, arg) -> {
		model.setArtist(arg);
		return null;
	}),
	SUBARTIST ((model, arg) -> {
		model.setSubArtist(arg);
		return null;
	}),
	PLAYLEVEL ((model, arg) -> {
		model.setPlaylevel(arg);
		return null;
	}),
	RANK ((model, arg) -> {
		try {
			final int rank = Integer.parseInt(arg);
			if (rank >= 0 && rank < 5) {
				model.setJudgerank(rank);
				model.setJudgerankType(BMSModel.JudgeRankType.BMS_RANK);
			} else {
				return new DecodeLog(WARNING, "#RANKに規定外の数字が定義されています : " + rank);
			}
		} catch (NumberFormatException e) {
			return new DecodeLog(WARNING, "#RANKに数字が定義されていません");
		}
		return null;
	}),
	DEFEXRANK ((model, arg) -> {
		try {
			final int rank = Integer.parseInt(arg);
			if (rank >= 1) {
				model.setJudgerank(rank);
				model.setJudgerankType(BMSModel.JudgeRankType.BMS_DEFEXRANK);
			} else {
				return new DecodeLog(WARNING, "#DEFEXRANK 1以下はサポートしていません" + rank);
			}
		} catch (NumberFormatException e) {
			return new DecodeLog(WARNING, "#DEFEXRANKに数字が定義されていません");
		}
		return null;
	}),
	TOTAL ((model, arg) -> {
		try {
			final double total = Double.parseDouble(arg);
			if(total > 0) {
				model.setTotal(total);
				model.setTotalType(BMSModel.TotalType.BMS);
			} else {
				return new DecodeLog(WARNING, "#TOTALが0以下です");
			}
		} catch (NumberFormatException e) {
			return new DecodeLog(WARNING, "#TOTALに数字が定義されていません");
		}
		return null;
	}),
	VOLWAV ((model, arg) -> {
		try {
			model.setVolwav(Integer.parseInt(arg));
		} catch (NumberFormatException e) {
			return new DecodeLog(WARNING, "#VOLWAVに数字が定義されていません");
		}
		return null;
	}),
	STAGEFILE ((model, arg) -> {
		model.setStagefile(arg.replace('\\', '/'));
		return null;
	}),
	BACKBMP ((model, arg) -> {
		model.setBackbmp(arg.replace('\\', '/'));
		return null;
	}),
	PREVIEW ((model, arg) -> {
		model.setPreview(arg.replace('\\', '/'));
		return null;
	}),
	LNOBJ ((model, arg) -> {
		try {
			if (model.getBase() == 62) {
				model.setLnobj(ChartDecoder.parseInt62(arg, 0));
			} else {
				model.setLnobj(Integer.parseInt(arg.toUpperCase(), 36));
			}
		} catch (NumberFormatException e) {
			return new DecodeLog(WARNING, "#LNOBJに数字が定義されていません");
		}
		return null;
	}),
	LNMODE ((model, arg) -> {
		try {
			int lnmode = Integer.parseInt(arg);
			if(lnmode < 0 || lnmode > 3) {
				return new DecodeLog(WARNING, "#LNMODEに無効な数字が定義されています");
			}
			model.setLnmode(lnmode);
		} catch (NumberFormatException e) {
			return new DecodeLog(WARNING, "#LNMODEに数字が定義されていません");
		}
		return null;
	}),
	DIFFICULTY ((model, arg) -> {
		try {
			model.setDifficulty(Integer.parseInt(arg));
		} catch (NumberFormatException e) {
			return new DecodeLog(WARNING, "#DIFFICULTYに数字が定義されていません");
		}
		return null;
	}),
	BANNER ((model, arg) -> {
		model.setBanner(arg.replace('\\', '/'));
		return null;
	}),
	COMMENT ((model, arg) -> {
		// TODO 未実装
		return null;
	});

	public final BiFunction<BMSModel, String, DecodeLog> function;

	private CommandWord(BiFunction<BMSModel, String, DecodeLog> function) {
		this.function = function;
	}	
}

/**
 * 予約語
 *
 * @author exch
 */
enum OptionWord {

	URL ((model, arg) -> {
		// TODO 未実装
		return null;
	});

	public final BiFunction<BMSModel, String, DecodeLog> function;

	private OptionWord(BiFunction<BMSModel, String, DecodeLog> function) {
		this.function = function;
	}
}
