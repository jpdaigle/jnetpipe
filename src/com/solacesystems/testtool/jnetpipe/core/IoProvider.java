package com.solacesystems.testtool.jnetpipe.core;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelectableChannel;

public interface IoProvider {
	public void handleIoException(IOException ex);

	public AbstractSelectableChannel channel();

}
