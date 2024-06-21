package com.sshtools.slf4jtty;

import java.io.PrintStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Marker;
import org.slf4j.event.Level;

public class JsonLogger extends AbstractLogger {

    private static final long serialVersionUID = 5253751888658435793L;
	private static final String RND = UUID.randomUUID().toString().replace("-", "");

	JsonLogger(String name, TtyLoggerConfiguration loggerConfiguration) {
    	super(name, loggerConfiguration);
    }

    protected void writeThrowable(Throwable t, PrintStream targetStream) {
//        if (t != null) {
//        	/* TODO configurable exception printing colors */
//        	Throwable nex = t;
//			int indent = 0;
//			AttributedStringBuilder report = new AttributedStringBuilder();
//			while(nex != null) {
//				if(indent > 0) {
//					report.append(String.format("%" + ( 8 + ((indent - 1 )* 2) ) + "s", ""));
//				}
//				report.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold());
//		        report.append(nex.getClass().getName() + ": " + nex.getMessage() == null ? "No message." : nex.getMessage());
//		        report.style(AttributedStyle.DEFAULT);
//				report.append(System.lineSeparator());
//
//				for(var el : nex.getStackTrace()) {
//					report.append(String.format("%" + ( 8 + (indent * 2) ) + "s", ""));
//					report.append("at ");
//					if(el.getModuleName() != null) {
//						report.append(el.getModuleName());
//						report.append('/');
//					}
//
//					report.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
//			        report.append(el.getClassName() + "." + el.getMethodName());
//			        report.style(AttributedStyle.DEFAULT);
//
//					if(el.getFileName() != null) {
//						report.append('(');
//						report.append(el.getFileName());
//						if(el.getLineNumber() > -1) {
//							report.append(':');
//							report.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
//					        report.append(String.valueOf(el.getLineNumber()));
//					        report.style(AttributedStyle.DEFAULT);
//							report.append(')');
//						}
//					}
//					report.append(System.lineSeparator());
//				}
//				indent++;
//				nex = nex.getCause();
//			}
//
//            targetStream.print(report.toAttributedString().toAnsi(loggerConfiguration.forceANSI ? null : loggerConfiguration.terminal()));
//        }
    }

    @Override
    protected void innerHandleNormalizedLoggingCall(Level level, List<Marker> markers, String messagePattern, Object[] arguments, Throwable t) {

        StringBuilder buf = new StringBuilder(32);
        buf.append('{');

    	AtomicInteger fieldIdx = new AtomicInteger();
		for (String field : loggerConfiguration.layout) {

			if (field.equals("date-time")) {
				if (loggerConfiguration.dateFormatter != null) {
					appendField(buf, field, getFormattedDate(), fieldIdx);
				} else {
					appendField(buf, field, String.valueOf(System.currentTimeMillis() - START_TIME), fieldIdx);
				}
			}
			else if (field.equals("thread-name")) {
				appendField(buf, field, Thread.currentThread().getName(), fieldIdx);
			}
			else if (field.equals("thread-id")) {
				appendField(buf, field, String.valueOf(Thread.currentThread().getId()), fieldIdx);
			}
			else if (field.equals("level")) {
				appendField(buf, field, level.name(), fieldIdx);
			}
			else if (field.equals("short-name")) {
	            if (shortLogName == null) {
					shortLogName = computeShortName();
				}
				appendField(buf, field, shortLogName, fieldIdx);
			}
			else if (field.equals("name")) {
				appendField(buf, field, name, fieldIdx);
			}
			else if (field.equals("message")) {
				String str = MessageFormatter.basicArrayFormat(null, messagePattern, arguments);
				appendField(buf, field, str, fieldIdx);
			}
			else if (field.equals("markers")) {
				if(markers == null) {
					appendField(buf, field, "", fieldIdx);
				} else {
					appendField(buf, field, String.join(",", markers.stream().map(Marker::getName).toList()) , fieldIdx);
				}
			}

		}

		appendField(buf, "pattern", messagePattern, fieldIdx);
		if(arguments != null) {
			for(int i = 0 ; i < arguments.length ; i++) {
				appendField(buf, "arg" + i,  arguments[i], fieldIdx);
			}
		}

        buf.append('}');

        write(buf, t);
    }

	private void appendField(StringBuilder buf, String field, Object value, AtomicInteger fieldIdx) {
		if(buf.length() > 1)
			buf.append(",");
		buf.append("\"");
		buf.append(escape(field));
		buf.append("\":");
		if(value == null)
			buf.append("null");
		else if(value instanceof Boolean b)
			buf.append(b);
		else if(value instanceof Number n)
			buf.append(n);
		else {
			buf.append("\"");
			buf.append(escape(value.toString()));
			buf.append("\"");
		}
		fieldIdx.incrementAndGet();
	}
	
	private String escape(String text) {
		return text.replace("\\", "\\\\").
				replace("\"", "\\\"").
				replace("\n", "\\n").
				replace("\r", "\\r").
				replace("\f", "\\f").
				replace("\t", "\\t").
				replace("\b", "\\b");
	}

}
