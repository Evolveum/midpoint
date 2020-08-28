/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.Channel;
import com.evolveum.midpoint.web.component.data.LinkedReferencePanel;
import com.evolveum.midpoint.web.component.data.column.LinkIconPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceAcquisitionType;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

public class ProvenanceAcquisitionHeaderPanel extends BasePanel<ProvenanceAcquisitionType> {

    private static final String ID_RESOURCE = "resource";
    private static final String ID_ORIGIN = "origin";
    private static final String ID_CHANNEL = "channel";
    private static final String ID_CHANNEL_ICON = "channelIcon";

    public ProvenanceAcquisitionHeaderPanel(String id, IModel<ProvenanceAcquisitionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        LinkedReferencePanel<ObjectReferenceType> resource = new LinkedReferencePanel<>(ID_RESOURCE, new PropertyModel<>(getModel(), "resourceRef"));
        add(resource);
        resource.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getResourceRef() != null && getModelObject().getResourceRef().getOid() != null));

        LinkedReferencePanel<ObjectReferenceType> origin = new LinkedReferencePanel<>(ID_ORIGIN, new PropertyModel<>(getModel(), "originRef"));
        add(origin);
        origin.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getOriginRef() != null && getModelObject().getOriginRef().getOid() != null));

        ReadOnlyModel<Channel> channelModel = new ReadOnlyModel<>(() -> Channel.findChannel(getModelObject().getChannel()));
        WebMarkupContainer channelIcon = new WebMarkupContainer(ID_CHANNEL_ICON);
        add(channelIcon);
        channelIcon.add(AttributeAppender.replace("class", new PropertyModel<>(channelModel, "iconCssClass")));
        Label channel = new Label(ID_CHANNEL, new StringResourceModel("${resourceKey}", channelModel));
        add(channel);
        channel.add(new VisibleBehaviour(() -> getModelObject() != null
                && (getModelObject().getOriginRef() == null || getModelObject().getOriginRef().getOid() == null)
                && (getModelObject().getResourceRef() == null || getModelObject().getResourceRef().getOid() == null)));

    }
}
