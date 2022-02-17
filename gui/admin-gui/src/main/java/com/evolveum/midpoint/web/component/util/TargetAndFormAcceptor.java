/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;

import java.io.Serializable;

@FunctionalInterface
public interface TargetAndFormAcceptor extends Serializable {
    void accept(AjaxRequestTarget target, Form<?> form);
}
