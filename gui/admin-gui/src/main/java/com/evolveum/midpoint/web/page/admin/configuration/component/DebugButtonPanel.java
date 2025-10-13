/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;

/**
 * @author lazyman
 */
public class DebugButtonPanel<T> extends BasePanel<T> {

    private static final String ID_EXPORT = "export";
    private static final String ID_DELETE = "delete";

    public DebugButtonPanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        AjaxButton export = new AjaxButton(ID_EXPORT, createStringResource("DebugButtonPanel.button.export")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                exportPerformed(target, DebugButtonPanel.this.getModel());
            }
        };
        add(export);

        AjaxButton delete = new AjaxButton(ID_DELETE, createStringResource("DebugButtonPanel.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deletePerformed(target, DebugButtonPanel.this.getModel());
            }
        };
        add(delete);
    }

    public void deletePerformed(AjaxRequestTarget target, IModel<T> model) {

    }

    public void exportPerformed(AjaxRequestTarget target, IModel<T> model) {

    }
}
