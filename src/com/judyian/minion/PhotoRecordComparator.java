package com.judyian.minion;

import java.util.Comparator;

public class PhotoRecordComparator implements Comparator<PhotoRecord> {

	@Override
	public int compare(PhotoRecord f1, PhotoRecord f2) {
		// Prefer highest altitude.
		// TODO maybe bucket by 10k ft or something
		int altitudeCompare = Double.valueOf(f1.altitudeFt).compareTo(
				Double.valueOf(f2.altitudeFt));
		if (altitudeCompare != 0) {
			return altitudeCompare;
		}

		// Return earlier one first.
		return Long.valueOf(-f1.timestamp).compareTo(
				Long.valueOf(-f2.timestamp));
	}
}