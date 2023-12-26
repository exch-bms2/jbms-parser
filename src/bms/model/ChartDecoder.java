package bms.model;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 譜面デコーダー
 * 
 * @author exch
 */
public abstract class ChartDecoder {

	int lntype;

	List<DecodeLog> log = new ArrayList<DecodeLog>();

	/**
	 * パスで指定したファイルをBMSModelに変換する
	 * 
	 * @param file
	 *            譜面ファイル
	 * @return 変換したBMSModel。失敗した場合はnull
	 */
	public BMSModel decode(File file) {
		return decode(file.toPath());
	}

	/**
	 * パスで指定したファイルをBMSModelに変換する
	 * 
	 * @param path
	 *            譜面ファイルのパス
	 * @return 変換したBMSModel。失敗した場合はnull
	 */
	public BMSModel decode(Path path) {
		return decode(new ChartInformation(path, lntype, null));		
	}
	
	/**
	 * デコードログを取得する
	 * 
	 * @return デコードログ
	 */
	public DecodeLog[] getDecodeLog() {
		return log.toArray(new DecodeLog[log.size()]);
	}
	
	public abstract BMSModel decode(ChartInformation info);

	/**
	 * パスで指定したファイルに対応するChartDecoderを取得する
	 * 
	 * @param p
	 *            譜面ファイルのパス
	 * @return 対応するChartDecoder。存在しない場合はnull
	 */
	public static ChartDecoder getDecoder(Path p) {
		final String s = p.getFileName().toString().toLowerCase();
		if (s.endsWith(".bms") || s.endsWith(".bme") || s.endsWith(".bml") || s.endsWith(".pms")) {
			return new BMSDecoder(BMSModel.LNTYPE_LONGNOTE);
		} else if (s.endsWith(".bmson")) {
			return new BMSONDecoder(BMSModel.LNTYPE_LONGNOTE);
		}
		return null;
	}
	
	public static int parseInt36(String s, int index) throws NumberFormatException {
		int result = parseInt36(s.charAt(index), s.charAt(index + 1));
		if (result == -1) {
			throw new NumberFormatException();
		}
		return result;
	}

	public static int parseInt36(char c1, char c2) {
		int result = 0;
		if (c1 >= '0' && c1 <= '9') {
			result = (c1 - '0') * 36;
		} else if (c1 >= 'a' && c1 <= 'z') {
			result = ((c1 - 'a') + 10) * 36;
		} else if (c1 >= 'A' && c1 <= 'Z') {
			result = ((c1 - 'A') + 10) * 36;
		} else {
			return -1;
		}

		if (c2 >= '0' && c2 <= '9') {
			result += (c2 - '0');
		} else if (c2 >= 'a' && c2 <= 'z') {
			result += (c2 - 'a') + 10;
		} else if (c2 >= 'A' && c2 <= 'Z') {
			result += (c2 - 'A') + 10;
		} else {
			return -1;
		}

		return result;
	}

	public static int parseInt62(String s, int index) throws NumberFormatException {
		int result = parseInt62(s.charAt(index), s.charAt(index + 1));
		if (result == -1) {
			throw new NumberFormatException();
		}
		return result;
	}

	public static int parseInt62(char c1, char c2) {
		int result = 0;
		if (c1 >= '0' && c1 <= '9') {
			result = (c1 - '0') * 62;
		} else if (c1 >= 'A' && c1 <= 'Z') {
			result = ((c1 - 'A') + 10) * 62;
		} else if (c1 >= 'a' && c1 <= 'z') {
			result = ((c1 - 'a') + 36) * 62;
		} else {
			return -1;
		}

		if (c2 >= '0' && c2 <= '9') {
			result += (c2 - '0');
		} else if (c2 >= 'A' && c2 <= 'Z') {
			result += (c2 - 'A') + 10;
		} else if (c2 >= 'a' && c2 <= 'z') {
			result += (c2 - 'a') + 36;
		} else {
			return -1;
		}

		return result;
	}

	public static String toBase62(int decimal) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0;i < 2;i++) {
			int mod = (int)(decimal % 62);
			if (mod < 10) {
				sb.append(mod);
			} else if (mod < 36) {
				mod = mod - 10 + 'A';
				sb.append((char) mod);
			} else if (mod < 62) {
				mod = mod - 36 + 'a';
				sb.append((char) mod);
			} else {
				sb.append("0");
			}
			decimal = (int)(decimal / 62);
		}
		return new String(sb.reverse());
	}
	
	protected void printLog(Path path) {
		log.forEach(log -> {
			switch(log.getState()) {
			case INFO:
				Logger.getGlobal().info(path + " : " + log.getMessage());
				break;
			case WARNING:
				Logger.getGlobal().warning(path + " : " + log.getMessage());
				break;
			case ERROR:
				Logger.getGlobal().severe(path + " : " + log.getMessage());
				break;
			}
		});
	}

	public static class TimeLineCache {
		
		public final double time;
		public final TimeLine timeline;
		
		public TimeLineCache(double time, TimeLine timeline) {
			this.time = time;
			this.timeline = timeline;
		}
	}
}
