package com.sshtools.slf4jtty;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedHashSet;

import org.jline.style.StyleExpression;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sshtools.slf4jtty.TtyLoggerConfiguration.Format;

public class TtyLoggerTest {
	
	final static String ESC = String.valueOf((char)27);
	
	private final static class LogOutput {
		final  ByteArrayOutputStream buf = new ByteArrayOutputStream();
		final TtyLoggerConfiguration cfg;
		
		LogOutput(String... exclude) {
			TtyConfigurationSet set = new TtyConfigurationSet();
			
			cfg = set.build();
			cfg.format = Format.ANSI;
			cfg.outputChoice = new OutputChoice(new PrintStream(buf));
			cfg.width = 132;
			
			var layout = new LinkedHashSet<>(cfg.layout);
			layout.removeAll(Arrays.asList(exclude));
			cfg.layout = layout;
		}
		
		String bufferText() {
			return new String(buf.toByteArray(), 0, buf.size());
		}
		
		TtyLogger logger(String name) {
			return new TtyLogger(name, cfg);
		}
	}
	
	@Test
	public void testBasic() {
		var lo = new LogOutput("date-time", "thread-name");
		var logger = lo.logger("TEST");
		logger.info("Basic Test 1");
		Assertions.assertEquals(
				"[34m[ℹ️ INFO] [0m [34;1mTEST[22m           [0m [34mBasic Test 1                                                                                             [0m\n",
				lo.bufferText());
	}
	
	@Test
	public void testParameters() {
		var lo = new LogOutput("date-time", "thread-name");
		var logger = lo.logger("TEST");
		logger.info("A Parameterised test. Parm 1: {}, Parm2: {}, Other", "Value 1", "Value 2");
		System.out.println(lo.bufferText());
		
		Assertions.assertEquals(
				"[34m[ℹ️ INFO] [0m [34;1mTEST[22m           [0m [34mA Parameterised test. Parm 1: [1mValue 1[22m, Parm2: [1mValue 2[22m, Other                                             [0m\n",
				lo.bufferText());
	}
	
	@Test
	public void testExceedWidth() {
		var lo = new LogOutput("date-time", "thread-name");
		var logger = lo.logger("TEST");
		logger.info("123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
		System.out.println(lo.bufferText());
		
		Assertions.assertEquals(
				"[[34mℹ️ INFO[0m  ] [34;1mTEST[0m            [34m12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234…[0m\n",
				lo.bufferText());
	}

    @Test
    public void evaluateExpressionWithRecursiveReplacements() {
    	StyleExpression underTest = new StyleExpression();
        AttributedString result = underTest.evaluate("@{underline foo @{fg:cyan bar}}");
        System.out.println(result.toAnsi());
        assert result.equals(new AttributedStringBuilder()
                .append("foo ", AttributedStyle.DEFAULT.underline())
                .append("bar", AttributedStyle.DEFAULT.underline().foreground(AttributedStyle.CYAN))
                .toAttributedString());
    }

    @Test
    public void evaluateExpressionWithRecursiveReplacementsWithTail() {
    	RecursiveStyleExpression underTest = new RecursiveStyleExpression();
        AttributedString result = underTest.evaluate("@{underline foo @{fg:cyan bar} and underline}");
        System.out.println(result.toAnsi());
        assert result.equals(new AttributedStringBuilder()
                .append("foo ", AttributedStyle.DEFAULT.underline())
                .append("bar", AttributedStyle.DEFAULT.underline().foreground(AttributedStyle.CYAN))
                .append(" and underline", AttributedStyle.DEFAULT.underline())
                .toAttributedString());
    }
}
