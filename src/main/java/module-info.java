
import org.slf4j.spi.SLF4JServiceProvider;

import com.sshtools.slf4jtty.TtyServiceProvider;

open module com.sshtools.slf4jtty {

    exports com.sshtools.slf4jtty;
    requires transitive org.slf4j;
    requires transitive java.logging;
	requires transitive com.sshtools.jini.config;
	requires transitive org.jline.terminal;
	requires transitive org.jline.style;
	requires static uk.co.bithatch.nativeimage.annotations;

    provides SLF4JServiceProvider with TtyServiceProvider;
}