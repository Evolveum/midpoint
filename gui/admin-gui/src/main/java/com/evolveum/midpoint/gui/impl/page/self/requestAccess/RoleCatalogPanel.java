/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoleCatalogPanel extends BasePanel implements WizardPanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_BACK = "back";

    public RoleCatalogPanel(String id) {
        super(id);

        initLayout();
    }

    @Override
    public IModel<String> getTitle() {
        return () -> getString("RoleCatalogPanel.title");
    }

    private void initLayout() {
        AjaxLink back = new AjaxLink<>(ID_BACK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onBackPerformed(target);
            }
        };
        add(back);
    }

    protected void onBackPerformed(AjaxRequestTarget target) {

    }
}
