package bms.model;

import java.nio.file.Path;

public class ChartInformation {

	final byte[] data;
	
	public final Path path;
	
	public final int[] allRandoms;
	
	public final int[] selectedRandoms;
	
	public ChartInformation(byte[] data, Path path, int[] allRandoms, int[] selectedRandoms) {
		this.data = data;
		this.path = path;
		this.allRandoms = allRandoms;
		this.selectedRandoms = selectedRandoms;
	}

}
