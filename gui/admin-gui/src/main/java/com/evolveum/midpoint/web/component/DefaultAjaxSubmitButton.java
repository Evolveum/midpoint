/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.util.TargetAndFormAcceptor;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

public class DefaultAjaxSubmitButton extends AjaxSubmitButton {

    private final PageBase pageBase;
    private final TargetAndFormAcceptor onSubmit;

    public DefaultAjaxSubmitButton(String id, IModel<String> label, PageBase pageBase, TargetAndFormAcceptor onSubmit) {
        super(id, label);
        this.pageBase = pageBase;
        this.onSubmit = onSubmit;
    }

    @Override
    protected void onError(AjaxRequestTarget target) {
        target.add(pageBase.getFeedbackPanel());
    }

    @Override
    protected void onSubmit(AjaxRequestTarget target) {
        onSubmit.accept(target, getForm());
    }
}
