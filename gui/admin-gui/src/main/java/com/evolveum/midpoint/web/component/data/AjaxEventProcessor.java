/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
