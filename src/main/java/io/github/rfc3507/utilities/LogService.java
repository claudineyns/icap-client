package io.github.rfc3507.utilities;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public final class LogService {

	public static enum LogLevel {
		INFO, SUCCESS, DEBUG, WARN, ERROR;
	}

	public static final LogService INSTANCE = new LogService();

	public static final LogService getInstance(final String name) {
		return new LogService(name);
	}

	private LogService() {
		/***/
	}

	private String name = "";

	private LogService(final String name) {
		this();
		this.name = String.format("[%s]", name);
	}

	public void info(final String template, final Object... args) {
		logv(template, LogLevel.INFO, args);
	}

	public void debug(final String template, final Object... args) {
		logv(template, LogLevel.DEBUG, args);
	}

	public void error(final String template, final Object... args) {
		logv(template, LogLevel.ERROR, args);
	}

	public void error(final String message, final Throwable throwable) {
		final StringBuilder sb = new StringBuilder(message);
		
		Throwable caused = throwable;
		
		boolean first = true;

		do {
			if( ! first ) {
				sb.append(String.format("%nCaused by %s", caused.getMessage()));
			}
			first = false;

			Arrays.asList(caused.getStackTrace())
			.forEach( element -> 
			{
				final String className = element.getClassName();
				final String methodName = element.getMethodName();
				final String fileName = element.getFileName();
				final int lineNumber = element.getLineNumber();
				final String trace = String.format("%n  at %s#%s (%s:%d)", className, methodName, fileName, lineNumber);
				sb.append(trace);
			});

			caused = caused.getCause();			
		} while(caused != null);

		logv(sb.toString(), LogLevel.ERROR);
	}

	public void warning(final String template, final Object... args) {
		logv(template, LogLevel.WARN, args);
	}

	private void logv(final String template, final LogLevel level, final Object... args) {
		String messageFormatted = template;

		for (int i = 0; i < args.length; ++i) {
			final String d = Optional.ofNullable(args[i]).orElse("").toString();
			messageFormatted = messageFormatted.replaceFirst("\\{\\}", d);
		}
		messageFormatted = messageFormatted.replaceAll("\\{\\}", "");

		log(messageFormatted, level);
	}

	private byte log(final String message, final LogLevel level) {
		final String dateTime = LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss", Locale.US));

		final String levelTag = String.format("[%s]", level.name());
		final String outMessage = String.format("%s %-7s %s %s%n", dateTime, levelTag, name, message);

		switch (level) {
			case INFO:
				return printOut(outMessage);
			case DEBUG:
				return printOut(outMessage);
			case WARN:
				return printErr(outMessage);
			case ERROR:
				return printErr(outMessage);
			default:
				return 0;
		}
	}

	private byte printOut(final String message) {
		return print(System.out, message);
	}

	private byte printErr(final String message) {
		return print(System.err, message);
	}

	private byte print(final PrintStream writer, final String message) {
		writer.print(message);
		return 0;
	}

}
