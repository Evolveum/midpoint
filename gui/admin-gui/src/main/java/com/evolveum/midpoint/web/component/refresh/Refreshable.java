/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.refresh;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * Refreshable page (or component).
 *
 * @author mederly
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
