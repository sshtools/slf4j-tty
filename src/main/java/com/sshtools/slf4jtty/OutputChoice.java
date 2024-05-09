package com.sshtools.slf4jtty;

import java.io.PrintStream;

import org.jline.terminal.Terminal;

/**
 * This class encapsulates the user's choice of output target.
 * <p>
 * TtyLogger and associated classes are based on SimpleLogger implementation to varying degrees.
 * 
 * @author Ceki G&uuml;lc&uuml;
 *
 */
public class OutputChoice {

    public enum OutputChoiceType {
        TERMINAL, SYS_OUT, CACHED_SYS_OUT, SYS_ERR, CACHED_SYS_ERR, FILE;
    }

    final OutputChoiceType outputChoiceType;
    final PrintStream targetPrintStream;

    OutputChoice(OutputChoiceType outputChoiceType) {
        if (outputChoiceType == OutputChoiceType.FILE) {
            throw new IllegalArgumentException();
        }
        this.outputChoiceType = outputChoiceType;
        if (outputChoiceType == OutputChoiceType.CACHED_SYS_OUT) {
            this.targetPrintStream = System.out;
        } else if (outputChoiceType == OutputChoiceType.CACHED_SYS_ERR) {
            this.targetPrintStream = System.err;
        } else {
            this.targetPrintStream = null;
        }
    }

    OutputChoice(Terminal terminal) {
        this.outputChoiceType = OutputChoiceType.TERMINAL;
        this.targetPrintStream = new PrintStream(terminal.output());
    }

    OutputChoice(PrintStream printStream) {
        this.outputChoiceType = OutputChoiceType.FILE;
        this.targetPrintStream = printStream;
    }

    PrintStream getTargetPrintStream() {
        switch (outputChoiceType) {
        case SYS_OUT:
            return System.out;
        case SYS_ERR:
            return System.err;
        case CACHED_SYS_ERR:
        case CACHED_SYS_OUT:
        case FILE:
        case TERMINAL:
            return targetPrintStream;
        default:
            throw new IllegalArgumentException();
        }

    }

}
