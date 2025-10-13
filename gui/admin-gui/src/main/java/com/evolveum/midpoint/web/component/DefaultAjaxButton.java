/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
