package com.sshtools.slf4jtty;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * An implementation of {@link ILoggerFactory} which always returns
 * {@link TtyLogger} instances.
 * <p>
 * TtyLogger and associated classes are based on SimpleLogger implementation to varying degrees.
 * 
 * @author Ceki G&uuml;lc&uuml;
 */
public class TtyLoggerFactory implements ILoggerFactory {

    ConcurrentMap<String, Logger> loggerMap;

    public TtyLoggerFactory() {
        loggerMap = new ConcurrentHashMap<>();
    }

    /**
     * Return an appropriate {@link SimpleLogger} instance by name.
     */
    public Logger getLogger(String name) {
        Logger simpleLogger = loggerMap.get(name);
        if (simpleLogger != null) {
            return simpleLogger;
        } else {
            Logger newInstance = new TtyLogger(name, TtyLoggerConfiguration.get());
            Logger oldInstance = loggerMap.putIfAbsent(name, newInstance);
            return oldInstance == null ? newInstance : oldInstance;
        }
    }

    /**
     * Clear the internal logger cache.
     *
     * This method is intended to be called by classes (in the same package) for
     * testing purposes. This method is internal. It can be modified, renamed or
     * removed at any time without notice.
     *
     * You are strongly discouraged from calling this method in production code.
     */
    void reset() {
        loggerMap.clear();
    }
}
