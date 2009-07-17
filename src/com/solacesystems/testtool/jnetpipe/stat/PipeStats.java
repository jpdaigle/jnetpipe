package com.solacesystems.testtool.jnetpipe.stat;

import java.util.concurrent.atomic.AtomicLong;

public class PipeStats {

	protected final AtomicLong _bytesIn = new AtomicLong(0), _bytesOut = new AtomicLong(0);

	protected PipeStats() {
	}

	public long getBytesIn() {
		return _bytesIn.get();
	}

	public long getBytesOut() {
		return _bytesOut.get();
	}
}
