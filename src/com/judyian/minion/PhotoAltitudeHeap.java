package com.judyian.minion;

import java.util.PriorityQueue;

public class PhotoAltitudeHeap {
	private PriorityQueue<FlightRecord> heap;

	public PhotoAltitudeHeap() {
		heap = new PriorityQueue<FlightRecord>(128,
				new FlightRecordComparator());
	}

	public void push(FlightRecord fr) {
		heap.add(fr);
	}

	public FlightRecord pop() {
		return heap.remove();
	}
}
