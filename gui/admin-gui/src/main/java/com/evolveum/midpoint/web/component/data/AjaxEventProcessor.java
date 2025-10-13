/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.io.Serializable;

/**
 * Created by Viliam Repan (lazyman).
 */
@FunctionalInterface
public interface AjaxEventProcessor extends Serializable {

    void onEventPerformed(AjaxRequestTarget target);
}
