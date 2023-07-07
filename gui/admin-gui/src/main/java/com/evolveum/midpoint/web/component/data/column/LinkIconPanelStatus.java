/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;

public class LinkIconPanelStatus extends Panel {

    private static final String ID_LINK = "link";
    private static final String ID_IMAGE = "image";

    public LinkIconPanelStatus(String id, LoadableDetachableModel<ClusterObjectUtils.Status> status) {
        super(id);
        initLayout(status);
    }

    public String getModel(LoadableDetachableModel<ClusterObjectUtils.Status> status) {
        return status.getObject().getDisplayString();
    }

    private void initLayout(LoadableDetachableModel<ClusterObjectUtils.Status> status) {
        setOutputMarkupId(true);

        Label image = new Label(ID_IMAGE);
        image.add(AttributeModifier.replace("class", getModel(status)));
        image.setOutputMarkupId(true);
        AjaxLink<Void> link = new AjaxLink<>(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                ClusterObjectUtils.Status status = onClickPerformed(target, null);

                if (status.equals(ClusterObjectUtils.Status.NEUTRAL)) {
                    image.add(AttributeModifier.replace("class", ClusterObjectUtils.Status.ADD.getDisplayString()));
                } else if (status.equals(ClusterObjectUtils.Status.ADD)) {
                    image.add(AttributeModifier.replace("class", ClusterObjectUtils.Status.REMOVE.getDisplayString()));
                } else if (status.equals(ClusterObjectUtils.Status.REMOVE)) {
                    image.add(AttributeModifier.replace("class", ClusterObjectUtils.Status.NEUTRAL.getDisplayString()));
                }
                target.add(image);
            }
        };
        link.add(image);
        link.setOutputMarkupId(true);

        add(link);

    }


    protected ClusterObjectUtils.Status onClickPerformed(AjaxRequestTarget target, ClusterObjectUtils.Status status) {
        return status;
    }
}
