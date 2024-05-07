package com.sshtools.slf4jtty;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.sshtools.jini.INI;
import com.sshtools.jini.config.INISet;
import com.sshtools.jini.config.Monitor;

import uk.co.bithatch.nativeimage.annotations.Resource;

@Resource({"com/sshtools/slf4jtty/TtyConfiguration\\.schema\\.ini"})
public final class TtyConfiguration {
	
	private final static class Default {
		private static TtyConfiguration DEFAULT = new TtyConfiguration();
	}
	
	public static TtyConfiguration get() {
		return Default.DEFAULT;
	}
	
	public enum DateTimeType {
		DATE, TIME, DATE_TIME
	}

	private final INISet.Builder bldr;
	private INISet set;
	private Terminal terminal;
	private Supplier<Terminal> terminalFactory; 

	private TtyConfiguration() {
    	bldr =  new INISet.Builder("slf4j-tty").
				withApp("slf4j-tty").
				withSchema(TtyConfiguration.class, "TtyConfiguration.schema.ini");
		bldr.withMonitor(new Monitor());
	}
	
	public void terminalFactory(Supplier<Terminal> terminalFactory) {
		if(this.terminalFactory != null) {
			throw new IllegalStateException("Already have a factory.");
		}
		if(terminal != null) {
			throw new IllegalStateException("Already have a terminal.");
		}
		this.terminalFactory = terminalFactory;
	}

	public INISet.Builder configurationBuilder() {
		if(set != null)
			throw new IllegalStateException("Cannot get build after it has been used.");
		return bldr;
	}
	
	INI configuration() {
		if(set == null)
			throw new IllegalStateException("Cannot get configuration before the set is built.");
		return set.document();
	}
	
	Terminal terminal() {
		if(set == null)
			throw new IllegalStateException("Cannot get terminal before configuration is built.");
		if(terminal == null) {
			if(terminalFactory == null)
				try {
					return terminal = TerminalBuilder.terminal();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			else
				return terminal = terminalFactory.get();
		}
		else
			return terminal;
	}
	
	INISet build() {
		if(set != null)
			throw new IllegalStateException("Already build.");
		return set = bldr.build();
	}
}
