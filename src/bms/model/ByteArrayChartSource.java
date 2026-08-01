package bms.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ByteArrayChartSource implements ChartSource {

	private final String location;
	private final byte[] data;

	public ByteArrayChartSource(String location, byte[] data) {
		this.location = location;
		this.data = data;
	}

	public String location() {
		return location;
	}

	public InputStream openStream() throws IOException {
		return new ByteArrayInputStream(data);
	}
}
