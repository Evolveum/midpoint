/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;

/**
 * Created by honchar
 */
public class LockoutStatusPanel extends Panel {

    private static final long serialVersionUID = 1L;
    private static final String ID_CONTAINER = "container";
    private static final String ID_LABEL = "label";
    private static final String ID_BUTTON = "button";
    private boolean isInitialState = true;
    private LockoutStatusType initialValue;

    public LockoutStatusPanel(String id) {
        this(id, null);
    }

    public LockoutStatusPanel(String id, IModel<LockoutStatusType> model) {
        super(id);
        initialValue = model.getObject(); //TODO: this is wrong, why do we need value in constructor?
        initLayout(model);
    }

    private void initLayout(final IModel<LockoutStatusType> model) {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        Label label = new Label(ID_LABEL, getLabelModel(model));
        container.add(label);

        AjaxButton button = new AjaxButton(ID_BUTTON, getButtonModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (!isInitialState) {
                    model.setObject(initialValue);
                } else {
                    model.setObject(LockoutStatusType.NORMAL);
                }
                isInitialState = !isInitialState;

                target.add(LockoutStatusPanel.this.get(ID_CONTAINER));
            }
        };
        container.add(button);
    }

    private IModel<String> getButtonModel() {
        return () -> {
            String key = isInitialState ? "LockoutStatusPanel.unlockButtonLabel" : "LockoutStatusPanel.undoButtonLabel";

            return getString(key);
        };
    }

    private IModel<String> getLabelModel(IModel<LockoutStatusType> model) {
        return () -> {
            LockoutStatusType object = model != null ? model.getObject() : null;

            String labelValue = object == null ?
                    getString("LockoutStatusType.UNDEFINED") : getString(WebComponentUtil.createEnumResourceKey(object));

            if (!isInitialState) {
                labelValue += " " + getString("LockoutStatusPanel.changesSaving");
            }

            return labelValue;
        };
    }
}
