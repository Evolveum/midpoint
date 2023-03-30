/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.web.page.admin.resources.component.TestConnectionResultPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SchemaCapabilityType;

import org.jetbrains.annotations.NotNull;

public abstract class ResourceOperationalButtonsPanel extends AssignmentHolderOperationalButtonsPanel<ResourceType> {

    private static final String DOT_CLASS = ResourceOperationalButtonsPanel.class.getName() + ".";
    private static final String OPERATION_REFRESH_SCHEMA = DOT_CLASS + "refreshSchema";
    private static final String OPERATION_SET_MAINTENANCE = DOT_CLASS + "setMaintenance";
    private static final String OPERATION_SET_MODE = DOT_CLASS + "setSimulationMode";

    private static final String ID_RESOURCE_BUTTONS = "resourceButtons";
    private static final String ID_MODE_BUTTONS = "modeButtons";

    public ResourceOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<ResourceType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        RepeatingView resourceButtons = new RepeatingView(ID_RESOURCE_BUTTONS);
        add(resourceButtons);
        initResourceButtons(resourceButtons);
        RepeatingView modeButtons = new RepeatingView(ID_MODE_BUTTONS);
        add(modeButtons);
        modeButtons.add(new VisibleBehaviour(() -> isEditingObject() && canEdit(getObjectType())));
        initModeButtons(modeButtons);
    }

    private void initResourceButtons(RepeatingView resourceButtons) {
        AjaxIconButton test = new AjaxIconButton(resourceButtons.newChildId(),
                Model.of(GuiStyleConstants.CLASS_TEST_CONNECTION_MENU_ITEM),
                createStringResource("pageResource.button.test")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                testConnectionPerformed(target);
            }

            @Override
            public boolean isVisible() {
                return !WebComponentUtil.isTemplateCategory(getPrismObject().asObjectable());
            }
        };
        test.showTitleAsLabel(true);
        test.add(new VisibleBehaviour(() -> isEditingObject()));
        resourceButtons.add(test);

        AjaxIconButton setMaintenance = new AjaxIconButton(resourceButtons.newChildId(),
                Model.of(GuiStyleConstants.CLASS_ICON_RESOURCE_MAINTENANCE),
                createStringResource("pageResource.button.toggleMaintenance")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                WebComponentUtil.toggleResourceMaintenance(getPrismObject(), OPERATION_SET_MAINTENANCE, target, getPageBase());
                refreshStatus(target);
            }
        };
        setMaintenance.showTitleAsLabel(true);
        setMaintenance.add(new VisibleBehaviour(() -> isEditingObject() && canEdit(getObjectType())));
        resourceButtons.add(setMaintenance);

        AjaxIconButton refreshSchema = new AjaxIconButton(resourceButtons.newChildId(),
                Model.of(GuiStyleConstants.CLASS_ICON_RESOURCE_SCHEMA),
                createStringResource("pageResource.button.refreshSchema")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                WebComponentUtil.refreshResourceSchema(getPrismObject(), OPERATION_REFRESH_SCHEMA, target, getPageBase());
            }

            @Override
            public boolean isVisible() {
                return !WebComponentUtil.isTemplateCategory(getPrismObject().asObjectable());
            }
        };
        refreshSchema.add(new VisibleBehaviour(() -> isVisibleRefresSchemaButton(getObjectType())));
        refreshSchema.showTitleAsLabel(true);
        resourceButtons.add(refreshSchema);
    }

    private void initModeButtons(RepeatingView modeButtons) {
        AjaxIconButton toggleToProduction = new AjaxIconButton(modeButtons.newChildId(),
                Model.of(GuiStyleConstants.CLASS_ICON_TOOGLE),
                createStringResource("OperationalButtonsPanel.button.toggleToProduction")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                WebComponentUtil.switchObjectMode(
                        getPrismObject(), OPERATION_SET_MODE, target, getPageBase(), SchemaConstants.LIFECYCLE_ACTIVE);
                refreshStatus(target);
            }
        };
        toggleToProduction.showTitleAsLabel(true);
        toggleToProduction.add(new VisibleBehaviour(() -> isToggleModeButtonVisible(SchemaConstants.LIFECYCLE_ACTIVE)));
        modeButtons.add(toggleToProduction);

        AjaxIconButton toggleToDevelopment = new AjaxIconButton(modeButtons.newChildId(),
                Model.of(GuiStyleConstants.CLASS_ICON_TOOGLE),
                createStringResource("OperationalButtonsPanel.button.toggleToDevelopment")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                WebComponentUtil.switchObjectMode(
                        getPrismObject(), OPERATION_SET_MODE, target, getPageBase(), SchemaConstants.LIFECYCLE_PROPOSED);
                refreshStatus(target);
            }
        };
        toggleToDevelopment.showTitleAsLabel(true);
        toggleToDevelopment.add(new VisibleBehaviour(() -> isToggleModeButtonVisible(SchemaConstants.LIFECYCLE_PROPOSED)));
        modeButtons.add(toggleToDevelopment);

    }

    private boolean isToggleModeButtonVisible(@NotNull String expectedLifecycleState) {
        String lifecycleState = getPrismObject().asObjectable().getLifecycleState();
        if (StringUtils.isEmpty(lifecycleState)) {
            lifecycleState = SchemaConstants.LIFECYCLE_ACTIVE;
        }

        return !expectedLifecycleState.equals(lifecycleState);
    }

    private void testConnectionPerformed(AjaxRequestTarget target) {
        final PrismObject<ResourceType> dto = getPrismObject();
        if (dto == null || StringUtils.isEmpty(dto.getOid())) {
            error(getString("pageResource.message.oidNotDefined"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        final TestConnectionResultPanel testConnectionPanel =
                new TestConnectionResultPanel(getPageBase().getMainPopupBodyId(),
                        dto.getOid(), getPage()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void okPerformed(AjaxRequestTarget target) {
                        refreshStatus(target);
                    }

                };
        testConnectionPanel.setOutputMarkupId(true);

        //TODO fix
//        getMainPopup().setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
//                refreshStatus(target);
//                return true;
//            }
//        });

        showMainPopup(testConnectionPanel, target);

    }

    private boolean canEdit(ResourceType resource) {
        if (!resource.getAdditionalConnector().isEmpty()) {
            return false;
        }
        return true;
    }

    private boolean isVisibleRefresSchemaButton(ResourceType resource) {
        if (!isEditingObject()) {
            return false;
        }
        if (!resource.getAdditionalConnector().isEmpty()) { // TODO what is this?
            if (resource.getCapabilities() == null) {
                return false;
            }
            if (resource.getCapabilities().getConfigured() != null) {
                SchemaCapabilityType configuredCapability = CapabilityUtil.getCapability(
                        resource.getCapabilities().getConfigured(), SchemaCapabilityType.class);
                if (configuredCapability == null) {
                    return false;
                }
                return !Boolean.FALSE.equals(configuredCapability.isEnabled());
            }
            return false;
        }
        return true;
    }

    protected abstract void refreshStatus(AjaxRequestTarget target);
}
