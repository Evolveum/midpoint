/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init.interpol;

import com.evolveum.midpoint.util.NetworkUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration2.interpol.Lookup;

import java.net.UnknownHostException;

/**
 * TODO consider moving this downwards to make it available for the rest of midPoint (not only to config.xml parsing).
 */
public class HostnameLookup implements Lookup {

    private static final Trace LOGGER = TraceManager.getTrace(HostnameLookup.class);

    public static final String PREFIX = "hostname";

    @Override
    public Object lookup(String variable) {
        try {
            String hostName = NetworkUtil.getLocalHostNameFromOperatingSystem();
            if (hostName == null) {
                LOGGER.error("Couldn't get local host name");
            }
            return hostName;
        } catch (UnknownHostException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get local host name", e);
            return null;
        }
    }
}
