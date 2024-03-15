/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.notification;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import org.apache.wicket.model.StringResourceModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class ComplexPropertyInputPanel<T extends Serializable> extends InputPanel {

    private static final long serialVersionUID = 1L;
    private IModel<T> model;

    public ComplexPropertyInputPanel(String id, IModel<T> model) {
        super(id);

        this.model = model;
    }

    public IModel<T> getModel() {
        return model;
    }

    protected MarkupContainer add(Component child, boolean addUpdatingBehaviour, String label) {
        if (addUpdatingBehaviour) {
            FormComponent fc = null;
            if (child instanceof FormComponent) {
                fc = (FormComponent) child;
            } else if (child instanceof InputPanel) {
                fc = ((InputPanel) child).getBaseFormComponent();
            }

            if (fc != null) {
                fc.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            }
        }

        if (label != null && child instanceof InputPanel) {
            FormComponent fc = ((InputPanel) child).getBaseFormComponent();
            fc.setLabel(new StringResourceModel(label));
        }

        return super.add(child);
    }

    protected MarkupContainer add(Component child, String label) {
        return add(child, true, label);
    }

    protected MarkupContainer add(Component child) {
        return add(child, true, null);
    }
}
