/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;

/**
 * @author lazyman
 */
public final class AjaxCallListenerFactory {

    private AjaxCallListenerFactory() {
    }

    /**
     * this listener will disable javascript event propagation from component to parent dom components
     *
     * @return {@link IAjaxCallListener}
     */
    public static IAjaxCallListener stopPropagation() {
        return new AjaxCallListener().onBefore("\nattrs.event.stopPropagation();");
    }
}
