package com.solacesystems.testtool.jnetpipe.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * A PipeController owns a serversocket and decides whether or not to accept
 * connections and create pipes.
 * 
 * It maintains a registry of PipeInstances.
 * 
 * @author jdaigle
 * 
 */
public class PipeController implements SocketConnectAcceptor {
	public static final Logger trace = Logger.getLogger(PipeController.class);

	ServerSocketChannel _ssc;
	InetAddress _remoteAddress;
	int _remotePort, _localPort;
	IoContext _ioContext;

	List<SocketChannel> _channels;
	List<PipeInstance> _pipes;
	PipeControllerState _state;

	public PipeController(
		final int localPort,
		final InetAddress remoteAddr,
		final int remotePort,
		final IoContext ctx) throws IOException {
		_localPort = localPort;
		_ssc = ServerSocketChannel.open();
		_ssc.configureBlocking(false);
		ServerSocket socket = _ssc.socket();
		socket.bind(new InetSocketAddress(localPort));
		_remoteAddress = remoteAddr;
		_remotePort = remotePort;
		_ioContext = ctx;
		_channels = new ArrayList<SocketChannel>();
		_pipes = new ArrayList<PipeInstance>();
		setState(PipeControllerState.UP);
	}

	public void acceptConnection() throws IOException {
		trace.info("acceptConnection()");
		SocketChannel sc = _ssc.accept();
		if (sc != null) {
			PipeInstance pi = new PipeInstance(sc, _remoteAddress, _remotePort, _ioContext);
			_pipes.add(pi);
		}
	}

	public AbstractSelectableChannel channel() {
		return _ssc;
	}

	@Override
	public void handleIoException(IOException ex) {
		trace.warn("IoException", ex);
	}

	public void start() {
		_ioContext.regServerAccept(this, true);
	}

	public void stop() {
		_ioContext.regServerAccept(this, false);
	}

	@Override
	public String toString() {
		return String.format("%s:%s:%s", _localPort, _remoteAddress, _remotePort);
	}

	private void setState(PipeControllerState newstate) {
		trace.info(String.format("State Transition (%s): %s -> %s\n", this, _state, newstate));
		_state = newstate;
	}
}