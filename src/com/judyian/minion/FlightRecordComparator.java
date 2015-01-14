package com.judyian.minion;

import java.util.Comparator;

public class FlightRecordComparator implements Comparator<FlightRecord> {

	@Override
	public int compare(FlightRecord f1, FlightRecord f2) {
		// Prefer highest altitude.
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