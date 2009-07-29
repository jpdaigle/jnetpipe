package com.solacesystems.testtool.jnetpipe.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.apache.log4j.Logger;

public class ChannelController implements SocketWriter {
	public final static int FL_DROP_READS = 1;
	public final static int FL_DROP_WRITES = 2;
	
	
	final static int BUF_SZ = 32768;
	final static Logger trace = Logger.getLogger(ChannelController.class);
	final SocketChannel _ch;
	final PipeInstance _parentPipe;
	final String _name;
	
	volatile int behaviour = 0;
	private ByteBuffer _nextWrite = null;

	public ChannelController(SocketChannel sc, PipeInstance parentPipe, String name) throws IOException {
		_ch = sc;
		_ch.socket().setTcpNoDelay(true);
		_parentPipe = parentPipe;
		_ch.configureBlocking(false);
		_name = name;
	}

	public void registerRead(boolean reg, int flags) {
		trace.debug(String.format("registerRead[%s], %s", this, reg));
		_parentPipe.getIoContext().regRW(SelectionKey.OP_READ, reg, this, flags);
	}

	public void registerWrite(boolean reg, int flags) {
		trace.debug(String.format("registerWrite[%s], %s", this, reg));
		_parentPipe.getIoContext().regRW(SelectionKey.OP_WRITE, reg, this, flags);
	}

	public int getBehaviourFlags() {
		return behaviour;
	}

	public void setBehaviourFlags(int flags) {
		String strdbg = "Setting behaviour flags ";
		strdbg += getBehaviourString();
		trace.info(strdbg);
		
		behaviour = flags;
	}
	
	public void close() {
		try {
			_ch.close();
		} catch (IOException e) {
			trace.debug("IOException in channel close.", e);
		}
	}

	/**
	 * Called when ready to read from channel
	 */
	@Override
	public void read() {
		ByteBuffer buf = ByteBuffer.allocate(BUF_SZ);
		try {
			int bytesRead = _ch.read(buf);
			if (bytesRead == -1) {
				// closed
				handleIoException(new IOException("Read EOF"));
			} else if (bytesRead > 0) {
				buf.flip();
				if ((behaviour & FL_DROP_READS) == 0)
					_parentPipe.incomingData(buf, this);
			}
		} catch (IOException ex) {
			handleIoException(ex);
		}
	}

	public void queueWrite(ByteBuffer buf) {
		assert (_nextWrite == null);
		_nextWrite = buf;
		registerWrite(true, 0);
	}

	/**
	 * Called when ready to write
	 */
	@Override
	public void write() {
		final int bytesToWrite = _nextWrite.remaining();
		try {
			// Drop all writes and report complete
			if ((behaviour & FL_DROP_WRITES) != 0) {
				registerWrite(false, 0); // write done!
				_nextWrite = null;
				_parentPipe.writeComplete(this);
				return;
			}
			
			final int bytesWritten = _ch.write(_nextWrite);
			if (bytesWritten == -1) {
				handleIoException(new IOException("EOF on write"));
			} else if (bytesWritten < bytesToWrite) {
				registerWrite(true, 0); // have more data
			} else if (bytesWritten == bytesToWrite) {
				registerWrite(false, 0); // write done!
				_nextWrite = null;
				_parentPipe.writeComplete(this);
			}
		} catch (IOException ex) {
			handleIoException(ex);
		}
	}

	@Override
	public void handleIoException(IOException ex) {
		_parentPipe.handleIoException(ex);
	}

	@Override
	public SocketChannel channel() {
		return _ch;
	}

	private String getBehaviourString() {
		int flags = getBehaviourFlags();
		String strdbg = String.valueOf(flags) + " ";
		if ((flags & FL_DROP_READS) != 0)
			strdbg += "FL_DROP_READS ";
		if ((flags & FL_DROP_WRITES) != 0)
			strdbg += "FL_DROP_WRITES ";
		if (flags == 0)
			strdbg += "0";
		return strdbg;
	}
	
	@Override
	public String toString() {
		return String.format("%s (B:%s)", _name, getBehaviourString());
	}
	
}
