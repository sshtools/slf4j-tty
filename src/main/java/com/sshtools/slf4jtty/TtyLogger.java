package com.sshtools.slf4jtty;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jline.style.StyleExpression;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.event.LoggingEvent;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.NormalizedParameters;
import org.slf4j.spi.LocationAwareLogger;

import com.sshtools.slf4jtty.TtyLoggerConfiguration.Alignment;

/**
 * <p>
 * Simple implementation of {@link Logger} that sends all enabled log messages,
 * for all defined loggers, to the console ({@code System.err}), enabling colourised
 * and styled output using ANSI escape sequences.
 * <p>
 * It behaves similarly to "SimpleLogger", being originally based on it, but 
 * all configuration is provided by INI files instead of property files, and this 
 * is focused more on terminal output rather than log files.
 * <p>
 * It is also more flexible in the format, size and position of log fields. 
 * <p>
 * TtyLogger and associated classes are based on SimpleLogger implementation to varying degrees.
 */
public class TtyLogger extends LegacyAbstractLogger {

    private static final long serialVersionUID = -632788891211436180L;

    private static final long START_TIME = System.currentTimeMillis();

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
    private transient String shortLogName = null;

    private static ThreadLocal<Boolean> reentrant = new ThreadLocal<>();
    
    private int lastWidth;
    private final Map<String, Integer> fieldWidths = new HashMap<>();
	private final TtyLoggerConfiguration loggerConfiguration;
    
    /**
     * Package access allows only {@link TtyLoggerFactory} to instantiate
     * SimpleLogger instances.
     */
    TtyLogger(String name, TtyLoggerConfiguration loggerConfiguration) {
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
    void write(StringBuilder buf, Throwable t) {
        PrintStream targetStream = loggerConfiguration.outputChoice.getTargetPrintStream();

        synchronized (loggerConfiguration) {
            targetStream.println(buf.toString());
            writeThrowable(t, targetStream);
            targetStream.flush();
        } 

    }

    protected void writeThrowable(Throwable t, PrintStream targetStream) {
        if (t != null) {
        	/* TODO configurable exception printing colors */
        	Throwable nex = t;
			int indent = 0;
			AttributedStringBuilder report = new AttributedStringBuilder();
			while(nex != null) {
				if(indent > 0) {
					report.append(String.format("%" + ( 8 + ((indent - 1 )* 2) ) + "s", ""));
					report.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold());
			        report.append(nex.getClass().getName() + ": " + nex.getMessage() == null ? "No message." : nex.getMessage());
			        report.style(AttributedStyle.DEFAULT);
					report.append(System.lineSeparator());
				}
				
				for(var el : nex.getStackTrace()) {
					report.append(String.format("%" + ( 8 + (indent * 2) ) + "s", ""));
					report.append("at ");
					if(el.getModuleName() != null) {
						report.append(el.getModuleName());
						report.append('/');
					}

					report.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
			        report.append(el.getClassName() + "." + el.getMethodName());
			        report.style(AttributedStyle.DEFAULT);
			        
					if(el.getFileName() != null) {
						report.append('(');
						report.append(el.getFileName());
						if(el.getLineNumber() > -1) {
							report.append(':');
							report.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
					        report.append(String.valueOf(el.getLineNumber()));
					        report.style(AttributedStyle.DEFAULT);
							report.append(')');
						}
					}
					report.append(System.lineSeparator());
				}
				indent++;
				nex = nex.getCause();
			}

            targetStream.print(report.toAttributedString().toAnsi(loggerConfiguration.forceANSI ? null : loggerConfiguration.terminal()));
        }
    }

    private String getFormattedDate() {
        Date now = new Date();
        String dateText;
        synchronized (loggerConfiguration.dateFormatter) {
            dateText = loggerConfiguration.dateFormatter.format(now);
        }
        return dateText;
    }

    private String computeShortName() {
        return name.substring(name.lastIndexOf(".") + 1);
    }

    /**
     * Is the given log level currently enabled?
     *
     * @param logLevel is this level enabled?
     * @return whether the logger is enabled for the given level
     */
    protected boolean isLevelEnabled(int logLevel) {
        // log level are numerically ordered so can use simple numeric
        // comparison
        return (logLevel >= currentLogLevel);
    }

    /** Are {@code trace} messages currently enabled? */
    public boolean isTraceEnabled() {
        return isLevelEnabled(LOG_LEVEL_TRACE);
    }

    /** Are {@code debug} messages currently enabled? */
    public boolean isDebugEnabled() {
        return isLevelEnabled(LOG_LEVEL_DEBUG);
    }

    /** Are {@code info} messages currently enabled? */
    public boolean isInfoEnabled() {
        return isLevelEnabled(LOG_LEVEL_INFO);
    }

    /** Are {@code warn} messages currently enabled? */
    public boolean isWarnEnabled() {
        return isLevelEnabled(LOG_LEVEL_WARN);
    }

    /** Are {@code error} messages currently enabled? */
    public boolean isErrorEnabled() {
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
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
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

    private void innerHandleNormalizedLoggingCall(Level level, List<Marker> markers, String messagePattern, Object[] arguments, Throwable t) {

        StringBuilder buf = new StringBuilder(32);
    	synchronized(fieldWidths) {
	        int width = getWidth();
	        if(this.lastWidth != width) {
	        	this.lastWidth = width;
	        	fieldWidths.clear();
	        	
	        	int total = width - (Math.max(0, loggerConfiguration.layout.size() - 1) * loggerConfiguration.gap);;
	        	int available = total;
	        	int autoFields = 0;
	        	
	        	/* First pass that sets initial size of fixed size fields, and calculates
	        	 * remaining space for any auto fields
	        	 */
	        	for(String field : loggerConfiguration.layout) {
	        		int fieldWidth = loggerConfiguration.fieldWidth.get(field);
        			fieldWidths.put(field, fieldWidth);
	        		if(fieldWidth > 0) {
	        			available -= fieldWidth;
	        			if(available < 0)
	        				available = 0;
	        		}
	        		else
	        			autoFields++;
	        	}
	        	
	        	/* Give each auto field a portion of the available space (if any). 
	        	 */
	        	int autoFieldSize = autoFields == 0 ? 0 : (int)((float)Math.max(autoFields, available) / (float)autoFields);
	        	for(Map.Entry<String, Integer> en : fieldWidths.entrySet()) {
	        		if(en.getValue() == 0) {
	        			en.setValue(autoFieldSize);
	        		}
	        	}
	        	
	        	/* If the total width of the row exceeds the available width, remove one character
	        	 * from each field until all fields will fit  
	        	 */
	        	int totalWidth = fieldWidths.values().stream().reduce(0, Integer::sum);
	        	int overflow = totalWidth - width;
	        	Iterator<Map.Entry<String, Integer>> fieldIt = null;
	        	for(int i = 0 ; i < overflow; i++) {
	        		if(fieldIt == null || !fieldIt.hasNext()) {
	        			fieldIt = fieldWidths.entrySet().iterator();
	        		}
	        		Map.Entry<String, Integer> field = fieldIt.next();
	        		field.setValue(Math.max(1, field.getValue() - 1));
	        	}
	        	
//	        	System.out.println("Col widths: " + String.join(", ", fieldWidths.values().stream().map(i -> String.valueOf(i)).toList()) + " in " + total + " (" + width + ")");
//	        	System.out.println("0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012");
//	        	System.out.println("          1         2         3         4         5         6         7         8         9         0         1         2         3  ");
	       	}
	    }
    	
    	String defaultStyle = null;
    	if(loggerConfiguration.styleAsLevel) {
    		defaultStyle = loggerConfiguration.levelStyles.get(level);
    	}
    	
    	AtomicInteger fieldIdx = new AtomicInteger();
		for (String field : loggerConfiguration.layout) {

			if (field.equals("date-time")) {
				if (loggerConfiguration.dateFormatter != null) {
					appendField(defaultStyle, buf, field, getFormattedDate(), fieldIdx, fieldWidths.get(field));
				} else {
					appendField(defaultStyle, buf, field, String.valueOf(System.currentTimeMillis() - START_TIME), fieldIdx, fieldWidths.get(field));
				}
			}
			else if (field.equals("thread-name")) {
				appendField(defaultStyle, buf, field, Thread.currentThread().getName(), fieldIdx, fieldWidths.get(field));				
			}
			else if (field.equals("thread-id")) {
				appendField(defaultStyle, buf, field, String.valueOf(Thread.currentThread().getId()), fieldIdx, fieldWidths.get(field));				
			}
			else if (field.equals("level")) {
				String levelStyle = loggerConfiguration.levelStyles.get(level);
				String levelText = loggerConfiguration.levelText.get(level);
				appendField(levelStyle, buf, field, levelText, fieldIdx, fieldWidths.get(field));
				
			}
			else if (field.equals("short-name")) {
	            if (shortLogName == null)
	            	shortLogName = computeShortName();
				appendField(defaultStyle, buf, field, shortLogName, fieldIdx, fieldWidths.get(field));
			}
			else if (field.equals("name")) {
				appendField(defaultStyle, buf, field, name, fieldIdx, fieldWidths.get(field));
			}
			else if (field.equals("message")) {
				String str = MessageFormatter.basicArrayFormat(loggerConfiguration.parameterStyle, messagePattern, arguments);
				appendField(defaultStyle, buf, field, str, fieldIdx, fieldWidths.get(field));
			}
			else if (field.equals("markers")) {
				if(markers == null)
					appendField(defaultStyle, buf, field, "", fieldIdx, fieldWidths.get(field));
				else {
					appendField(defaultStyle, buf, field , String.join(",", markers.stream().map(Marker::getName).toList()), fieldIdx, fieldWidths.get(field));
				}
			}
			
		}

        write(buf, t);
    }

	private void appendField(String defaultStyle, StringBuilder buf, String field, String value, AtomicInteger fieldIdx, int fieldWidth) {
		if(fieldIdx.get() > 0 && loggerConfiguration.gap > 0) {
			buf.append(String.format("%" + loggerConfiguration.gap + "s", ""));
		}
		
		var valueStyle = loggerConfiguration.fieldStyles.get(field);
		var decoration = loggerConfiguration.fieldDecoration.get(field);
		var decorationChars = WCWidth.mk_wcswidth(decoration.replace("${" + field + "}", ""));
		var availableWidthWidth = Math.max(1, fieldWidth - decorationChars);
		
		if(defaultStyle != null) {
			valueStyle = defaultStyle.replace("${text}", valueStyle);
		}

		var ftext = valueStyle.replace("${" + field + "}", value);
		
//		int extras =  countOuterCharacters(field, valueStyle);
//		String valueText = padOrTrim(Math.max(1, fieldWidth - extras), value, field.equals("message"));
		
		var attrs = new AttributedStringBuilder();
		var sex = new RecursiveStyleExpression();
		sex.setMaxLength(availableWidthWidth);
		sex.setEllipsis(loggerConfiguration.ellipsis);
		sex.evaluate(attrs, ftext);
		var styledTextLength = WCWidth.mk_wcswidth(attrs.toString());
		if(styledTextLength < availableWidthWidth) {
			var amount = availableWidthWidth - styledTextLength;
			var align = loggerConfiguration.fieldAlignment.get(field);
			if(align == Alignment.LEFT) {
				for(int i = 0 ; i < amount; i++) {
					attrs.append(' ');
				}
			}
			else {
				if(align == Alignment.CENTER) {
					amount /= 2;
				}
				var indented = new AttributedStringBuilder();
				for(int i = 0 ; i < amount; i++) {
					indented.append(' ');
				}
				indented.append(attrs);
				attrs = indented;
			}
		}
		
		var decorated = new AttributedStringBuilder();
		var trm = loggerConfiguration.forceANSI ? null : loggerConfiguration.terminal();
		decorated.appendAnsi(decoration.replace("${" + field + "}", attrs.toAnsi(trm)));
		buf.append(decorated.toAnsi(trm));
		
		fieldIdx.incrementAndGet();
	}
	
	private int countOuterCharacters(String field, String str) {
		StyleExpression expr = new StyleExpression();
		return WCWidth.mk_wcswidth(expr.evaluate(str.replace("${" + field + "}", "")).toString());
	}
    
	private int getWidth() {
		int width = loggerConfiguration.width;
		if(width == 0) {
			try {
				var terminal = loggerConfiguration.terminal();
				if(terminal == null) 
					width = 0;
				else
					width = terminal.getWidth();
				return width < 1 ? loggerConfiguration.fallbackWidth : width;
			}
			catch(Exception e) {
				return loggerConfiguration.fallbackWidth;
			}
		}
		else {
			return width;
		}
	}

    public void log(LoggingEvent event) {
        int levelInt = event.getLevel().toInt();

        if (!isLevelEnabled(levelInt)) {
            return;
        }

        NormalizedParameters np = NormalizedParameters.normalize(event);

        innerHandleNormalizedLoggingCall(event.getLevel(), event.getMarkers(), np.getMessage(), np.getArguments(), event.getThrowable());
    }

    @Override
    protected String getFullyQualifiedCallerName() {
        return null;
    }

    String padOrTrim(int width, String str, boolean valueHasStyles) {
        if(width == 0)
            return str;
        else {
        	int strlen = str.length();
        	int paramChars = 0;
        	if(valueHasStyles) {
        		StyleExpression sexp = new StyleExpression();
        		AttributedString astr = sexp.evaluate(str);
        		int alen = WCWidth.mk_wcswidth(astr.toString());
            	if(str.startsWith("Using transport")) {
            		System.err.println("Value has styles: " + valueHasStyles + " Strlen: " +strlen + " Alen: " + alen );
            	}
	    		paramChars =strlen - alen;
        	}
    		int wwidth = WCWidth.mk_wcswidth(str) ;
    		int diff =strlen - wwidth;
    		if(wwidth < width) {
                return String.format("%-" + Math.max(1, ( width - diff )) + "s", str);
    		}
    		else {
    			return str.substring(0, Math.max(0,  width + paramChars - diff - loggerConfiguration.ellipsisWidth)) + loggerConfiguration.ellipsis;
    		}
        }
    } 
}
