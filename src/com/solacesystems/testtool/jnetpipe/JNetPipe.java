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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		configureLogging();
		
		IoContext ctx = new IoContext();
		try {
			PipeController pc = new PipeController(
				5901, 
				Inet4Address.getByName("192.168.1.246"),
				5901, 
				ctx);
			
			pc.start();
			System.out.println("Sleeping...");
			Thread.sleep(3600 * 1000);
		} catch (Exception ex) {
			System.err.println("MAIN>> " + ex);
			ex.printStackTrace();
		}
	}
	
	private static void configureLogging() {
		ConsoleAppender ap = new ConsoleAppender();
		Layout layout = new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN);
		ap.setLayout(layout);
		ap.setThreshold(Level.INFO);
		ap.activateOptions();
		BasicConfigurator.configure(ap);
	}
}
