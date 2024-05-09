package com.sshtools.slf4jtty;

import static java.util.Objects.requireNonNull;

import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.jline.style.NopStyleSource;
import org.jline.style.StyleExpression;
import org.jline.style.StyleResolver;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * An alternative version of {@link StyleExpression} that supports nested 
 * style expressions fully. 
 * <p>
 * For example, <code>@{underline foo @{fg:cyan bar} and just underline}</code> would
 * not produce the intuitive results, an underlined word of default, followed by a cyan underlined word, and
 * the another few words with default color and underline again.
 * <p>
 * It achieves this with a simple parser that stacks the styles as they are encountered
 * within enclosing '{' and '}' characters. When the scope is closed, the last style is popped
 * from the stack and the entire {@link AttributedStyle} built again. The actual string built
 * will use the comma separated format supported by {@link StyleResolver}.
 */
public class RecursiveStyleExpression {
	private final StyleResolver resolver;
	
	private int maxLength = 0;
	private String ellipsis = "..";

    public RecursiveStyleExpression() {
        this(new StyleResolver(new NopStyleSource(), ""));
    }

    public RecursiveStyleExpression(final StyleResolver resolver) {
        this.resolver = requireNonNull(resolver);
    }

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;		
	}

	public String getEllipsis() {
		return ellipsis;
	}

	public void setEllipsis(String ellipsis) {
		this.ellipsis = ellipsis;
	}

	/**
	 * @return the maxLength
	 */
	public int getMaxLength() {
		return maxLength;
	}

	/**
     * Evaluate expression and append to buffer.
     *
     * @param buff the buffer to append to
     * @param expression the expression to evaluate
     */
    public void evaluate(final AttributedStringBuilder buff, final String expression) {
        requireNonNull(buff);
        requireNonNull(expression);
        
        // 
        var chars = expression.toCharArray();
        
        @Deprecated /* Maybe */
        var stack = new Stack<String>();

        var go = new AtomicBoolean(true);
        var cidx = new AtomicInteger();
        var introducer = 0;
        var escape = false;
        var maybeEllipsis = maxLength == 0 || ellipsis == null || ellipsis.length() == 0 ? 0 : maxLength - ellipsis.length();
        var tail = new StringBuffer();
        StringBuilder styleName = null;
        
        Consumer<Character> appender = (ch) -> {
    		var cix= cidx.getAndIncrement();
        	if(maxLength == 0) {
        		buff.append(ch);
        	}
        	else {
	        	if(maybeEllipsis != 0 && cix >= maybeEllipsis) {
	        		tail.append(ch);
	        	}
	        	else {
	        		buff.append(ch);
	        	}
	        	if(cix == maxLength) {
	        		buff.append(ellipsis);
	        		tail.setLength(0);
	        		go.set(false);
	        		buff.style(AttributedStyle.DEFAULT);
	        	}
        	}
        };
        
        for(int i = 0 ; i < chars.length && go.get(); i++) {
        	var ch = chars[i];
        	if(styleName != null) {
        		if(ch == ' ' && styleName.length() > 0) {
                    var styleStr = styleName.toString();
					var style = resolver.resolve(styleStr);
                    stack.push(styleStr);
                    buff.style(style);
        			styleName = null;
        		}
        		else if(ch == '}') {
        			/* Null content, just ignore */
        			styleName = null;
        		} else if(ch != ' ') {
        			styleName.append(ch);
        		}
        	}
        	else if(escape) {
        		appender.accept(ch);
        		escape = false;
        	}
        	else {
	        	if(ch == '\\') {
	        		escape = true;
	        	}
	        	else if(ch == '@' && introducer == 0) {
	        		introducer++;
	        	}
	        	else if(ch == '{' && introducer == 1) {
	        		styleName = new StringBuilder();
	        		if(!stack.isEmpty()) {
	        			styleName.append(String.join(",", stack));
	        			styleName.append(",");
	        		}
	        		introducer = 0;
	        	}
	        	else if(ch == '}' && !stack.isEmpty()) {
	        		stack.pop();
	        		if(!stack.isEmpty()) {
	                    var styleStr = String.join(",", stack);
						var style = resolver.resolve(styleStr);
	                    stack.push(styleStr);
	                    buff.style(style);
	        		}
	        	}
	        	else {
	        		introducer = 0;
	        		appender.accept(ch);
	        	}
        	}
        }
        
        if(tail.length() > 0) {
        	buff.append(tail.toString());
        }
    }

    /**
     * Evaluate expression.
     *
     * @param expression the expression to evaluate
     * @return the result string
     */
    public AttributedString evaluate(final String expression) {
        AttributedStringBuilder buff = new AttributedStringBuilder();
        evaluate(buff, expression);
        return buff.toAttributedString();
    }
}
