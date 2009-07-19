package com.solacesystems.testtool.jnetpipe.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import com.solacesystems.testtool.jnetpipe.core.impl.PipeStatsImpl;
import com.solacesystems.testtool.jnetpipe.stat.PipeStats;

public class PipeInstance implements SocketConnector {
	public static final Logger trace = Logger.getLogger(PipeInstance.class);
	private static final AtomicInteger counter = new AtomicInteger(0);

	SocketChannel _localChannel, _remoteChannel;
	ChannelController _localCtrler, _remoteCtrler;
	volatile PipeState _pipeState;
	final IoContext _ioContext;
	final InetSocketAddress _remoteSocketAddress;
	final String _name;
	final int _id;
	final PipeStatsImpl _stats;
	
	public PipeInstance(
		SocketChannel localChannel,
		InetAddress remoteAddr,
		int remotePort,
		IoContext ctx) {
		_ioContext = ctx;
		_localChannel = localChannel;
		_id = counter.getAndIncrement();
		_stats = new PipeStatsImpl();
		_name = String.format("PipeInstance-%s", _id);
		_remoteSocketAddress = new InetSocketAddress(remoteAddr, remotePort);
		try {
			_localCtrler = new ChannelController(
				_localChannel, 
				this, 
				String.format("localChannel-%s", _id));
			startConnect();
		} catch (IOException ex) {
			trace.error("PipeInstance creation error", ex);
			setState(PipeState.DOWN);
		}
		trace.info(String.format("Created %s", _name));
	}

	private void startConnect() throws IOException {
		setState(PipeState.CONNECTING);
		_remoteChannel = SocketChannel.open();
		_remoteChannel.configureBlocking(false);
		_remoteCtrler = new ChannelController(_remoteChannel, this, String.format("remoteChannel-%s", _id));
		boolean connectresult = _remoteChannel.connect(_remoteSocketAddress);
		if (!connectresult) {
			_ioContext.regFinishConnect(this, true);
		} else {
			setState(PipeState.UP);
		}
	}

	public SocketChannel channel() {
		return _remoteChannel;
	}

	public void handleIoException(IOException ioe) {
		trace.error("IOException in PipeInstance", ioe);
		ioe.printStackTrace();
		setState(PipeState.DOWN);
	}

	public void finishConnect() {
		trace.info("FinishConnect callback.");
		try {
			_remoteChannel.finishConnect();
			_ioContext.regFinishConnect(this, false);
			setState(PipeState.UP);
		} catch (IOException ex) {
			setState(PipeState.DOWN);
		}
	}

	private void setState(PipeState newstate) {
		trace.info(String.format("State Transition (%s): %s -> %s\n", this, _pipeState, newstate));
		// TODO: refactor this: cleaner state design!!!

		List<ChannelController> channels = Arrays.asList(_localCtrler, _remoteCtrler);
		switch (newstate) {
		case UP:
			for(ChannelController cc : channels) {
				if (cc != null) {
					cc.registerRead(true);
				}
			}
			break;
		case DOWN:
			for(ChannelController cc : channels) {
				if (cc != null) {
					cc.registerRead(false);
					cc.close();
				}
			}
			break;
		}
		_pipeState = newstate;
	}

	public IoContext getIoContext() {
		return _ioContext;
	}

	public PipeStats getStats() {
		return _stats;
	}
	
	/**
	 * callback when incoming data is ready
	 */
	public void incomingData(ByteBuffer buf, ChannelController source) {
		// A pipe is bringing in data, we echo it to the output (the other pipe)
		
		source.registerRead(false);
		if (source == _localCtrler) {
			_stats.incrBytesOut(buf.remaining());
		} else if (source == _remoteCtrler) {
			_stats.incrBytesIn(buf.remaining());
		}
		
		ChannelController writer = (source == _localCtrler) ? _remoteCtrler : _localCtrler;
		writer.queueWrite(buf);
	}

	/** callback when a queued write completes */
	public void writeComplete(ChannelController source) {
		ChannelController reader = (source == _localCtrler) ? _remoteCtrler : _localCtrler;
		reader.registerRead(true);
	}
	
	@Override
	public String toString() {
		return String.format("%s (%s)", _name, _pipeState);
	}

}
