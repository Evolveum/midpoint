package com.evolveum.midpoint.logging.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;

public class NdcFilteringRollingFileAppender extends RollingFileAppender {

	public void append(LoggingEvent event) {
		//TODO: from where to get the filtering configuration
		if (StringUtils.startsWith(event.getNDC(), "repository")) {
			super.append(event);
		}
	}

}
