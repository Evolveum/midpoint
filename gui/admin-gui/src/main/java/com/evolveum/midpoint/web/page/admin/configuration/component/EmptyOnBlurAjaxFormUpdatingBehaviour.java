/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;

public class EmptyOnBlurAjaxFormUpdatingBehaviour  extends AjaxFormComponentUpdatingBehavior {

    public EmptyOnBlurAjaxFormUpdatingBehaviour() {
        super("blur");
    }

    @Override
    protected void onUpdate(AjaxRequestTarget target) {
    }
}
