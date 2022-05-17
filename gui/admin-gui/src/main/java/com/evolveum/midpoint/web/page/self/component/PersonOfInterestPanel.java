/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.self.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PersonOfInterestPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_NEXT = "next";

    public PersonOfInterestPanel(String id) {
        super(id);

        initLayout();
    }

    private void initLayout() {
        AjaxLink next = new AjaxLink<>(ID_NEXT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onNextPerformed(target);
            }
        };
        add(next);
    }

    protected void onNextPerformed(AjaxRequestTarget target) {

    }
}
