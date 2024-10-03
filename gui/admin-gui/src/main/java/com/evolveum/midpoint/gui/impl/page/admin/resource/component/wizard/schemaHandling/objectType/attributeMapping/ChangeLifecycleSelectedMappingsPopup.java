/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attributeMapping;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.LifecycleStateFormPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismPropertyHeaderPanel;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.dialog.SimplePopupable;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChangeLifecycleSelectedMappingsPopup extends SimplePopupable {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeLifecycleSelectedMappingsPopup.class);

    private static final String ID_HEADER = "header";
    private static final String ID_INPUT = "input";
    private static final String ID_APPLY_CHANGES = "applyChanges";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CLOSE = "close";

    private final IModel<List<PrismContainerValueWrapper<MappingType>>> selected;

    private final IModel<PrismPropertyWrapper<String>> firstWrapper;

    private Fragment footer;

    public ChangeLifecycleSelectedMappingsPopup(
            String id,
            IModel<List<PrismContainerValueWrapper<MappingType>>> selected) {
        super(id,
                330,
                330,
                PageBase.createStringResourceStatic("ChangeLifecycleSelectedMappingsPopup.title"));
        this.selected = selected;
        this.firstWrapper = PrismPropertyWrapperModel.fromContainerValueWrapper(
                () -> selected.getObject().get(0), MappingType.F_LIFECYCLE_STATE);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        getPageBase().getMainPopup().getDialogComponent().add(AttributeAppender.append("style", "width:330px;"));
        initLayout();

        initFooter();
    }

    private void initLayout() {
        add(new VerticalFormPrismPropertyHeaderPanel<>(ID_HEADER, firstWrapper));

        add(new LifecycleStateFormPanel(ID_INPUT, firstWrapper));
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
                getPageBase().hideMainPopup(target);
            }
        });

    }

    protected void applyChanges(AjaxRequestTarget target) {
        if (selected.getObject().size() == 1) {
            getPageBase().hideMainPopup(target);
            return;
        }
        try {
            String selectedValue = firstWrapper.getObject().getValue().getRealValue();

            selected.getObject().forEach(containerValue -> {
                try {
                    PrismPropertyWrapper<Object> property = containerValue.findProperty(MappingType.F_LIFECYCLE_STATE);
                    property.getValue().setRealValue(selectedValue);
                    WebComponentUtil.showToastForRecordedButUnsavedChanges(target, property);
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find lifecycle state property in " + containerValue);
                }
            });

            getPageBase().hideMainPopup(target);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get single value from " + firstWrapper.getObject());
        }
    }

    @NotNull
    @Override
    public Fragment getFooter() {
        return footer;
    }
}
