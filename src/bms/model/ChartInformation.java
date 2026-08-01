package bms.model;

import java.nio.file.Path;

public class ChartInformation {

	public final Path path;

	public final ChartSource source;
	
	public final int lntype;
	
	public final int[] selectedRandoms;
	
	public ChartInformation(Path path, int lntype, int[] selectedRandoms) {
		this(new PathChartSource(path), lntype, selectedRandoms);
	}

	public ChartInformation(ChartSource source, int lntype, int[] selectedRandoms) {
		this.source = source;
		this.path = source instanceof PathChartSource ? ((PathChartSource) source).path() : null;
		this.lntype = lntype;
		this.selectedRandoms = selectedRandoms;
	}

}
