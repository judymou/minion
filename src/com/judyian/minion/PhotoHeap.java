package com.judyian.minion;

import java.util.PriorityQueue;

public class PhotoHeap {
	private PriorityQueue<FlightRecord> heap;

	public PhotoHeap() {
		heap = new PriorityQueue<FlightRecord>(128,
				new FlightRecordComparator());
	}

	public void push(FlightRecord fr) {
		heap.add(fr);
	}

	public void push(long timestamp, double altitudeFt, String path) {
		push(new FlightRecord(timestamp, altitudeFt, path));
	}

	public FlightRecord pop() {
		return heap.remove();
	}
}
