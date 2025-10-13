/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
