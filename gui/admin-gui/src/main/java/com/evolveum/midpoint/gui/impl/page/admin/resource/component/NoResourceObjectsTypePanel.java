/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.web.component.AjaxIconButton;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

/**
 * Panel displayed when no resource object specific values are not yet defined.
 * <p>
 * Renders a simple card layout with a title, subtitle, and optional toolbar buttons.
 * Buttons are dynamically built from the {@link NoResourceObjectDto} model.
 * <p>
 */
public class NoResourceObjectsTypePanel extends BasePanel<NoResourceObjectDto> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PANEL = "panel";
    private static final String ID_TITLE = "title";
    private static final String ID_SUBTITLE = "subtitle";
    private static final String ID_TOOLBAR = "toolbar";
    private static final String ID_TOOLBAR_BUTTON = "toolbar-button";

    public NoResourceObjectsTypePanel(String id, IModel<NoResourceObjectDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer panel = new WebMarkupContainer(ID_PANEL);
        panel.setOutputMarkupId(true);
        add(panel);

        Label titleComponent = createLabelComponent(ID_TITLE, getModelObject().getTitle());
        panel.add(titleComponent);

        Label subtitleComponent = createLabelComponent(ID_SUBTITLE, getModelObject().getSubtitle());
        panel.add(subtitleComponent);

        WebMarkupContainer toolbars = new WebMarkupContainer(ID_TOOLBAR);
        toolbars.setOutputMarkupId(true);
        panel.add(toolbars);

        RepeatingView buttons = new RepeatingView(ID_TOOLBAR_BUTTON);

        NoResourceObjectDto modelObject = getModelObject();
        modelObject.getToolbarButtons().forEach(dto -> buttons.add(createButton(buttons, dto)));
        toolbars.add(buttons);
    }

    /**
     * Creates a label component with common output markup settings.
     *
     * @param id    Component ID
     * @param title The label model (typically a localized string resource)
     * @return Configured {@link Label} instance
     */
    private @NotNull Label createLabelComponent(String id, StringResourceModel title) {
        Label label = new Label(id, title);
        label.setOutputMarkupId(true);
        label.setOutputMarkupPlaceholderTag(true);
        return label;
    }

    /**
     * Creates a dynamic {@link AjaxIconButton} for a toolbar item based on its DTO.
     *
     * @param parent The parent {@link RepeatingView} to attach to
     * @param dto    The button configuration DTO
     * @return Configured {@link AjaxIconButton}
     */
    private @NotNull AjaxIconButton createButton(
            @NotNull RepeatingView parent,
            @NotNull NoResourceObjectDto.ToolbarButtonDto dto) {
        AjaxIconButton button = new AjaxIconButton(parent.newChildId(), dto.getIconCss(), dto.getLabel()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                dto.action(target);
            }
        };
        button.add(AttributeAppender.append("class", dto.getCssClass()));
        button.add(new VisibleBehaviour(dto::isVisible));
        button.setOutputMarkupId(true);
        button.showTitleAsLabel(true);
        return button;
    }

}
