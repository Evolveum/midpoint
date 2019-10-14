/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 *
 */
public class TestAsyncUpdateCaching extends TestAsyncUpdate {

	@Override
	protected File getResourceFile() {
		return RESOURCE_ASYNC_CACHING_FILE;
	}

	@NotNull
	@Override
	public List<String> getConnectorTypes() {
		return singletonList(ASYNC_CONNECTOR_TYPE);
	}

	@Override
	boolean isCached() {
		return true;
	}
}
