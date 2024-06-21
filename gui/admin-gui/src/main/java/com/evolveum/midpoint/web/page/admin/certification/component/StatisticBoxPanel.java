/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.io.Serial;

public class StatisticBoxPanel extends BasePanel<StatisticBoxDto> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_IMAGE = "image";
    private static final String ID_LABEL = "label";
    private static final String ID_DESCRIPTION_ID = "description";
    private static final String ID_RIGHT_SIDE_COMPONENT = "rightSideComponent";

    public StatisticBoxPanel(String id, IModel<StatisticBoxDto> modelObject) {
        super(id, modelObject);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Component image = WebComponentUtil.createPhotoOrDefaultImagePanel(ID_IMAGE, getModelObject().getMessageImageResource(),
                getDefaultImageIcon());
        add(image);

        Label label = new Label(ID_LABEL, getLabelModel());
        add(label);

        Label description = new Label(ID_DESCRIPTION_ID, getDescriptionModel());
        description.setEnabled(false);
        add(description);

        Component rightSideComponent = createRightSideComponent(ID_RIGHT_SIDE_COMPONENT);
        add(rightSideComponent);
    }

    private IconType getDefaultImageIcon() {
        return new IconType()
                .cssClass(getModelObject().getBoxImageCss());
    }

    private IModel<String> getLabelModel() {
        return () -> getModelObject().getBoxTitle();
    }

    private IModel<String> getDescriptionModel() {
        return () -> getModelObject().getBoxDescription();
    }

    private Component createRightSideComponent(String id) {
        return new WebMarkupContainer(id);
    }
}
