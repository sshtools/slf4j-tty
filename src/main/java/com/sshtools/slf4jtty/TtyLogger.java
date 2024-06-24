package com.sshtools.slf4jtty;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jline.style.StyleExpression;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;

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
@SuppressWarnings("serial")
public class TtyLogger extends AbstractLogger {

    private int lastWidth;
    private final Map<String, Integer> fieldWidths = new HashMap<>();
    
    /**
     * Package access allows only {@link TtyLoggerFactory} to instantiate
     * SimpleLogger instances.
     */
    TtyLogger(String name, TtyLoggerConfiguration loggerConfiguration) {
    	super(name, loggerConfiguration);
    }

    @Override
    protected void writeThrowable(Throwable t, PrintStream targetStream) {
        if (t != null) {
        	/* TODO configurable exception printing colors */
        	Throwable nex = t;
			int indent = 0;
			AttributedStringBuilder report = new AttributedStringBuilder();
			while(nex != null) {
				if(indent > 0) {
					report.append(String.format("%" + ( 8 + ((indent - 1 )* 2) ) + "s", ""));
				}
				report.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold());
		        report.append(nex.getClass().getName() + ": " + nex.getMessage() == null ? "No message." : nex.getMessage());
		        report.style(AttributedStyle.DEFAULT);
				report.append(System.lineSeparator());
				
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

			switch(loggerConfiguration.format) {
			case AUTO:
	            targetStream.print(report.toAttributedString().toAnsi(loggerConfiguration.terminal()));
	            break;
			case ANSI:
	            targetStream.print(report.toAttributedString().toAnsi(null));
	            break;
			case PLAIN:
	            targetStream.print(report.toAttributedString().toString());
	            break;
	        default:
	        	throw new UnsupportedOperationException();
			}
        }
    }

    protected void innerHandleNormalizedLoggingCall(Level level, List<Marker> markers, String messagePattern, Object[] arguments, Throwable t) {

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
		


		switch(loggerConfiguration.format) {
		case AUTO:
			decorated.appendAnsi(decoration.replace("${" + field + "}", attrs.toAnsi(loggerConfiguration.terminal())));
			buf.append(decorated.toAnsi(loggerConfiguration.terminal()));
            break;
		case ANSI:
			decorated.appendAnsi(decoration.replace("${" + field + "}", attrs.toAnsi(null)));
			buf.append(decorated.toAnsi(null));
            break;
		case PLAIN:
			decorated.appendAnsi(decoration.replace("${" + field + "}", attrs.toString()));
			buf.append(decorated.toString());
            break;
        default:
        	throw new UnsupportedOperationException();
		}
		
		fieldIdx.incrementAndGet();
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
