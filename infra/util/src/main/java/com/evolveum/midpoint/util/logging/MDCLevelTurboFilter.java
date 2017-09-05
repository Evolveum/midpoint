/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.util.logging;

import org.slf4j.MDC;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * This class allow output for given MDC value and given level
 * implments logback turbofilter feature
 * <p>
 * If given value of MDC is found and also level of message reach given level
 * then onMatch action is done 
 * else onMissmatch actionis done
 * 
 * <p>
 * Action values:
 * ACCEPT - bypass basic selection rule and follow processing
 * NEUTRAL - follow processing
 * DENY - stop processing
 * <p>
 * Level values:OFF,ERROR,WARN,INFO,DEBUG,TRACE
 *  
 * @author mamut
 *
 */
public class MDCLevelTurboFilter extends TurboFilter {

	private FilterReply onMatch = FilterReply.ACCEPT;
	private FilterReply onMismatch = FilterReply.NEUTRAL;
	private String mdcKey;
	private String mdcValue;
	private Level level = Level.OFF;

	@Override
	public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {

		if (null == mdcKey || null == mdcValue) {
			return FilterReply.NEUTRAL;
		}
		//First compare levels
		if (level.isGreaterOrEqual(borderLevel())) {
			//Second test MDCvalue match current MDC->key => value 
			if (mdcValue.equals(MDC.get(mdcKey))) {
				//if midpoint clas then process
				if (logger.getName().contains("com.evolveum.midpoint")) {
					return onMatch;
				// if PROFILING then skip
				} else if ("PROFILING".equals(logger.getName())) {
					return FilterReply.NEUTRAL;
				// if external class then move to TRACE 
				} else {
					if (level.isGreaterOrEqual(Level.DEBUG)) {
						level = Level.TRACE;
					}
				}
				
			} else {
				return onMismatch;
			}
		}
		return FilterReply.NEUTRAL;
	}

	/**
	 * @param action the action to set on success
	 */
	public void setOnMatch(String action) {
		if ("NEUTRAL".equals(action)) {
			this.onMatch = FilterReply.NEUTRAL;
		} else if ("ACCEPT".equals(action)) {
			this.onMatch = FilterReply.ACCEPT;
		} else {
			this.onMatch = FilterReply.DENY;
		}
	}

	/**
	 * @param action the onMismatch to set on failure
	 */
	public void setOnMismatch(String action) {
		if ("NEUTRAL".equals(action)) {
			this.onMismatch = FilterReply.NEUTRAL;
		} else if ("ACCEPT".equals(action)) {
			this.onMismatch = FilterReply.ACCEPT;
		} else {
			this.onMismatch = FilterReply.DENY;
		}
	}

	/**
	 * @param mdcKey the mdcKey to watch
	 */
	public void setMDCKey(String mdcKey) {
		System.out.println("MDCkey = " + mdcKey);
		this.mdcKey = mdcKey;
	}

	/**
	 * @param mdcValue the mdcValue to match with MDCkey
	 */
	public void setMDCValue(String mdcValue) {
		System.out.println("MDCvalue = " + mdcValue);
		this.mdcValue = mdcValue;
	}

	/**
	 * @param loggingLevel the level to breach
	 */
	public void setLevel(String loggingLevel) {
		String level = loggingLevel.toUpperCase();
		if ("OFF".equals(level)) {
			this.level = Level.OFF;
		} else if ("ERROR".equals(level)) {
			this.level = Level.ERROR;
		} else if ("WARN".equals(level)) {
			this.level = Level.WARN;
		} else if ("INFO".equals(level)) {
			this.level = Level.INFO;
		} else if ("DEBUG".equals(level)) {
			this.level = Level.DEBUG;
		} else if ("TRACE".equals(level)) {
			this.level = Level.TRACE;
		} else {
			this.level = Level.ALL;
		}
	}

	/**
	 * @return the level
	 */
	private Level borderLevel() {
		return level;
	}

	@Override
	public void start() {
	}

}
