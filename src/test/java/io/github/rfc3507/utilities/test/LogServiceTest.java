package io.github.rfc3507.utilities.test;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.github.rfc3507.utilities.LogService;

@TestInstance(Lifecycle.PER_CLASS)
public class LogServiceTest {
	
	final LogService log = LogService.getInstance(getClass().getSimpleName());
	
	@AfterAll
	public void terminate() throws Exception {
		Thread.sleep(1000);
	}
	
	@Test
	public void testAll() {
		log.info("Test {} {}", "#info()", UUID.randomUUID().toString());
		log.debug("Test {} {}", "#debug()", null);
		log.warning("Test {}", "#warning()");
		log.error("Test {}", "#error()");
		log.error("Test", new Exception("Failure Test", new IllegalStateException("Failure Test #2")));
	}

}
