/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.refresh;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * Refreshable page (or component).
 */
public interface Refreshable {

    /**
     * Called on manually requested refresh action.
     * @param target The request target.
     */
    void refresh(AjaxRequestTarget target);

    /**
     * If the refresh is enabled
     */
    boolean isRefreshEnabled();

    /**
     * Current refreshing interval (may depend on page content).
     */
    int getRefreshInterval();
}
