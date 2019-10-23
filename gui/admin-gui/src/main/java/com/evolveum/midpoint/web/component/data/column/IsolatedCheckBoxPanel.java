/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Isolated checkbox - checkbox that is displayed as (visually) stand-alone component.
 * This checkbox is not supposed to have any labels associated with it.
 *
 * For checkbox in forms see com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel
 *
 * @author lazyman
 */
public class IsolatedCheckBoxPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private static final String ID_CHECK = "check";

    public IsolatedCheckBoxPanel(String id, IModel<Boolean> model) {
        this(id, model, new Model<>(true));
    }

    public IsolatedCheckBoxPanel(String id, IModel<Boolean> model, final IModel<Boolean> enabled) {
        super(id);

        AjaxCheckBox check = new AjaxCheckBox(ID_CHECK, model) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                IsolatedCheckBoxPanel.this.onUpdate(target);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                IsolatedCheckBoxPanel.this.updateAjaxAttributes(attributes);
            }
        };
        check.setOutputMarkupId(true);
        check.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return enabled.getObject();
            }
        });

        add(check);
    }

    public AjaxCheckBox getPanelComponent() {
        return (AjaxCheckBox) get(ID_CHECK);
    }

    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
    }

    public void onUpdate(AjaxRequestTarget target) {
    }

    public boolean getValue() {
        Boolean val = getPanelComponent().getModelObject();
        if (val == null) {
            return false;
        }

        return val.booleanValue();
    }
}
