package bms.model;

import java.nio.file.Path;

public interface ChartDecoder {
	
	public abstract BMSModel decode(Path p);
	
	public abstract DecodeLog[] getDecodeLog();
	
	public static ChartDecoder getDecoder(Path p) {
		final String s = p.getFileName().toString().toLowerCase();
		if (s.endsWith(".bms") || s.endsWith(".bme") || s.endsWith(".bml") || s.endsWith(".pms")) {
			return new BMSDecoder(BMSModel.LNTYPE_LONGNOTE);
		} else if(s.endsWith(".bmson")) {
			return new BMSONDecoder(BMSModel.LNTYPE_LONGNOTE);			
		}
		return null;
	}

	public static int parseInt36(String s, int index) throws NumberFormatException {
		int result = parseInt36(s.charAt(index), s.charAt(index + 1));
		if(result == -1) {
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

}
