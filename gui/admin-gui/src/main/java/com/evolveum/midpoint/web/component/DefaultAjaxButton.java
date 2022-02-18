/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.web.component.util.TargetAcceptor;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

public class DefaultAjaxButton extends AjaxButton {

    private final TargetAcceptor onClick;

    public DefaultAjaxButton(String id, IModel<String> label, TargetAcceptor onClick) {
        super(id, label);
        this.onClick = onClick;
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        onClick.accept(target);
    }
}
