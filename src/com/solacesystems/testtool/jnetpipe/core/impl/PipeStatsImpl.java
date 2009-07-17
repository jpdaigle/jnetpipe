package com.solacesystems.testtool.jnetpipe.core.impl;

import com.solacesystems.testtool.jnetpipe.stat.PipeStats;

public class PipeStatsImpl extends PipeStats {
	public PipeStatsImpl() {
	}

	public void reset() {
		_bytesIn.set(0);
		_bytesOut.set(0);
	}

	public void incrBytesIn(long v) {
		_bytesIn.addAndGet(v);
	}

	public void incrBytesOut(long v) {
		_bytesOut.addAndGet(v);
	}
}
