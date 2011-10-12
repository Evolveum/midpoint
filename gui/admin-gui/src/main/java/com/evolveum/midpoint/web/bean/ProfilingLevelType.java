package com.evolveum.midpoint.web.bean;

import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;

public enum ProfilingLevelType {

	
	OFF("off"),
	
	INFO("entry/exit"),
	
	DEBUG("elapsed time"),
	
	TRACE("arguments");
	
	private String value;
	
	private ProfilingLevelType(String value){
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public static LoggingLevelType toLoggerLevelType(String profilingLevel){
		if (ProfilingLevelType.OFF.value.equals(profilingLevel)){
			return LoggingLevelType.OFF;
		}
		if (ProfilingLevelType.INFO.value.equals(profilingLevel)){
			return LoggingLevelType.INFO;
		}
		if (ProfilingLevelType.DEBUG.value.equals(profilingLevel)){
			return LoggingLevelType.DEBUG;
		}
		if (ProfilingLevelType.TRACE.value.equals(profilingLevel)){
			return LoggingLevelType.TRACE;
		}
		return null;
		
	}
	
	public static ProfilingLevelType fromLoggerLevelType(LoggingLevelType loggerLevel){
		if (LoggingLevelType.OFF.equals(loggerLevel)){
			return ProfilingLevelType.OFF;
		}
		if (LoggingLevelType.INFO.equals(loggerLevel)){
			return ProfilingLevelType.INFO;
		}
		if (LoggingLevelType.DEBUG.equals(loggerLevel)){
			return ProfilingLevelType.DEBUG;
		}
		if (LoggingLevelType.TRACE.equals(loggerLevel) || LoggingLevelType.ALL.equals(loggerLevel)){
			return ProfilingLevelType.TRACE;
		}
		
		return ProfilingLevelType.OFF;
	}
}
