/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.resources.component.TestConnectionResultPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SchemaCapabilityType;

public class ResourceOperationalButtonsPanel extends AssignmentHolderOperationalButtonsPanel<ResourceType> {

    private static final String DOT_CLASS = ResourceOperationalButtonsPanel.class.getName() + ".";
    private static final String OPERATION_REFRESH_SCHEMA = DOT_CLASS + "refreshSchema";
    private static final String OPERATION_SET_MAINTENANCE = DOT_CLASS + "setMaintenance";

    private static final String ID_RESOURCE_BUTTONS = "resourceButtons";

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
    }

    private void initResourceButtons(RepeatingView resourceButtons) {
        AjaxButton test = new AjaxButton(resourceButtons.newChildId(),
                createStringResource("pageResource.button.test")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                testConnectionPerformed(target);
            }
        };
        test.add(AttributeAppender.append("class", "btn-default btn-sm"));
        resourceButtons.add(test);

        AjaxButton setMaintenance = new AjaxButton(resourceButtons.newChildId(),
                createStringResource("pageResource.button.toggleMaintenance")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                WebComponentUtil.toggleResourceMaintenance(getPrismObject(), OPERATION_SET_MAINTENANCE, target, getPageBase());
                refreshStatus(target);
            }
        };
        setMaintenance.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return canEdit(getObjectType());
            }
        });
        setMaintenance.add(AttributeAppender.append("class", "btn-default btn-sm"));
        resourceButtons.add(setMaintenance);

        AjaxButton refreshSchema = new AjaxButton(resourceButtons.newChildId(),
                createStringResource("pageResource.button.refreshSchema")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                WebComponentUtil.refreshResourceSchema(getPrismObject(), OPERATION_REFRESH_SCHEMA, target, getPageBase());
            }
        };
        refreshSchema.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isVisibleRefresSchemaButton(getObjectType());
            }
        });
        refreshSchema.add(AttributeAppender.append("class", "btn-default btn-sm"));
        resourceButtons.add(refreshSchema);
        AjaxButton editXml = new AjaxButton(resourceButtons.newChildId(),
                createStringResource("pageResource.button.editXml")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(PageDebugView.PARAM_OBJECT_ID, getPrismObject().getOid());
                parameters.add(PageDebugView.PARAM_OBJECT_TYPE, "ResourceType");
                getPageBase().navigateToNext(PageDebugView.class, parameters);
            }
        };
        resourceButtons.add(editXml);
        editXml.add(AttributeAppender.append("class", "btn-default btn-sm"));
        AjaxButton configurationEdit = new AjaxButton(resourceButtons.newChildId(),
                createStringResource("pageResource.button.configurationEdit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                startWizard(true, false);
            }
        };
        configurationEdit.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return canEdit(getObjectType());
            }
        });
        configurationEdit.add(AttributeAppender.append("class", "btn-default btn-sm"));
        resourceButtons.add(configurationEdit);
        AjaxButton wizardShow = new AjaxButton(resourceButtons.newChildId(),
                createStringResource("pageResource.button.wizardShow")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                startWizard(false, true);
            }
        };
        wizardShow.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return canEdit(getObjectType());
            }
        });
        wizardShow.add(AttributeAppender.append("class", "btn-default btn-sm"));
        resourceButtons.add(wizardShow);
        AjaxButton wizardEdit = new AjaxButton(resourceButtons.newChildId(),
                createStringResource("pageResource.button.wizardEdit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                startWizard(false, false);
            }
        };
        wizardEdit.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return canEdit(getObjectType());
            }
        });
        wizardEdit.add(AttributeAppender.append("class", "btn-default btn-sm"));
        resourceButtons.add(wizardEdit);

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

    private void startWizard(boolean configOnly, boolean readOnly) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, getPrismObject().getOid());        // compatibility with PageAdminResources
        parameters.add(PageResourceWizard.PARAM_CONFIG_ONLY, configOnly);
        parameters.add(PageResourceWizard.PARAM_READ_ONLY, readOnly);
        getPageBase().navigateToNext(new PageResourceWizard(parameters));
    }

    private boolean isVisibleRefresSchemaButton(ResourceType resource) {
        if (!resource.getAdditionalConnector().isEmpty()) {
            if (resource.getCapabilities() == null) {
                return false;
            }
            if (resource.getCapabilities().getConfigured() != null) {
                SchemaCapabilityType configuredCapability = CapabilityUtil.getCapability(resource.getCapabilities().getConfigured().getAny(), SchemaCapabilityType.class);
                if (configuredCapability == null) {
                    return false;
                }
                return configuredCapability.isEnabled();
            }
            return false;
        }
        return true;
    }

    //TODO abstract
    protected void refreshStatus(AjaxRequestTarget target) {

    }

}
