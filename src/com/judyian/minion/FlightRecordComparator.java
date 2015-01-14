package com.judyian.minion;

import java.util.Comparator;

public class FlightRecordComparator implements Comparator<FlightRecord> {

	@Override
	public int compare(FlightRecord f1, FlightRecord f2) {
		// Prefer highest altitude.
		int altitudeCompare = Integer.valueOf(f1.altitudeFt).compareTo(
				Integer.valueOf(f2.altitudeFt));
		if (altitudeCompare != 0) {
			return altitudeCompare;
		}

		// Return earlier one first.
		return Integer.valueOf(-f1.timestamp).compareTo(
				Integer.valueOf(-f2.timestamp));
	}
}