package com.solacesystems.testtool.jnetpipe;

import java.io.IOException;
import java.net.Inet4Address;

import com.solacesystems.testtool.jnetpipe.core.IoContext;
import com.solacesystems.testtool.jnetpipe.core.PipeController;

/**
 * Programmatic JNetPipe users should acquire a PipeController from here.
 * 
 * @author jpdaigle
 * 
 */
public class PipeControllerFactory {

	/**
	 * Init a IoContext and a new PipeController, registering it in that
	 * IoContext.
	 * 
	 * @param localPort
	 * @param remoteHost
	 * @param remotePort
	 * @return
	 * @throws IOException
	 */
	public static PipeController createPipeController(int localPort, String remoteHost, int remotePort)
			throws IOException {
		IoContext ctx = new IoContext();
		ctx.start();
		PipeController pc = new PipeController(localPort, Inet4Address.getByName(remoteHost), remotePort, ctx);
		return pc;
	}

}
