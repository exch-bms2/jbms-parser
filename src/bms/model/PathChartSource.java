package bms.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PathChartSource implements ChartSource {

	private final Path path;

	public PathChartSource(Path path) {
		this.path = path;
	}

	public Path path() {
		return path;
	}

	public String location() {
		return path.toString();
	}

	public InputStream openStream() throws IOException {
		return Files.newInputStream(path);
	}
}
