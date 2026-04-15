/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import com.evolveum.midpoint.web.component.AjaxButton;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

public abstract class ResourceCreationPopup extends BasePanel implements Popupable {

    private static final String ID_WITH_CONFIGURATION = "withConfiguration";
    private static final String ID_WITHOUT_CONFIGURATION = "withoutConfiguration";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CLOSE = "close";

    private Fragment footer;

    public ResourceCreationPopup(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        initFooter();
    }

    private void initLayout() {
        AjaxButton withConfiguration = new AjaxButton(ID_WITH_CONFIGURATION) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                createNewResource(true);
            }
        };
        withConfiguration.setOutputMarkupId(true);
        add(withConfiguration);

        AjaxButton withoutConfiguration = new AjaxButton(ID_WITHOUT_CONFIGURATION) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                createNewResource(false);
            }
        };
        withoutConfiguration.setOutputMarkupId(true);
        add(withoutConfiguration);
    }

    protected abstract void createNewResource(boolean useConfiguration);

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.add(new AjaxLink<>(ID_CLOSE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        });

    }

    @Override
    public int getWidth() {
        return 500;
    }

    @Override
    public int getHeight() {
        return 400;
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
        return null;
    }

    @Override
    public Component getContent() {
        return ResourceCreationPopup.this;
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }
}
