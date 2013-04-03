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
 */

package com.evolveum.midpoint.util.logging;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evolveum.midpoint.util.logging.impl.TraceImpl;

/**
 * Factory for trace instances.
 */
public class TraceManager {

    private static Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TraceManager.class);

    public static Trace getTrace(Class clazz) {
        Logger logger = org.slf4j.LoggerFactory.getLogger(clazz);
        return new TraceImpl(logger);
    }
    
    public static Trace getTrace(String loggerName) {
        Logger logger = org.slf4j.LoggerFactory.getLogger(loggerName);
        return new TraceImpl(logger);
    }
    
    public static ILoggerFactory getILoggerFactory() {
    	return LoggerFactory.getILoggerFactory();
    }
}
