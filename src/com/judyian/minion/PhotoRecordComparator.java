package com.judyian.minion;

import java.util.Comparator;

public class PhotoRecordComparator implements Comparator<PhotoRecord> {
	// Time from start, in minutes, where we think the best pictures will be.
	private static final int SWEET_SPOT_ELAPSED_MIN = 60;
	private static final int SWEET_SPOT_ELAPSED_MS = SWEET_SPOT_ELAPSED_MIN * 60 * 1000;

	@Override
	public int compare(PhotoRecord f1, PhotoRecord f2) {
		long f1Elapsed = f1.timestamp - Tracker.START_TIME;
		long f2Elapsed = f2.timestamp - Tracker.START_TIME;

		return Long.valueOf(-Math.abs(f1Elapsed - SWEET_SPOT_ELAPSED_MS))
				.compareTo(
						Long.valueOf(-Math.abs(f2Elapsed
								- SWEET_SPOT_ELAPSED_MS)));
	}
}