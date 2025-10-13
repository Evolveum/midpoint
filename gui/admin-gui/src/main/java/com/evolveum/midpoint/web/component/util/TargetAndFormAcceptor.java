/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.util;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;

import java.io.Serializable;

@FunctionalInterface
public interface TargetAndFormAcceptor extends Serializable {
    void accept(AjaxRequestTarget target, Form<?> form);
}
