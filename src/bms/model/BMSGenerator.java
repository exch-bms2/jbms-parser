package bms.model;

public class BMSGenerator {

	private int[] random;
	
	private byte[] data;
	
	private boolean ispms;
	
	public BMSGenerator(byte[] data, boolean ispms, int[] random) {
		this.data = data;
		this.random = random;
		this.ispms = ispms;
	}
	
	public BMSModel generate(int[] random) {
		BMSDecoder decoder = new BMSDecoder();
		return decoder.decode(data, ispms, random);
	}
	
	public int[] getRandom() {
		return random;
	}
}
