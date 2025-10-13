/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.configuration.api;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 *  This is an attempt to provide more typed access to config.xml file content.
 *
 *  EXPERIMENTAL.
 */
@Experimental
public interface SystemConfigurationSection {

    String getJmap();

    String getJhsdb();

    String getLogFile();
}
