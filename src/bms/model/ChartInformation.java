package bms.model;

import java.nio.file.Path;

public class ChartInformation {

	public final Path path;
	
	public final int lntype;
	
	public final int[] selectedRandoms;
	
	public ChartInformation(Path path, int lntype, int[] selectedRandoms) {
		this.path = path;
		this.lntype = lntype;
		this.selectedRandoms = selectedRandoms;
	}

}
