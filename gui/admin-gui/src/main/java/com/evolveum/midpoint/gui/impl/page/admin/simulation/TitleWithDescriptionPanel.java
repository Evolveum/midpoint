/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TitleWithDescriptionPanel extends BasePanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";

    private IModel<String> description;

    public TitleWithDescriptionPanel(String id, IModel<String> title, IModel<String> description) {
        super(id, title);

        this.description = description;

        initLayout();
    }

    private void initLayout() {
        AjaxLink link = new AjaxLink<>(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        add(link);

        Label title = new Label(ID_TITLE, getModel());
        link.add(title);

        Label description = new Label(ID_DESCRIPTION, this.description);
        description.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(this.description.getObject())));
        link.add(description);
    }
}
