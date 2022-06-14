/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CatalogItemDetailsPanel extends BasePanel<ObjectType> implements Popupable {

    private static final String ID_BUTTONS = "buttons";

    public CatalogItemDetailsPanel(String id, IModel<ObjectType> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
    }

    @Override
    public Component getFooter() {
        return new Fragment("footer", ID_BUTTONS, this);
    }

    @Override
    public int getWidth() {
        return 905;
    }

    @Override
    public int getHeight() {
        return 1139;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public IModel<String> getTitle() {
        return () -> WebComponentUtil.getTranslatedPolyString(getModelObject().getName());
    }

    @Override
    public Component getComponent() {
        return this;
    }
}
