package com.solacesystems.testtool.jnetpipe;

import java.net.Inet4Address;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import com.solacesystems.testtool.jnetpipe.core.IoContext;
import com.solacesystems.testtool.jnetpipe.core.PipeController;

public class JNetPipe {

	public static void main(String[] args) {
		
		boolean debug = false;
		boolean stats = false;
		int localPort = 0, remotePort = 0;
		String remoteHost = null;
		
		for(int i = 0; i < args.length; i++) {
			if (args[i].equals("-d")) {
				debug = true;
			} else if (args[i].equals("-h")) {
				printUsage();
				return;
			} else if (args[i].equals("-s")) {
				stats = true;
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
			pc.start();
			System.out.println("Sleeping...");
			Thread.sleep(3600 * 1000);
		} catch (Exception ex) {
			System.err.println("MAIN>> " + ex);
			ex.printStackTrace();
		}
	}
	
	private static void printUsage() {
		System.out.println("Usage: JNetPipe [-d] [-s] LOCALPORT:REMOTEHOST:REMOTEPORT");
		System.out.println("   -d: debug");
		System.out.println("   -s: Dump stats");
	}
	
	private static void configureLogging(boolean debug) {
		ConsoleAppender ap = new ConsoleAppender();
		Layout layout = new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN);
		ap.setLayout(layout);
		ap.setThreshold(debug ? Level.DEBUG : Level.INFO);
		ap.activateOptions();
		BasicConfigurator.configure(ap);
	}
}
