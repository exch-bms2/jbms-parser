package bms.model;

import java.nio.file.Path;

public class ChartInformation {

	private final byte[] data;
	
	public final Path path;
	
	private final int[] random;
	
	public ChartInformation(byte[] data, Path path, int[] random) {
		this.data = data;
		this.path = path;
		this.random = random;
	}

}
