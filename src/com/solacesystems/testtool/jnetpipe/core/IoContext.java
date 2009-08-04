package com.solacesystems.testtool.jnetpipe.core;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.log4j.Logger;

public class IoContext {
	// Flags for SelectionKey operations
	public static final int FL_IGNOREEXCEPTIONS = 1;

	private static final Logger trace = Logger.getLogger(IoContext.class);
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	
	private Thread _worker;
	private Queue<Runnable> _regOps;
	private Selector _selector;

	public IoContext() {
		_regOps = new LinkedBlockingQueue<Runnable>();
		try {
			_selector = Selector.open();
		} catch (IOException ex) {
			ex.printStackTrace();
			return;
		}
	}
	
	public void start() {
		if (_worker != null) {
			return; // already started
		}
		_worker = new Thread(new Runnable() {
			@Override
			public void run() {
				trace.info("IO context startup.");
				io_loop();
				trace.info("IO context shutdown.");
			}
		});
		_worker.setDaemon(true);
		_worker.start();
	}
	
	public void io_loop() {
		while (true) {
			try {
				// Check registration operations
				while (!_regOps.isEmpty()) {
					Runnable reg_op = _regOps.poll();
					reg_op.run();
				}

				// Wait on operations
				final int ready = _selector.select(1000);
				if (ready > 0) {
					Set<SelectionKey> keys = _selector.selectedKeys();
					trace.debug("IO: KEYS READY " + keys);
					Iterator<SelectionKey> it = keys.iterator();
					while (it.hasNext()) {
						SelectionKey skey = it.next();
						it.remove();
						final boolean ready_ACCEPT = (skey.readyOps() & SelectionKey.OP_ACCEPT) != 0;
						final boolean ready_READ = (skey.readyOps() & SelectionKey.OP_READ) != 0;
						final boolean ready_WRITE = (skey.readyOps() & SelectionKey.OP_WRITE) != 0;
						final boolean ready_CONNECT = (skey.readyOps() & SelectionKey.OP_CONNECT) != 0;

						Object attachment = skey.attachment();
						if (attachment instanceof SocketConnectAcceptor && ready_ACCEPT) {
							((SocketConnectAcceptor) attachment).acceptConnection();
						}
						if (attachment instanceof SocketConnector && ready_CONNECT) {
							((SocketConnector) attachment).finishConnect();
						}
						if (attachment instanceof SocketWriter && ready_READ) {
							((SocketWriter) attachment).read();
						}
						if (attachment instanceof SocketWriter && ready_WRITE) {
							((SocketWriter) attachment).write();
						}
					}

				}

			} catch (Throwable t) {
				trace.warn("IoContext Error ", t);
				t.printStackTrace();
			}
		}
	}

	private synchronized void addRegOp(Runnable r) {
		_regOps.add(r);
		_selector.wakeup();
	}

	private Runnable newRegRWOp(final int ops, final boolean addOps, final IoProvider io, final int flags) {
		return new Runnable() {
			@Override
			public void run() {
				try {
					AbstractSelectableChannel sc = io.channel();
					SelectionKey skey = sc.keyFor(_selector);
					int newOpSet = (skey == null) ? 0 : skey.interestOps();
					if (addOps)
						newOpSet |= ops;
					else
						newOpSet &= ~ops;
					skey = sc.register(_selector, newOpSet);
					skey.attach(io);
				} catch (Exception ex) {
					if ((flags & FL_IGNOREEXCEPTIONS) == 0) {
						IOException ioe = (ex instanceof IOException) ? (IOException) ex
							: new IOException(ex);
						trace.error("IoException in interest registration", ioe);
						io.handleIoException(ioe);
					}
				}
			}
		};
	}

	public void regServerAccept(final PipeController pc, final boolean addOps) {
		// Runnable r = newRegServerAccept(pc);
		Runnable r = newRegRWOp(SelectionKey.OP_ACCEPT, addOps, pc, 0);
		addRegOp(r);
	}

	public void regFinishConnect(final PipeInstance pi, final boolean addOps) {
		// Runnable r = newRegConnectFinish(pi);
		Runnable r = newRegRWOp(SelectionKey.OP_CONNECT, addOps, pi, 0);
		addRegOp(r);
	}

	public void regRW(final int ops, final boolean addOps, final SocketWriter sw, final int flags) {
		Runnable r = newRegRWOp(ops, addOps, sw, flags);
		addRegOp(r);
	}

	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

}
