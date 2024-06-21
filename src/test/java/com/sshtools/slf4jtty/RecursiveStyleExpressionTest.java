package com.sshtools.slf4jtty;

import org.jline.utils.AttributedStringBuilder;
import org.junit.jupiter.api.Test;

public class RecursiveStyleExpressionTest {

	@Test
	public void testWidth() {
		var rex = new RecursiveStyleExpression();
		rex.setMaxLength(80);
		rex.setEllipsis("…");
		var btr = new AttributedStringBuilder();
		rex.evaluate(btr, "@{red This text is in red. We want to make it longer than @{bold 80 characters, so the text is clipped. This should be more than enough");
		System.out.println("br: " + btr.columnLength() + " : " + btr.length());
		System.out.println("br: " + btr.columnLength() + " : " + btr.length());
		System.out.println("t: " + btr.toString());
	}
}
