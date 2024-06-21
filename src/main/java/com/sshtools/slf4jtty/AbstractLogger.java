package com.sshtools.slf4jtty;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.NormalizedParameters;
import org.slf4j.spi.LocationAwareLogger;

public abstract class AbstractLogger extends LegacyAbstractLogger {

    private static final long serialVersionUID = -632788891211436180L;

    protected static final long START_TIME = System.currentTimeMillis();

    protected static final int LOG_LEVEL_TRACE = LocationAwareLogger.TRACE_INT;
    protected static final int LOG_LEVEL_DEBUG = LocationAwareLogger.DEBUG_INT;
    protected static final int LOG_LEVEL_INFO = LocationAwareLogger.INFO_INT;
    protected static final int LOG_LEVEL_WARN = LocationAwareLogger.WARN_INT;
    protected static final int LOG_LEVEL_ERROR = LocationAwareLogger.ERROR_INT;

    // The OFF level can only be used in configuration files to disable logging.
    // It has
    // no printing method associated with it in o.s.Logger interface.
    protected static final int LOG_LEVEL_OFF = LOG_LEVEL_ERROR + 10;

    /** The current log level */
    private final int currentLogLevel;
    /** The short name of this simple log instance */
    protected transient String shortLogName = null;

    private static ThreadLocal<Boolean> reentrant = new ThreadLocal<>();
    
    protected final TtyLoggerConfiguration loggerConfiguration;
    
    /**
     * Package access allows only {@link TtyLoggerFactory} to instantiate
     * SimpleLogger instances.
     */
    AbstractLogger(String name, TtyLoggerConfiguration loggerConfiguration) {
        this.name = name;
        this.loggerConfiguration = loggerConfiguration;

        int levelString = recursivelyComputeLevel();
        if (levelString != -1) {
            this.currentLogLevel = levelString;
        } else {
            this.currentLogLevel = loggerConfiguration.defaultLogLevel;
        }
    }

    int recursivelyComputeLevel() {
        String tempName = name;
        int levelString = -1;
        int indexOfLastDot = tempName.length();
        while ((levelString == -1) && (indexOfLastDot > -1)) {
            tempName = tempName.substring(0, indexOfLastDot);
            levelString = loggerConfiguration.loggerLevels.getOrDefault(tempName, -1);
            indexOfLastDot = String.valueOf(tempName).lastIndexOf(".");
        }
        return levelString;
    }

    /**
     * To avoid intermingling of log messages and associated stack traces, the two
     * operations are done in a synchronized block.
     * 
     * @param buf
     * @param t
     */
    final void write(StringBuilder buf, Throwable t) {
        PrintStream targetStream = loggerConfiguration.outputChoice.getTargetPrintStream();

        synchronized (loggerConfiguration) {
            targetStream.println(buf.toString());
            writeThrowable(t, targetStream);
            targetStream.flush();
        } 

    }

    protected abstract void writeThrowable(Throwable t, PrintStream targetStream);

    protected final String getFormattedDate() {
        Date now = new Date();
        String dateText;
        synchronized (loggerConfiguration.dateFormatter) {
            dateText = loggerConfiguration.dateFormatter.format(now);
        }
        return dateText;
    }

    protected final String computeShortName() {
        return name.substring(name.lastIndexOf(".") + 1);
    }

    /**
     * Is the given log level currently enabled?
     *
     * @param logLevel is this level enabled?
     * @return whether the logger is enabled for the given level
     */
    protected final boolean isLevelEnabled(int logLevel) {
        // log level are numerically ordered so can use simple numeric
        // comparison
        return (logLevel >= currentLogLevel);
    }

    /** Are {@code trace} messages currently enabled? */
    public final boolean isTraceEnabled() {
        return isLevelEnabled(LOG_LEVEL_TRACE);
    }

    /** Are {@code debug} messages currently enabled? */
    public final boolean isDebugEnabled() {
        return isLevelEnabled(LOG_LEVEL_DEBUG);
    }

    /** Are {@code info} messages currently enabled? */
    public final boolean isInfoEnabled() {
        return isLevelEnabled(LOG_LEVEL_INFO);
    }

    /** Are {@code warn} messages currently enabled? */
    public final boolean isWarnEnabled() {
        return isLevelEnabled(LOG_LEVEL_WARN);
    }

    /** Are {@code error} messages currently enabled? */
    public final boolean isErrorEnabled() {
        return isLevelEnabled(LOG_LEVEL_ERROR);
    }

    /**
     * SimpleLogger's implementation of
     * {@link org.slf4j.helpers.AbstractLogger#handleNormalizedLoggingCall(Level, Marker, String, Object[], Throwable) AbstractLogger#handleNormalizedLoggingCall}
     * }
     *
     * @param level the SLF4J level for this event
     * @param marker  The marker to be used for this event, may be null.
     * @param messagePattern The message pattern which will be parsed and formatted
     * @param arguments  the array of arguments to be formatted, may be null
     * @param throwable  The exception whose stack trace should be logged, may be null
     */
    @Override
    protected final void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
    	/* JLine uses SLF4J for logging, which we may trigger by using it to style our text! Avoid
    	 * re-entering.
    	 */
    	Boolean reentered = reentrant.get();
    	if(Boolean.TRUE.equals(reentered)) {
    		return;
    	}
    	
    	try {
    		reentrant.set(true);
    		
	        List<Marker> markers = null;
	
	        if (marker != null) {
	            markers = new ArrayList<>();
	            markers.add(marker);
	        }
	
	        innerHandleNormalizedLoggingCall(level, markers, messagePattern, arguments, throwable);
    	}
    	finally {
    		reentrant.remove();
    	}
    }

    protected abstract void innerHandleNormalizedLoggingCall(Level level, List<Marker> markers, String messagePattern, Object[] arguments, Throwable t);

    public final void log(LoggingEvent event) {
        int levelInt = event.getLevel().toInt();

        if (!isLevelEnabled(levelInt)) {
            return;
        }

        NormalizedParameters np = NormalizedParameters.normalize(event);

        innerHandleNormalizedLoggingCall(event.getLevel(), event.getMarkers(), np.getMessage(), np.getArguments(), event.getThrowable());
    }

    @Override
    protected final String getFullyQualifiedCallerName() {
        return null;
    } 
}
