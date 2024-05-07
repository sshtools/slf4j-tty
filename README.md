# SLF4J TTY

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.sshtools/slf4j-tty/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.sshtools/slf4j-tty)
[![javadoc](https://javadoc.io/badge2/com.sshtools/slf4j-tty/javadoc.svg)](https://javadoc.io/doc/com.sshtools/slf4j-tty)
![JPMS](https://img.shields.io/badge/JPMS-com.sshtools.slf4jtty-purple) 

A colourful logger provider for SLF4J for making your console log output pretty.

*Note this is not intended for logging to a file, although there are options to do so. This provider is focused on console log output'*

Requires any modern terminal that has ANSI color and emoticon support.

## Features

 * Flexible configuration based on INI files.
 * Live configuration changes.
 * Style a log event according to log priority level.
 * Style and decorate any field with color, styles and emoticons.
 * Highlight parameters in parameterized log events.
 
## Usage

SLF4J-TTY is in Maven central, so simply add the dependency to your project. 

```xml
<dependency>
    <groupId>com.sshtools</groupId>
    <artifactId>slf4j-tty</artifactId>
    <version>0.0.1</version>
</dependency>
```

_See badge above for version available on Maven Central. Snapshot versions are in the [Sonatype OSS Snapshot Repository](https://oss.sonatype.org/content/repositories/snapshots/)._

## Configuration

Configuration is achieved using INI format files. The library comes with a pre-defined default, which you can override any and all properties using a resource in your project. 

Further configuration may be achieved by the user by creating either a system-wide or user specific file in the usual location for configuration the operating system in use.

### Defaults

The defaults used are as follows. 

```ini
;
; Log
;

[log]
	enabled = TRUE
	default-level = INFO
	output = SYS_ERR
	log-file = 
	
[output]
	style-as-level = TRUE
	gap  = 1
	ellipsis = …
	width = 0
	fallback-width = 132
	parameter-style = @{bold ${parameter}}
	layout = level, short-name, message, thread-name, date-time 
		
[fields]
	[date-time]
		alignment = RIGHT
		type = DATE_TIME
		format = SHORT
		width = 23
		style = @{faint ${date-time}}
	
	[level]
		alignment = LEFT
		width = 9
		style = [${level}]
	
	[thread-id]
		alignment = LEFT
		width = 10
		style = ${thread-id}
	
	[thread-name]
		alignment = LEFT
		width = 15
		style = [${thread-name}]
	
	[message]
		alignment = LEFT
		width = 0
		style =${message}
	
	[markers]
		alignment = LEFT
		width = 0
		style = ${markers}
	
	[name]
		alignment = LEFT
		width = 0
		style = ${name}
	
	[short-name]
		alignment = LEFT
		width = 15
		style = @{bold ${short-name}}
		

[levels]
	name = Levels
	description = Configuration for each of the levels
	
	[TRACE]
		text = 🔍 TRACE
		style = @{faint ${text}}
		
	[DEBUG]
		text = 🐛 DEBUG
		style = @{italic,fg:cyan ${text}}
		
	[INFO]
		text = ℹ️ INFO
		style = @{fg:blue ${text}}
			
	[WARN]
		text = ⚠️ WARN
		style = @{fg:yellow ${text}}
		
	[ERROR] 
		text = ⛔ ERROR
		style = @{bold,fg:red ${text}}
		
```

### Styles Expressions

SLF4J-TTY uses Jline3's [StyleExpression](https://www.javadoc.io/doc/org.jline/jline/3.23.0/org/jline/style/StyleExpression.html) for it's `style` configuration items. With these, you can style the text for the item using any common support ANSI terminal sequence.

#### Selection Of Styles

 * fg
 * bg
 * blink
 * bold
 * conceal
 * crossed-out
 * crossedout
 * faint
 * hidden
 * inverse
 * inverse-neg
 * inverseneg
 * italic
 * underline