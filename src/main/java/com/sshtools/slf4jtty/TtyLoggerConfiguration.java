package com.sshtools.slf4jtty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.event.Level;
import org.slf4j.helpers.Reporter;

import com.sshtools.jini.INI;
import com.sshtools.jini.INI.Section;
import com.sshtools.jini.config.INISet;
import com.sshtools.slf4jtty.OutputChoice.OutputChoiceType;

/**
 * This class holds configuration values for {@link TtyLogger}. The
 * values are computed at runtime. See {@link TtyLogger} documentation for
 * more information.
 * <p>
 * TtyLogger and associated classes are based on SimpleLogger implementation to varying degrees.
 * 
 * @author Ceki G&uuml;lc&uuml;
 * @author Scott Sanders
 * @author Rod Waldhoff
 * @author Robert Burrell Donkin
 * @author C&eacute;drik LIME
 * @author Brett Smith
 * 
 */
public class TtyLoggerConfiguration {
	
	public enum Alignment {
		LEFT, CENTER, RIGHT
	}

    static int DEFAULT_LOG_LEVEL_DEFAULT = TtyLogger.LOG_LEVEL_INFO;
    int defaultLogLevel = DEFAULT_LOG_LEVEL_DEFAULT;

    DateFormat dateFormatter = null;
    OutputChoice outputChoice = null;

    private INISet iniSet;

    final Map<String, String> fieldStyles = new HashMap<>();
    final Map<String, Alignment> fieldAlignment = new HashMap<>();
    final Map<Level, String> levelStyles = new HashMap<>();
    final Map<Level, String> levelText = new HashMap<>();
    boolean styleAsLevel;
    int gap;
    int width;
    int fallbackWidth;
    Set<String> layout;
    final Map<String, Integer> fieldWidth = new HashMap<>();
    String parameterStyle;
    String ellipsis;
    int ellipsisWidth;

    void init() {
    	
		iniSet = TtyConfiguration.get().build();

        /* Configuration ... */
        
        INI config = iniSet.document();
		Section outputSection = config.section("output");
		styleAsLevel = outputSection.getBoolean("style-as-level");
		gap = outputSection.getInt("gap");
		width = outputSection.getInt("width");
		fallbackWidth = outputSection.getInt("fallback-width");
		parameterStyle = outputSection.get("parameter-style");
		ellipsis = outputSection.get("ellipsis");
		ellipsisWidth = WCWidth.mk_wcswidth(ellipsis);
		layout = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(outputSection.getAll("layout"))));
        
        Section levels = config.section("levels");
        for(Level lvl : Level.values()) {
        	Section levelSection = levels.section(lvl.name());
        	levelStyles.put(lvl, levelSection.get("style"));
        	levelText.put(lvl, levelSection.get("text"));
        }
        
        Section fields = config.section("fields");
        for(Section fieldSection : fields.allSections()) {
        	fieldStyles.put(fieldSection.key(), fieldSection.get("style"));
        	fieldWidth.put(fieldSection.key(), fieldSection.getInt("width"));
        	fieldAlignment.put(fieldSection.key(), fieldSection.getEnum(Alignment.class, "alignment"));
        	if(fieldSection.key().equals("date-time")) {
        		TtyConfiguration.DateTimeType dateType = fieldSection.getEnum(TtyConfiguration.DateTimeType.class, "type");
        		String dateFormatStr = fieldSection.get("format");
        		if(dateFormatStr.equals("SHORT")) {
        			switch(dateType) {
        			case DATE:
            			dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT);
            			break;
        			case TIME:
            			dateFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);
            			break;
            		default:
            			dateFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            			break;
        			}
        		}
        		else if(dateFormatStr.equals("LONG")) {
        			switch(dateType) {
        			case DATE:
            			dateFormatter = DateFormat.getDateInstance(DateFormat.LONG);
            			break;
        			case TIME:
            			dateFormatter = DateFormat.getTimeInstance(DateFormat.LONG);
            			break;
            		default:
            			dateFormatter = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
            			break;
        			}
        		}
        		else if(dateFormatStr.equals("MEDIUM")) {
        			switch(dateType) {
        			case DATE:
            			dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
            			break;
        			case TIME:
            			dateFormatter = DateFormat.getTimeInstance(DateFormat.MEDIUM);
            			break;
            		default:
            			dateFormatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
            			break;
        			}
        		}
        		else if(dateFormatStr.equals("FULL")) {
        			switch(dateType) {
        			case DATE:
            			dateFormatter = DateFormat.getDateInstance(DateFormat.FULL);
            			break;
        			case TIME:
            			dateFormatter = DateFormat.getTimeInstance(DateFormat.FULL);
            			break;
            		default:
            			dateFormatter = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
            			break;
        			}
        		}
        		else if(!dateFormatStr.equals("TIMESTAMP")) {
	        		 try {
	                     dateFormatter = new SimpleDateFormat(dateFormatStr);
	                 } catch (IllegalArgumentException e) {
	                     Reporter.error("Bad date format; will output relative time", e);
	                 }
        		}
        	}
        }

		Section logSection = config.section("log");
		if(logSection.getBoolean("enabled")) {
			defaultLogLevel = stringToLevel(logSection.getEnum(Level.class, "default-level").name());
		}
		else {
			defaultLogLevel = TtyLogger.LOG_LEVEL_OFF;
		}

        String logFile = logSection.get("log-file", "");
        if(logFile.startsWith("~/") || logFile.startsWith("~\\"))
            logFile = System.getProperty("user.home") + logFile.substring(1);

        outputChoice = computeOutputChoice(logFile, logSection.getEnum(OutputChoiceType.class, "output"));

    }

    static int stringToLevel(String levelStr) {
        if ("trace".equalsIgnoreCase(levelStr)) {
            return TtyLogger.LOG_LEVEL_TRACE;
        } else if ("debug".equalsIgnoreCase(levelStr)) {
            return TtyLogger.LOG_LEVEL_DEBUG;
        } else if ("info".equalsIgnoreCase(levelStr)) {
            return TtyLogger.LOG_LEVEL_INFO;
        } else if ("warn".equalsIgnoreCase(levelStr)) {
            return TtyLogger.LOG_LEVEL_WARN;
        } else if ("error".equalsIgnoreCase(levelStr)) {
            return TtyLogger.LOG_LEVEL_ERROR;
        } else if ("off".equalsIgnoreCase(levelStr)) {
            return TtyLogger.LOG_LEVEL_OFF;
        }
        // assume INFO by default
        return TtyLogger.LOG_LEVEL_INFO;
    }

    private static OutputChoice computeOutputChoice(String logFile, OutputChoiceType outputChoiceType) {
    	switch(outputChoiceType) {
    	case FILE:
    		try {
                File logFileObj = new File(logFile);
                if(!logFileObj.getParentFile().exists()) {
                    if(!logFileObj.getParentFile().mkdirs()) {
                        throw new IllegalStateException("Could not create logging directory " + logFileObj.getParent());
                    }
                }
                FileOutputStream fos = new FileOutputStream(logFile);
                PrintStream printStream = new PrintStream(fos);
                return new OutputChoice(printStream);
            } catch (FileNotFoundException e) {
                Reporter.error("Could not open [" + logFile + "]. Defaulting to System.err", e);
                return new OutputChoice(OutputChoiceType.SYS_ERR);
            }
    	default:
    		return new OutputChoice(outputChoiceType);
    	}
    }

}
