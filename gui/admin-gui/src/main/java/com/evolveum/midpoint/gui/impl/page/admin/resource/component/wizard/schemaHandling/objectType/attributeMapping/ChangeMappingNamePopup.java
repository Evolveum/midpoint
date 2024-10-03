/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attributeMapping;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismPropertyPanel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.dialog.SimplePopupable;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChangeMappingNamePopup extends SimplePopupable {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeMappingNamePopup.class);

    private static final String ID_INPUT = "input";
    private static final String ID_APPLY_CHANGES = "applyChanges";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CLOSE = "close";

    private final IModel<PrismContainerValueWrapper<MappingType>> selected;

    private final IModel<PrismPropertyWrapper<String>> itemWrapper;

    private Fragment footer;

    public ChangeMappingNamePopup(
            String id,
            IModel<PrismContainerValueWrapper<MappingType>> selected) {
        super(id,
                500,
                330,
                PageBase.createStringResourceStatic("ChangeMappingNamePopup.title"));
        this.selected = selected;
        this.itemWrapper = PrismPropertyWrapperModel.fromContainerValueWrapper(
                () -> selected.getObject(), MappingType.F_NAME);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        getPageBase().getMainPopup().getDialogComponent().add(AttributeAppender.append("style", "width:500px;"));
        initLayout();

        initFooter();
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        itemWrapper.getObject().setReadOnly(true);
    }

    private void initLayout() {
        ItemPanelSettings settings = new ItemPanelSettingsBuilder().editabilityHandler(wrapper -> true).build();
        itemWrapper.getObject().setReadOnly(false);
        add(new VerticalFormPrismPropertyPanel<>(ID_INPUT, itemWrapper, settings));
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        AjaxIconButton createNewTask = new AjaxIconButton(ID_APPLY_CHANGES,
                () -> "fa fa-arrow",
                createStringResource("ChangeLifecycleSelectedMappingsPopup.applyChanges")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                applyChanges(target);
            }
        };
        createNewTask.showTitleAsLabel(true);
        footer.add(createNewTask);

        footer.add(new AjaxLink<>(ID_CLOSE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                itemWrapper.getObject().setReadOnly(true);
                getPageBase().hideMainPopup(target);
            }
        });

    }

    protected void applyChanges(AjaxRequestTarget target) {
        if (itemWrapper.getObject() == null) {
            getPageBase().hideMainPopup(target);
            return;
        }

        try {
            if (!itemWrapper.getObject().getDelta().isEmpty()) {
                PrismContainerValueWrapper<MappingType> containerValue = selected.getObject();
                try {
                    PrismPropertyWrapper<Object> property = containerValue.findProperty(MappingType.F_MAPPING_ALIAS);
                    if (property.getValues().stream().noneMatch(value -> value != null
                            && StringUtils.equals(
                                (String) value.getRealValue(),
                                getOldMappingName()))) {
                        PrismPropertyValue<Object> newValue = PrismContext.get().itemFactory().createPropertyValue();
                        newValue.setValue(itemWrapper.getObject().getValue().getOldValue().getRealValue());
                        property.add(newValue, getParentPage());
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find name property in " + containerValue);
                }

                WebComponentUtil.showToastForRecordedButUnsavedChanges(target, itemWrapper.getObject());
                itemWrapper.getObject().setReadOnly(true);
                getPageBase().hideMainPopup(target);
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get single value from " + itemWrapper.getObject());
        }
    }

    private String getOldMappingName() {
        try {
            return itemWrapper.getObject().getValue().getOldValue().getRealValue();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get single value from " + itemWrapper.getObject());
        }
        return null;
    }

    @NotNull
    @Override
    public Fragment getFooter() {
        return footer;
    }
}
