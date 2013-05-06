/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.common;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

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
	
	private Long override = null;
	// TODO: more sophisticated functions

	public long currentTimeMillis() {
		if (override != null) {
			return override;
		}
		return System.currentTimeMillis();
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
		this.override = overrideTimestamp;
	}
	
	public void override(XMLGregorianCalendar overrideTimestamp) {
		override(XmlTypeConverter.toMillis(overrideTimestamp));
	}

}
