package com.solacesystems.testtool.jnetpipe.core;

import java.io.IOException;

public interface SocketConnectAcceptor extends IoProvider {
	public void acceptConnection() throws IOException;
}
