package com.judyian.minion;

public class FlightRecord {
	public long timestamp;
	public double altitudeFt;
	public String imagePath;
	
	public FlightRecord(long timestamp, double altitudeFt, String imagePath) {
		this.timestamp = timestamp;
		this.altitudeFt = altitudeFt;
		this.imagePath = imagePath;
	}
}
