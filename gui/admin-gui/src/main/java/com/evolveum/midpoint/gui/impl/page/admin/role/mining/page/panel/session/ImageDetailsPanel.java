/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.image.CustomImageResource;
import com.evolveum.midpoint.web.component.dialog.Popupable;

public class ImageDetailsPanel extends BasePanel<CustomImageResource> implements Popupable {

    private static final String ID_IMAGE = "image";
    private static final String ID_COLUMN_HEADER = "column-header";
    private static final String ID_ROW_HEADER = "row-header";

    public ImageDetailsPanel(String id, IModel<CustomImageResource> messageModel) {
        super(id, messageModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        CustomImageResource modelObject = getModelObject();

        IconWithLabel columnHeader = new IconWithLabel(ID_COLUMN_HEADER, createStringResource(modelObject.getColumnTitle())) {
            @Override
            protected String getIconCssClass() {
                return modelObject.getColumnIcon();
            }
        };

        IconWithLabel rowHeader = new IconWithLabel(ID_ROW_HEADER, createStringResource(modelObject.getRowTitle())) {
            @Override
            protected String getIconCssClass() {
                return modelObject.getRowIcon();
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getIconComponentCssStyle() {
                return "transform: rotate(90deg);";
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getLabelComponentCssClass() {
                return "pt-1";
            }
        };

        add(columnHeader);
        add(rowHeader);

        Image image = new Image(ID_IMAGE, modelObject);
        add(image);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 800;
    }

    @Override
    public int getHeight() {
        return 800;
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
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("");
    }

    @Override
    public @NotNull Component getFooter() {
        Component footer = Popupable.super.getFooter();
        footer.add(AttributeModifier.append("class", "border-0"));
        return footer;
    }
}
