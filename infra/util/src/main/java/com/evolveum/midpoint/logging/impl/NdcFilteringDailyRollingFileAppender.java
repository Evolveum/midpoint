package com.evolveum.midpoint.logging.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class NdcFilteringDailyRollingFileAppender extends DailyRollingFileAppender {

	private Map<String, List<String>> loggerComponents = new HashMap<String, List<String>>();

	public NdcFilteringDailyRollingFileAppender() {
	}

	public NdcFilteringDailyRollingFileAppender(Layout layout, String filename, String datePattern) throws IOException {
		super(layout, filename, datePattern);
	}

	private String getNdcSubsystem(LoggingEvent event) {
		//Note: possible logging performance issue, because we have only access to String representation of NDC 
		int whiteCharPos = StringUtils.lastIndexOf(event.getNDC(), " ");
		String ndcSubsystem;
		if (whiteCharPos > -1) {
			ndcSubsystem = StringUtils.substring(event.getNDC(), whiteCharPos + 1);
		} else {
			ndcSubsystem = event.getNDC();
		}

		return ndcSubsystem;
	}

	private List<String> getSubsystems(String loggerName) {
		//Log4j loggers are in hierarchy, so we have to search it from bottom to top
		String searchName = loggerName;
		int dotPos = -1;
		List<String> subsystems = null;
		while ((dotPos = StringUtils.lastIndexOf(searchName, ".")) > -1) {
			subsystems = loggerComponents.get(searchName);
			if (subsystems != null) {
				return subsystems;
			}
			searchName = StringUtils.substring(searchName, 0, dotPos);
		}
	
		return subsystems = loggerComponents.get(searchName);
		
	}
	
	public void append(LoggingEvent event) {

		String ndcSubsystem = getNdcSubsystem(event);

		if (StringUtils.isNotEmpty(ndcSubsystem)) {
 			List<String> subsystems = getSubsystems(event.getLogger().getName());
			if (subsystems != null && subsystems.contains(ndcSubsystem)) {
				super.append(event);
			}
		} else {
			//if ndc is not set, then act as regular appender
			super.append(event);
		}
	}

	public synchronized void addLoggerConfiguration(List<String> pckgs, List<String> subsystems) {
		// Note: possible problems, if there is more configurations for the same
		// package. Only last configuration in the list will be applied
		for (String pckg : pckgs) {
			loggerComponents.put(pckg, subsystems);
		}
	}


}
