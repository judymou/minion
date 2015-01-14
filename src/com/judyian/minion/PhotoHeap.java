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

	public FlightRecord pop() {
		return heap.remove();
	}
}
