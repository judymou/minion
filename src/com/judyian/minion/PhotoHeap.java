package com.judyian.minion;

import java.util.PriorityQueue;

public class PhotoHeap {
	private PriorityQueue<PhotoRecord> heap;

	public PhotoHeap() {
		heap = new PriorityQueue<PhotoRecord>(128, new PhotoRecordComparator());
	}

	public void push(PhotoRecord fr) {
		heap.add(fr);
	}

	public void push(long timestamp, double altitudeFt, double lat, double lng,
			String path) {
		push(new PhotoRecord(timestamp, altitudeFt, lat, lng, path));
	}

	public PhotoRecord pop() {
		return heap.remove();
	}
}
