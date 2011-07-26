package com.evolveum.midpoint.logging.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;

public class NdcFilteringDailyRollingFileAppender extends DailyRollingFileAppender {

	private Map<String, List<String>> loggerComponents = new HashMap<String, List<String>>();

	private String getNdcSubsystem(LoggingEvent event) {
		//Note: possible logging performance issue, because we have only access to String representation of NDC 
		int whiteCharPos = StringUtils.indexOf(event.getNDC(), " ");
		String ndcSubsystem;
		if (whiteCharPos > -1) {
			ndcSubsystem = StringUtils.substring(event.getNDC(), 0, whiteCharPos);
		} else {
			ndcSubsystem = event.getNDC();
		}

		return ndcSubsystem;
	}

	public void append(LoggingEvent event) {

		String ndcSubsystem = getNdcSubsystem(event);

		if (StringUtils.isNotEmpty(ndcSubsystem)) {
			List<String> subsystems = loggerComponents.get(event.getLogger().getName());
			if (subsystems != null && subsystems.contains(ndcSubsystem)) {
				super.append(event);
			}
		}
	}

	public synchronized void resetLoggerConfiguration() {
		loggerComponents.clear();

	}

	public synchronized void addLoggerConfiguration(List<String> pckgs, List<String> subsystems) {
		// Note: possible problems, if there is more configurations for the same
		// package. Only last configuration in the list will be applied
		for (String pckg : pckgs) {
			loggerComponents.put(pckg, subsystems);
		}

	}

}
