package net.rfc3507.debug;

import net.rfc3507.client.ICAPClient;

public class Runner {
	
	public static void main(String[] args) throws Exception {
		
		final String host = "localhost";
		final int port = 1344;
		
		final String service = "info";

		final ICAPClient client = new ICAPClient(host, port);
		client.options(service);
		
	}

}
