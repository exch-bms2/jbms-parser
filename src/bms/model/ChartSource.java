package bms.model;

import java.io.IOException;
import java.io.InputStream;

public interface ChartSource {

	public String location();

	public InputStream openStream() throws IOException;
}
