/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.server;

import org.apache.wicket.Component;

import java.util.Collection;

@FunctionalInterface
public interface RefreshableTabPanel {
    Collection<Component> getComponentsToUpdate();
}
