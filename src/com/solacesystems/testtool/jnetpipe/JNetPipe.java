package com.solacesystems.testtool.jnetpipe;

import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.util.JConsole;
import bsh.util.NameCompletionTable;
import com.solacesystems.testtool.jnetpipe.core.IoContext;
import com.solacesystems.testtool.jnetpipe.core.PipeController;

public class JNetPipe {

	private static Interpreter interpreter;

	public static void main(String[] args) {

		boolean debug = false;
		boolean stats = false;
		boolean shell = false;
		int localPort = 0, remotePort = 0;
		String remoteHost = null;

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-d")) {
				debug = true;
			} else if (args[i].equals("-h")) {
				printUsage();
				return;
			} else if (args[i].equals("-s") || args[i].equals("--stats")) {
				stats = true;
			} else if (args[i].equals("--shell")) {
				shell = true;
			} else {
				// Argument is a tunnel spec PORT:REMOTEHOST:REMOTEPORT
				String[] tspec = args[i].split(":");
				if (tspec.length != 3) {
					printUsage();
					return;
				}
				localPort = Integer.parseInt(tspec[0]);
				remoteHost = tspec[1];
				remotePort = Integer.parseInt(tspec[2]);
			}
		}

		if (remoteHost == null) {
			printUsage();
			return;
		}

		configureLogging(debug);

		IoContext ctx = new IoContext();
		ctx.start();
		try {
			PipeController pc = new PipeController(
				localPort, 
				Inet4Address.getByName(remoteHost),
				remotePort, 
				ctx);
			if (stats)
				pc.enableStats();

			// Start PipeController (accepts connections)
			pc.start();

			// Start the BeanShell ui
			if (shell)
				startConsole(pc, ctx);

			// Wait forever
			Thread.sleep(Long.MAX_VALUE);
		} catch (IOException ex) {
			System.err.println("MAIN>> " + ex);
			ex.printStackTrace();
		} catch (InterruptedException ex) {
			System.err.println("MAIN>> " + ex);
			ex.printStackTrace();
		}
	}

	private static void printUsage() {
		System.out
			.println("Usage: JNetPipe [-d] [--stats] [--shell] LOCALPORT:REMOTEHOST:REMOTEPORT");
		System.out.println("   -d:      Debug");
		System.out.println("   --stats: Dump stats");
		System.out.println("   --shell: Start shell");
	}

	private static void configureLogging(boolean debug) {
		ConsoleAppender ap = new ConsoleAppender();
		Layout layout = new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN);
		ap.setLayout(layout);
		ap.setThreshold(debug ? Level.DEBUG : Level.INFO);
		ap.activateOptions();
		BasicConfigurator.configure(ap);
	}

	private static void startConsole(final PipeController pipe, final IoContext ioContext) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// This is all standard boilerplate for creating a JFrame
				JFrame frame = new JFrame();
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setPreferredSize(new Dimension(900, 600));
				frame.setTitle("JNetPipe shell");

				// BeanShell objects
				final String LOOPEXIT = "loopexit";
				/*
				 * Loops in bsh scripts should always check the state of
				 * "loopexit" in order to process Ctrl+C:
				 * 
				 * while(!loopexit) { ... };
				 */
				final JConsole console = new JConsole() {
					@Override
					public void keyPressed(KeyEvent e) {
						super.keyPressed(e);
						try {
							if (e.getKeyCode() == KeyEvent.VK_C
								&& ((e.getModifiers() & InputEvent.CTRL_MASK) > 0)) {
								// ctrl+c, set loopexit
								interpreter.set(LOOPEXIT, true);
							} else {
								// Any other key: clear loopflag
								interpreter.set(LOOPEXIT, false);
							}
						} catch (EvalError e1) {
							e1.printStackTrace();
						}
					}
				};
				interpreter = new Interpreter(console);
				try {
					// The PipeController will be accessible as "netpipe"
					interpreter.set("netpipe", pipe);
					interpreter.set(LOOPEXIT, false);
				} catch (EvalError e) {
					e.printStackTrace();
				}
				NameCompletionTable nct = new NameCompletionTable();
				nct.add(interpreter.getNameSpace());
				console.setNameCompletion(nct);
				interpreter.setShowResults(true);

				// Build the frame
				frame.add(console);
				final Thread thread = new Thread(interpreter, "BeanShell");
				thread.setDaemon(true);
				thread.start();
				frame.pack();
				frame.setVisible(true);

				// Defer shell init until after the BeanShell banner
				Runnable r_setupShell = new Runnable() {
					@Override
					public void run() {
						try {
							interpreter.eval("importCommands(\"commands\");");
							interpreter.eval("printWelcome(\"" + pipe.toString() + "\")");
						} catch (EvalError e) {
							e.printStackTrace();
						}
					}
				};
				ioContext.getScheduler().schedule(r_setupShell, 250, TimeUnit.MILLISECONDS);

			}
		});
	}
}
