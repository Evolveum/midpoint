/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.common;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * System-wide clock. This class provides current time. By default is only proxies the usual system
 * current time functions. But it may be explicitly manipulated to artificially shift the time. This
 * of little value for a running system but it is really useful in the tests. Especially tests that
 * test time-based behavior. Using the Clock avoids changing the actual system time (or JVM's perception
 * of time) therefore the tests are easier to use and usual tools still make sense (e.g. log record timestamps
 * are correct).
 *
 * @author Radovan Semancik
 */
public class Clock {

	private static final Trace LOGGER = TraceManager.getTrace(Clock.class);

	private Long override = null;
	// TODO: more sophisticated functions

	public long currentTimeMillis() {
		if (override != null) {
			return override;
		}
		return System.currentTimeMillis();
	}

	public XMLGregorianCalendar currentTimeXMLGregorianCalendar() {
		long millis = currentTimeMillis();
		return XmlTypeConverter.createXMLGregorianCalendar(millis);
	}

	public boolean isPast(long date) {
		return currentTimeMillis() > date;
	}

	public boolean isPast(XMLGregorianCalendar date) {
		return isPast(XmlTypeConverter.toMillis(date));
	}


	public boolean isFuture(long date) {
		return currentTimeMillis() < date;
	}

	public boolean isFuture(XMLGregorianCalendar date) {
		return isFuture(XmlTypeConverter.toMillis(date));
	}

	public void override(long overrideTimestamp) {
		LOGGER.info("Clock override: {}", override);
		this.override = overrideTimestamp;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Clock current time: {}", currentTimeXMLGregorianCalendar());
		}
	}

	public void override(XMLGregorianCalendar overrideTimestamp) {
		override(XmlTypeConverter.toMillis(overrideTimestamp));
	}

	public void overrideDuration(String durationString) {
		overrideDuration(XmlTypeConverter.createDuration(durationString));
	}

	public void overrideDuration(Duration duration) {
		XMLGregorianCalendar time = currentTimeXMLGregorianCalendar();
		time.add(duration);
		override(time);
	}

	public void resetOverride() {
		LOGGER.info("Clock override reset");
		this.override = null;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Clock current time: {}", currentTimeXMLGregorianCalendar());
		}
	}

}
