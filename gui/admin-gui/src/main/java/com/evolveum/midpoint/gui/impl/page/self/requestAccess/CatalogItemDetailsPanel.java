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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CatalogItemDetailsPanel extends BasePanel<ObjectType> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_ADD = "add";
    private static final String ID_CLOSE = "close";

    private Fragment footer;

    public CatalogItemDetailsPanel(IModel<ObjectType> model) {
        super(Popupable.ID_CONTENT, model);

        initLayout();
    }

    private void initLayout() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.add(new AjaxLink<>(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target, CatalogItemDetailsPanel.this.getModel());
            }
        });
        footer.add(new AjaxLink<>(ID_CLOSE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                closePerformed(target, CatalogItemDetailsPanel.this.getModel());
            }
        });
    }

    @Override
    public Component getFooter() {
        return footer;
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
    public Component getContent() {
        return this;
    }

    protected void addPerformed(AjaxRequestTarget target, IModel<ObjectType> model) {

    }

    protected void closePerformed(AjaxRequestTarget target, IModel<ObjectType> model) {

    }
}
