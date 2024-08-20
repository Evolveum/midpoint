/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.io.Serial;

public class StatisticBoxPanel<T> extends BasePanel<StatisticBoxDto<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_IMAGE = "image";
    private static final String ID_LABEL = "label";
    private static final String ID_DESCRIPTION_ID = "description";
    private static final String ID_RIGHT_SIDE_COMPONENT = "rightSideComponent";

    public StatisticBoxPanel(String id, IModel<StatisticBoxDto<T>> modelObject) {
        super(id, modelObject);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        StatisticBoxDto<T> statisticObject = getModelObject();

        Component image = WebComponentUtil.createPhotoOrDefaultImagePanel(ID_IMAGE, statisticObject.getMessageImageResource(),
                getDefaultImageIcon(statisticObject));
        image.add(AttributeAppender.append("style", "font-size: 40px;"));
        add(image);

        Label label = new Label(ID_LABEL, getLabelModel(statisticObject));
        add(label);

        Label description = new Label(ID_DESCRIPTION_ID, getDescriptionModel(statisticObject));
        description.setEnabled(false);
        add(description);

        Component rightSideComponent = createRightSideComponent(ID_RIGHT_SIDE_COMPONENT, statisticObject);
        add(rightSideComponent);
    }

    private IconType getDefaultImageIcon(StatisticBoxDto<T> statisticObject) {
        return new IconType()
                .cssClass(statisticObject.getBoxImageCss());
    }

    private IModel<String> getLabelModel(StatisticBoxDto<T> statisticObject) {
        return statisticObject::getBoxTitle;
    }

    private IModel<String> getDescriptionModel(StatisticBoxDto<T> statisticObject) {
        return statisticObject::getBoxDescription;
    }

    protected Component createRightSideComponent(String id, StatisticBoxDto<T> statisticObject) {
        return new WebMarkupContainer(id);
    }
}
