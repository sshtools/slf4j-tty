package com.sshtools.slf4jtty;

import java.util.function.Supplier;

import org.jline.terminal.Terminal;

import com.sshtools.jini.config.INISet;
import com.sshtools.jini.config.Monitor;

import uk.co.bithatch.nativeimage.annotations.Resource;

@Resource({"com/sshtools/slf4jtty/TtyConfiguration\\.schema\\.ini"})
public final class TtyConfigurationSet {
	
	private final static class Default {
		private static TtyConfigurationSet DEFAULT = new TtyConfigurationSet();
	}
	
	public static TtyConfigurationSet get() {
		return Default.DEFAULT;
	}
	
	public enum DateTimeType {
		DATE, TIME, DATE_TIME
	}

	private final INISet.Builder loggersBldr;
	private final INISet.Builder bldr;
	private Supplier<Terminal> terminalFactory;
	private TtyLoggerConfiguration configuration;
	
	TtyConfigurationSet() {
		
    	var mtr = new Monitor();
    	
		bldr =  new INISet.Builder("output").
				withApp("slf4j-tty").
				withSchema(TtyConfigurationSet.class, "TtyConfiguration.schema.ini").
				withOptionalDefault(TtyConfigurationSet.class, "/slf4j-tty.ini").
				withMonitor(mtr);

		loggersBldr =  new INISet.Builder("loggers").
				withApp("slf4j-tty").
				withOptionalDefault(TtyConfigurationSet.class, "/slf4j-tty-loggers.ini").
				withMonitor(mtr);
	}
	
	public void terminalFactory(Supplier<Terminal> terminalFactory) {
		if(configuration!= null) {
			throw new IllegalStateException("Cannot get after configuration has been built.");
		}
		this.terminalFactory = terminalFactory;
	}
	
	public INISet.Builder configurationBuilder() {
		if(configuration  != null)
			throw new IllegalStateException("Cannot get after configuration has been built.");
		return bldr;
	}
	
	public INISet.Builder loggersBuilder() {
		if(configuration  != null)
			throw new IllegalStateException("Cannot get after configuration has been built.");
		return loggersBldr;
	}
	
	TtyLoggerConfiguration build() {
		if(configuration != null)
			throw new IllegalStateException("Already build.");
		
		return configuration = new TtyLoggerConfiguration(bldr.build(), loggersBldr.build(), terminalFactory);
	}
}
