package com.solacesystems.testtool.jnetpipe.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.apache.log4j.Logger;

public class PipeInstance implements SocketConnector {
	public static final Logger trace = Logger.getLogger(PipeInstance.class);

	SocketChannel _localChannel, _remoteChannel;
	ChannelController _localCtrler, _remoteCtrler;
	volatile PipeState _pipeState;
	final IoContext _ioContext;
	final InetSocketAddress _remoteSocketAddress;

	public PipeInstance(
		SocketChannel localChannel,
		InetAddress remoteAddr,
		int remotePort,
		IoContext ctx) {
		_ioContext = ctx;
		_localChannel = localChannel;
		_remoteSocketAddress = new InetSocketAddress(remoteAddr, remotePort);
		try {
			_localCtrler = new ChannelController(_localChannel, this, "local");
			startConnect();
		} catch (IOException ex) {
			trace.error("PipeInstance creation error", ex);
			setState(PipeState.DOWN);
		}
	}

	private void startConnect() throws IOException {
		setState(PipeState.CONNECTING);
		_remoteChannel = SocketChannel.open();
		_remoteChannel.configureBlocking(false);
		_remoteCtrler = new ChannelController(_remoteChannel, this, "remote");
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

		switch (newstate) {
		case UP:
			if (_localCtrler != null) {
				_localCtrler.registerRead(true);
			}
			if (_remoteCtrler != null) {
				_remoteCtrler.registerRead(true);
			}
			break;
		case DOWN:
			if (_localCtrler != null) {
				_localCtrler.registerRead(false);
				_localCtrler.close();
			}
			if (_remoteCtrler != null) {
				_remoteCtrler.registerRead(false);
				_remoteCtrler.close();
			}
			break;
		}
		_pipeState = newstate;
	}

	public IoContext getIoContext() {
		return _ioContext;
	}

	// callback when incoming data is ready
	public void incomingData(ByteBuffer buf, ChannelController source) {
		// A pipe is bringing in data, we echo it to the output
		ChannelController writer = (source == _localCtrler) ? _remoteCtrler : _localCtrler;
		source.registerRead(false);
		writer.queueWrite(buf);
	}

	// callback when a queued write completes
	public void writeComplete(ChannelController source) {
		ChannelController reader = (source == _localCtrler) ? _remoteCtrler : _localCtrler;
		reader.registerRead(true);
	}

}
