/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;

public class LinkIconPanelStatus extends Panel {

    private static final String ID_LINK = "link";
    private static final String ID_IMAGE = "image";

    public LinkIconPanelStatus(String id, LoadableDetachableModel<RoleAnalysisOperationMode> status) {
        super(id);
        initLayout(status);
    }

    public String getModel(LoadableDetachableModel<RoleAnalysisOperationMode> status) {
        return status.getObject().getDisplayString();
    }

    private void initLayout(LoadableDetachableModel<RoleAnalysisOperationMode> status) {
        setOutputMarkupId(true);

        Label image = new Label(ID_IMAGE);
        image.add(AttributeModifier.replace("class", getModel(status)));
        image.setOutputMarkupId(true);
        AjaxLink<Void> link = new AjaxLink<>(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                RoleAnalysisOperationMode roleAnalysisOperationMode = onClickPerformed(target, status.getObject());
                image.add(AttributeModifier.replace("class", roleAnalysisOperationMode.toggleStatus().getDisplayString()));
                target.add(image);
            }
        };
        link.add(image);
        link.setOutputMarkupId(true);

        add(link);

    }

    protected RoleAnalysisOperationMode onClickPerformed(AjaxRequestTarget target, RoleAnalysisOperationMode roleAnalysisOperationMode) {
        return roleAnalysisOperationMode;
    }
}
