package com.solacesystems.testtool.jnetpipe;

import java.net.Inet4Address;
import org.apache.log4j.BasicConfigurator;
import com.solacesystems.testtool.jnetpipe.core.IoContext;
import com.solacesystems.testtool.jnetpipe.core.PipeController;

public class JNetPipe {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BasicConfigurator.configure();
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
}
