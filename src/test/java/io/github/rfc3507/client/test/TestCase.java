package io.github.rfc3507.client.test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.github.rfc3507.client.ICAPClient;
import io.github.rfc3507.client.ICAPRequest;
import io.github.rfc3507.client.ICAPResponse;
import io.github.rfc3507.utilities.LogService;

@TestInstance(Lifecycle.PER_CLASS)
public class TestCase {
	private LogService logger = LogService.getInstance("TestCase");
	
	private String dockerContainerId;

	private final int LOCAL_PORT = 50000 + ( (int) ( Math.random() * 10000 ) );

	@BeforeAll
	public void startup() throws Exception {
		final Process process = Runtime.getRuntime().exec(new String[] {"docker", "run", "--rm", "-d", "-p"+LOCAL_PORT+":1344", "claudiney/icap-server-java"});
		final int status = process.waitFor();

		if(status != 0) {
			throw new IllegalStateException("Could not start docker container");
		}

		final ByteArrayOutputStream cache = new ByteArrayOutputStream();
		IOUtils.copy(process.getInputStream(), cache);

		dockerContainerId = new String(cache.toByteArray(), StandardCharsets.US_ASCII).replaceAll("[\\r\\n]$", "");
		logger.info("[Startup] Docker started to load at process {}", this.dockerContainerId);

		logger.info("[Startup] Checking readiness...");

		while(true) {
			Thread.sleep(2000L);

			final Process process2 = Runtime.getRuntime().exec(new String[] {"docker", "logs", this.dockerContainerId});
			process2.waitFor();

			final StringBuilder sb = new StringBuilder("");
			
			final InputStream in = process2.getInputStream();
			int octet = -1;
			while ((octet = in.read()) != -1 ) {
				sb.append((char)octet);
			}

			if( sb.toString().contains("bytecode.cvd updated") ) {
				break;
			};
		}
		
		Thread.sleep(2000L);
		logger.info("[Startup] Ready to run\n");
	}

	static final int connect_timeout = 1000;
	static final int read_timeout = 5000;
	
	@Test
	public void optionsSuccessful() throws Exception {
		logger.info("#optionsSuccessful() STARTED");
		
		final ICAPClient client = new ICAPClient("localhost", LOCAL_PORT);
		logger.info("Icap Client Host {}", client.getIcapHost());
		logger.info("Icap Client Port {}", client.getIcapPort());
		logger.info("Icap Client Version {}", ICAPClient.getIcapVersion());

		client.setConnectTimeout(connect_timeout);
		client.setReadTimeout(read_timeout);

		client.options("info");

		logger.info("#optionsSuccessful() ENDED\n");
	}

	@Test
	public void echoReqmodSucessful() throws Exception {
		logger.info("#echoReqmodSucessful() STARTED");

		final String content = "Hello, There!"; 
		final byte[] raw = content.getBytes(StandardCharsets.US_ASCII);

		final ICAPRequest request = new ICAPRequest("echo", ICAPRequest.Mode.REQMOD);
		request.setHttpRequestHeader(
				( 		"POST / HTTP/1.1\r\n"
					+	"Content-Type: text/plain\r\n"
					+	"Content-Length: " + raw.length + "\r\n"
				).getBytes(StandardCharsets.US_ASCII)
			);
		request.setHttpRequestBody(raw);

		final ICAPClient client = new ICAPClient("localhost", LOCAL_PORT);
		client.setConnectTimeout(connect_timeout);
		client.setReadTimeout(read_timeout);
		final ICAPResponse response = client.execute(request);
		
		logger.info("Response adaptation: HTTP request headers:\n{}", new String(response.getHttpRequestHeader(), StandardCharsets.US_ASCII));
		logger.info("Response adaptation: HTTP raw request body:\n{}", new String(response.getHttpRawRequestBody(), StandardCharsets.US_ASCII));
		logger.info("Response adaptation: HTTP shrink request body:\n{}", new String(response.getHttpShrinkRequestBody(), StandardCharsets.US_ASCII));

		logger.info("#echoReqmodSucessful() ENDED\n");
	}

	@Test
	public void echoRespmodSucessful() throws Exception {
		logger.info("#echoRespmodSucessful() STARTED");

		final String content = "Hello, There!"; 
		final byte[] raw = content.getBytes(StandardCharsets.US_ASCII);

		final ICAPRequest request = new ICAPRequest("echo", ICAPRequest.Mode.RESPMOD);
		request.setHttpResponseHeader(
				( 		"HTTP/1.1 200 OK\r\n"
					+	"Content-Type: text/plain\r\n"
					+	"Content-Length: " + raw.length + "\r\n"
				).getBytes(StandardCharsets.US_ASCII)
			);
		request.setHttpResponseBody(raw);

		final ICAPClient client = new ICAPClient("localhost", LOCAL_PORT);
		client.setConnectTimeout(connect_timeout);
		client.setReadTimeout(read_timeout);
		final ICAPResponse response = client.execute(request);
		
		logger.info("Response adaptation: HTTP response headers:\n{}", new String(response.getHttpResponseHeader(), StandardCharsets.US_ASCII));
		logger.info("Response adaptation: HTTP raw response body:\n{}", new String(response.getHttpRawResponseBody(), StandardCharsets.US_ASCII));
		logger.info("Response adaptation: HTTP shrink response body:\n{}", new String(response.getHttpShrinkResponseBody(), StandardCharsets.US_ASCII));

		logger.info("#echoRespmodSucessful() ENDED\n");
	}

	@Test
	public void viruscanSucessful() throws Exception {
		logger.info("#viruscanSucessful() STARTED");

		final URL url = new URL("https://secure.eicar.org/eicar.com");
		final HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
		https.setDoInput(true);

		final ByteArrayOutputStream rawData = new ByteArrayOutputStream();
		IOUtils.copy(https.getInputStream(), rawData);

		final byte[] raw = rawData.toByteArray();

		final ICAPRequest request = new ICAPRequest("virus_scan", ICAPRequest.Mode.REQMOD);
		request.setHttpResponseHeader(
				( 		"POST /eicar.com HTTP/1.1\r\n"
					+	"Content-Type: text/plain\r\n"
					+	"Content-Length: " + raw.length + "\r\n"
				).getBytes(StandardCharsets.US_ASCII)
			);
		request.setHttpResponseBody(raw);

		final ICAPClient client = new ICAPClient("localhost", LOCAL_PORT);
		client.setConnectTimeout(connect_timeout);
		client.setReadTimeout(15000);
		final ICAPResponse response = client.execute(request);
		
		logger.info("Response adaptation: HTTP response headers:\n{}", new String(response.getHttpResponseHeader(), StandardCharsets.US_ASCII));
		logger.info("Response adaptation: HTTP raw response body:\n{}", new String(response.getHttpRawResponseBody(), StandardCharsets.US_ASCII));
		logger.info("Response adaptation: HTTP shrink response body:\n{}", new String(response.getHttpShrinkResponseBody(), StandardCharsets.US_ASCII));

		logger.info("#viruscanSucessful() ENDED\n");
	}

	@AfterAll
	public void terminate() throws Exception {
		logger.info("Stopping Docker process {}", this.dockerContainerId);
		
		final Process process = Runtime.getRuntime().exec(new String[] {"docker", "stop", this.dockerContainerId});
		process.waitFor();

		final ByteArrayOutputStream cache = new ByteArrayOutputStream();
		IOUtils.copy(process.getInputStream(), cache);

		final ByteArrayOutputStream failure = new ByteArrayOutputStream();
		IOUtils.copy(process.getErrorStream(), failure);
		
		logger.info("Testcase completed");
	}
	
}
