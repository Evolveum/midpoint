/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ItemDefinitionDto;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.TextPanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class ItemDefinitionPanel extends BasePanel<ItemDefinitionDto> implements Popupable {

    public ItemDefinitionPanel(String id, IModel<ItemDefinitionDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        add(new TextPanel<>("displayName", new PropertyModel<>(getModel(), ItemDefinitionDto.F_DISPLAY_NAME)));
        add(new TextPanel<>("displayOrder", new PropertyModel<>(getModel(), ItemDefinitionDto.F_DISPLAY_ORDER)));
        //TODO switch component
        TextPanel<String> isIndexed = new TextPanel<>("indexed", new PropertyModel<>(getModel(), ItemDefinitionDto.F_INDEXED));
        isIndexed.add(new VisibleBehaviour(() -> getModelObject().getOriginalDefinition() instanceof PrismPropertyDefinition<?>));
        add(isIndexed);
        //TODO switch component
        add(new TextPanel<>("minOccurs", new PropertyModel<>(getModel(), ItemDefinitionDto.F_MIN_OCCURS)));
        //TODO switch component
        add(new TextPanel<>("maxOccurs", new PropertyModel<>(getModel(), ItemDefinitionDto.F_MAX_OCCURS)));

        AjaxSubmitButton okButton = new AjaxSubmitButton("save") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                refresh(target);
            }
        };
        add(okButton);
    }

    protected void refresh(AjaxRequestTarget target) {

    }

    @Override
    public int getWidth() {
        return 50;
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
    public IModel<String> getTitle() {
        return createStringResource("Edit Definition");
    }

    @Override
    public Component getContent() {
        return this;
    }
}
