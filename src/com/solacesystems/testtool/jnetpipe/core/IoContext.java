package com.solacesystems.testtool.jnetpipe.core;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.Logger;

public class IoContext {

	public static final Logger trace = Logger.getLogger(IoContext.class);

	private Thread _worker;
	private Queue<Runnable> _regOps;
	private Selector _selector;

	public IoContext() {
		try {
			_selector = Selector.open();
		} catch (IOException ex) {
			ex.printStackTrace();
			return;
		}

		_regOps = new LinkedBlockingQueue<Runnable>();
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
				long stime = System.currentTimeMillis();
				int ready = _selector.select(1000);
				long etime = System.currentTimeMillis();
				Set<SelectionKey> keys = _selector.selectedKeys();
				System.out.println(">> " + (etime - stime) + " keys: " + keys.size());
				if (keys.size() > 0) {
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

	private void addRegOp(Runnable r) {
		_regOps.add(r);
		_selector.wakeup();
	}


	private Runnable newRegRWOp(final int ops, final boolean addOps, final IoProvider io) {
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
				} catch (ClosedChannelException cce) {
					trace.error("ClosedChannelException in interest registration", cce);
					io.handleIoException(cce);
				}
			}
		};
	}

	public void regServerAccept(final PipeController pc, final boolean addOps) {
		// Runnable r = newRegServerAccept(pc);
		Runnable r = newRegRWOp(SelectionKey.OP_ACCEPT, addOps, pc);
		addRegOp(r);
	}

	public void regFinishConnect(final PipeInstance pi, final boolean addOps) {
		// Runnable r = newRegConnectFinish(pi);
		Runnable r = newRegRWOp(SelectionKey.OP_CONNECT, addOps, pi);
		addRegOp(r);
	}

	public void regRW(final int ops, final boolean addOps, final SocketWriter sw) {
		Runnable r = newRegRWOp(ops, addOps, sw);
		addRegOp(r);
	}

}
