package com.judyian.minion;

public class PhotoRecord {
	public long timestamp;
	public double altitudeFt;
	public String imagePath;

	public double lat;
	public double lng;

	public PhotoRecord(long timestamp, double altitudeFt, double lat,
			double lng, String imagePath) {
		this.timestamp = timestamp;
		this.altitudeFt = altitudeFt;
		this.lat = lat;
		this.lng = lng;
		this.imagePath = imagePath;
	}
}
