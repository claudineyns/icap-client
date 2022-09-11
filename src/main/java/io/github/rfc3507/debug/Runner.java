package io.github.rfc3507.debug;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import io.github.rfc3507.client.ICAPClient;
import io.github.rfc3507.client.ICAPRequest;
import io.github.rfc3507.client.ICAPResponse;
import io.github.rfc3507.client.ICAPRequest.Mode;

public class Runner {
	
	public static void main(String[] args) throws Exception {
		
		final String host = "localhost";
		final int port = 1344;
		
//		final String host = "192.168.49.2";
//		final int port = 30268;
		
		final String service = "virus_scan";

		ICAPResponse response = null; 
				
		final ICAPClient client = new ICAPClient(host, port);
		response = client.options(service);
		System.out.println("--------- OPTIONS -------------");
		show(response);
		System.out.println();
		
		final ICAPRequest request = new ICAPRequest(service, Mode.RESPMOD);
//		request.setHttpRequestBody(
		request.setHttpResponseBody(
				Files.readAllBytes(new File(System.getProperty("user.home")+"/Downloads/eicar.com").toPath())
			);
		response = client.execute(request);
		System.out.println("--------- RESPONSE -------------");
		show(response);
		System.out.println();
		
	}
	
	static void show(ICAPResponse response) {
		System.out.println(response.recoverStatusLine());
		for(Map.Entry<String, List<String>> entry: response.getHeaderEntries().entrySet()) {
			System.out.println(entry.getKey()+": "+entry.getValue());
		}
		final byte[] responseBody = response.getHttpShrinkResponseBody();
		if(responseBody != null) {
			System.out.println("-----");
			System.out.println(new String(responseBody, StandardCharsets.US_ASCII));
		}
	}

}
