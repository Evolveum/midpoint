/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2011 Peter Prochazka
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
				return onMatch;
			} else {
				return onMismatch;
			}
		}
		return FilterReply.NEUTRAL;
	}

	/**
	 * @param onMatch action to set on success
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
	 * @param onMismatch the onMismatch to set on failure
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
		System.out.println("MDCvalue = mdcValue");
		this.mdcValue = mdcValue;
	}

	/**
	 * @param level the level to breach
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
