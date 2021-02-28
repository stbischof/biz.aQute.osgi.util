package biz.aQute.gogo.commands.provider.log;

import static org.osgi.service.log.Logger.ROOT_LOGGER_NAME;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.annotations.GogoCommand;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.Logger;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;
import org.slf4j.LoggerFactory;

import biz.aQute.gogo.commands.dtoformatter.DTOFormatter;
import biz.aQute.gogo.commands.provider.base.DisplayUtil;

@GogoCommand(scope = "aQute", function = {
	"levels", "defaultlevel", "addlevel", "rmlevel", //
	"slf4jdebug", "slf4jinfo", "slf4jwarn", "slf4jerror", //
	"tail"
})
@Component(service = LoggerAdminCommands.class, immediate = true)
public class LoggerAdminCommands {

	final BundleContext					context;
	final LoggerAdmin					admin;
	final Map<CommandSession, LogTail>	sessions	= new HashMap<>();
	final LogReaderService				log;

	@Activate
	public LoggerAdminCommands(@Reference
	LoggerAdmin admin, @Reference
	LogReaderService log, @Reference
	DTOFormatter formatter, BundleContext context) {
		this.log = log;
		this.context = context;
		this.admin = admin;
		dtos(formatter);
	}

	@Deactivate
	void deactivate() {
		sessions.values()
			.forEach(LogTail::close);
	}

	void dtos(DTOFormatter f) {
		f.build(LogEntry.class)
			.inspect()
			.method("sequence")
			.format("time", l -> Instant.ofEpochMilli(l.getTime()))
			.method("message")
			.method("bundle")
			.method("servicereference")
			.method("exception")
			.method("logLevel")
			.method("loggerName")
			.method("threadInfo")
			.method("location")
			.part()
			.line()
			.format("time", l -> Instant.ofEpochMilli(l.getTime()))
			.method("message")
			.as(b -> DisplayUtil.dateTime(b.getTime()) + " " + b.getMessage());
	}

	@Descriptor("Show the current log")
	public List<LogEntry> log() throws InterruptedException {

		if (log == null) {
			System.err.println("No log service");
			return Collections.emptyList();
		}
		Enumeration<LogEntry> log2 = log.getLog();
		return Collections.list(log2);
	}

	@Descriptor("Continuously show the log messages")
	public void tail(CommandSession s, @Descriptor("The minimum log level (DEBUG,INFO,WARN,ERROR)")
	LogLevel level) throws IOException {
		LogTail lt = sessions.get(s);
		if (lt == null) {
			sessions.put(s, new LogTail(s, log, level));
		} else {
			sessions.remove(s);
			lt.close();
		}
	}

	@Descriptor("Continuously show all the log messages")
	public void tail(CommandSession s) throws IOException {
		tail(s, LogLevel.DEBUG);
	}

	@Descriptor("Add a logger prefix to the context of the given bundle")
	public Map<String, LogLevel> addlevel(@Descriptor("The logger context bundle")
	Bundle b, @Descriptor("The name of the logger prefix or ROOT for all")
	String name, @Descriptor("The log level to set (DEBUG,INFO,WARN,ERROR)")
	LogLevel level) {
		return add(b.getSymbolicName(), name, level);
	}

	@Descriptor("Remove a log level from the given bundle")
	public Map<String, LogLevel> rmlevel(@Descriptor("The logger context bundle")
	Bundle b, @Descriptor("The name of the logger prefix or ROOT for all")
	String name) {
		return rm(b.getSymbolicName(), name);

	}

	@Descriptor("Add a log name prefix to the root logger")
	public Map<String, LogLevel> addlevel(@Descriptor("The logger name prefix")
	String name, @Descriptor("The log level to set (DEBUG,INFO,WARN,ERROR)")
	LogLevel level) {
		return add(ROOT_LOGGER_NAME, name, level);
	}

	@Descriptor("Remove a log level from the root context")
	public Map<String, LogLevel> rmlevel(String name) {
		return rm(ROOT_LOGGER_NAME, name);
	}

	@Descriptor("Show the levels for a given bundle")
	public Map<String, LogLevel> levels(@Descriptor("The logger context bundle")
	Bundle b) {
		LoggerContext loggerContext = admin.getLoggerContext(b.getSymbolicName());
		if (loggerContext.isEmpty())
			loggerContext = admin.getLoggerContext(null);

		return loggerContext.getLogLevels();
	}

	@Descriptor("Show all log levels")
	public Map<String, Map<String, LogLevel>> levels() {
		Map<String, Map<String, LogLevel>> map = new LinkedHashMap<>();
		map.put(ROOT_LOGGER_NAME, admin.getLoggerContext(ROOT_LOGGER_NAME)
			.getLogLevels());
		for (Bundle b : context.getBundles()) {
			LoggerContext lctx = admin.getLoggerContext(b.getSymbolicName());
			if (lctx.isEmpty())
				continue;

			Map<String, LogLevel> logLevels = lctx.getLogLevels();
			map.put(b.getSymbolicName(), logLevels);
		}
		return map;
	}

	@Descriptor("Show the default level")
	public String defaultlevel() {
		try {
			return admin.getLoggerContext(null)
				.getEffectiveLogLevel(ROOT_LOGGER_NAME)
				.toString();
		} catch (Exception e0) {
			e0.printStackTrace();
			return e0.toString();
		}
	}

	@Descriptor("Set the default level")
	public String defaultlevel(@Descriptor("The log level to set (DEBUG,INFO,WARN,ERROR)")
	LogLevel level) {
		Map<String, LogLevel> map = new HashMap<>();
		map.put(Logger.ROOT_LOGGER_NAME, level);
		admin.getLoggerContext(null)
			.setLogLevels(map);
		return defaultlevel();
	}

	private Map<String, LogLevel> rm(String ctx, String name) {
		LoggerContext loggerContext = admin.getLoggerContext(ctx);
		Map<String, LogLevel> logLevels = loggerContext.getLogLevels();
		logLevels.remove(name);
		loggerContext.setLogLevels(logLevels);
		return loggerContext.getLogLevels();
	}

	private Map<String, LogLevel> add(String ctx, String name, LogLevel level) {
		LoggerContext loggerContext = admin.getLoggerContext(ctx);
		Map<String, LogLevel> logLevels = loggerContext.getLogLevels();
		logLevels.put(name, level);
		loggerContext.setLogLevels(logLevels);
		return loggerContext.getLogLevels();
	}

	@Descriptor("Create an SLF4J debug entry (for testing)")
	public void slf4jdebug(@Descriptor("The message to log")
	Object message) {
		org.slf4j.Logger logger = LoggerFactory.getLogger(LoggerAdminCommands.class);
		logger.debug("{}", message);
	}

	@Descriptor("Create an SLF4J warn entry")
	public void slf4jwarn(@Descriptor("The message to log")
	Object message) {
		org.slf4j.Logger logger = LoggerFactory.getLogger(LoggerAdminCommands.class);
		logger.warn("{}", message);
	}

	@Descriptor("Create an SLF4J info entry")
	public void slf4jinfo(@Descriptor("The message to log")
	Object message) {
		org.slf4j.Logger logger = LoggerFactory.getLogger(LoggerAdminCommands.class);
		logger.info("{}", message);
	}

	@Descriptor("Create an SLF4J error entry")
	public void slf4jerror(@Descriptor("The message to log")
	Object message) {
		org.slf4j.Logger logger = LoggerFactory.getLogger(LoggerAdminCommands.class);
		logger.error("{}", message);
	}
}
