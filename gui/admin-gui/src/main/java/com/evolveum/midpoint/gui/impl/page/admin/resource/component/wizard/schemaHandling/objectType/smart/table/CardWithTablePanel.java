/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public class CardWithTablePanel<T extends Serializable> extends BasePanel<T> implements Popupable {

    private static final String ID_TABLE = "table";
    private static final String ID_CLOSE_BUTTON = "closeButton";
    private static final String ID_FOOTER_BUTTONS = "footerButtons";

    public CardWithTablePanel(String id, IModel<T> model) {
        super(id, model);
        initLayout();

        add(AttributeModifier.append("class", "p-0"));
    }

    private void initLayout() {

        WebMarkupContainer component = createComponent(ID_TABLE);
        component.setOutputMarkupId(true);
        add(component);
    }

    /**
     * Creates a table component to be placed inside the card.
     * This method should be overridden to provide specific table implementation.
     *
     * @param id the markup ID for the table
     * @return a WebMarkupContainer representing the table
     */
    protected WebMarkupContainer createComponent(String id) {
        return new WebMarkupContainer(id);
    }

    protected void noPerformed(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    protected StringResourceModel getCloseButtonModel() {
        return createStringResource("CardWithTablePanel.close");
    }

    protected boolean isCloseButtonVisible() {
        return true;
    }

    @Override
    public @NotNull Component getFooter() {
        Fragment componentsFooter = new Fragment(Popupable.ID_FOOTER, ID_FOOTER_BUTTONS, this);

        AjaxLinkPanel closeButton = new AjaxLinkPanel(ID_CLOSE_BUTTON, getCloseButtonModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                noPerformed(target);
            }
        };

        closeButton.setOutputMarkupId(true);
        closeButton.add(new VisibleBehaviour(this::isCloseButtonVisible));
        componentsFooter.add(closeButton);
        return componentsFooter;
    }

    @Override
    public int getWidth() {
        return 80;
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public IModel<String> getTitle() {
        return null;
    }

    @Override
    public @Nullable Component getTitleComponent() {
        return new Label(Popupable.ID_TITLE, getTitle());
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of(GuiStyleConstants.CLASS_ICON_PREVIEW);
    }
}
