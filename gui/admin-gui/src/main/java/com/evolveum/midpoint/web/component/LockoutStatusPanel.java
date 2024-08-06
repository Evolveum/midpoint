/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;

import java.io.Serial;

public class LockoutStatusPanel extends Panel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";
    private static final String ID_LABEL = "label";
    private static final String ID_BUTTON = "button";
    private boolean resetToInitialState = false;
    private LockoutStatusType initialValue;

    public LockoutStatusPanel(String id) {
        this(id, null);
    }

    public LockoutStatusPanel(String id, IModel<LockoutStatusType> model) {
        super(id);
        initLayout(model);
    }

    private void initLayout(final IModel<LockoutStatusType> model) {
        initialValue = model.getObject();

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        Label label = new Label(ID_LABEL, getLabelModel(model));
        container.add(label);

        AjaxButton button = new AjaxButton(ID_BUTTON, getButtonModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (resetToInitialState) {
                    model.setObject(initialValue);
                } else {
                    model.setObject(LockoutStatusType.NORMAL);
                }
                lockoutStatusResetPerformed(resetToInitialState);
                resetToInitialState = !resetToInitialState;

                reloadComponents(target);
            }
        };
        container.add(button);
    }

    protected void lockoutStatusResetPerformed(boolean resetToNormalState) {
        //to be overridden
    }

    //todo ugly hack to fix 9856: when lockout status is reset to Normal, also reset lockout expiration timestamp
    private void reloadComponents(AjaxRequestTarget target) {
        PrismContainerPanel<?,?> containerPanel = findParent(PrismContainerPanel.class);
        if (containerPanel != null) {
            containerPanel.visitChildren(FormComponent.class, (formComponent, object) -> {
                target.add(formComponent);
            });
        }
        target.add(LockoutStatusPanel.this.get(ID_CONTAINER));
    }

    private IModel<String> getButtonModel() {
        return () -> {
            String key = resetToInitialState ? "LockoutStatusPanel.undoButtonLabel" : "LockoutStatusPanel.unlockButtonLabel";

            return getString(key);
        };
    }

    private IModel<String> getLabelModel(IModel<LockoutStatusType> model) {
        return () -> {
            LockoutStatusType object = model != null ? model.getObject() : null;

            String labelValue = object == null ?
                    getString("LockoutStatusType.UNDEFINED") : getString(WebComponentUtil.createEnumResourceKey(object));

            if (resetToInitialState) {
                labelValue += " " + getString("LockoutStatusPanel.changesSaving");
            }

            return labelValue;
        };
    }
}
