/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageFocusDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.BasicSettingResourceObjectTypeStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.DelineationResourceObjectTypeStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.FocusResourceObjectTypeStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation.*;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.associations.AssociationStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.AttributeInboundStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.AttributeOutboundStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.LimitationsStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.MainConfigurationStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials.PasswordStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization.ReactionMainSettingStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization.ReactionOptionalSettingStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.*;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction.BasicConstructionStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction.ConstructionGroupStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction.ConstructionOutboundMappingsStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.construction.ConstructionResourceStepPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.ContainerValueWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.roles.component.RoleSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/role", matchUrlForSecurity = "/admin/role")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, label = "PageAdminRoles.auth.roleAll.label", description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL, label = "PageRole.auth.role.label", description = "PageRole.auth.role.description") })
public class PageRole extends PageFocusDetails<RoleType, FocusDetailsModels<RoleType>> {

    private static final Trace LOGGER = TraceManager.getTrace(PageRole.class);

    public PageRole() {
        super();
    }

    public PageRole(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageRole(PrismObject<RoleType> role) {
        super(role);
    }

    @Override
    public Class<RoleType> getType() {
        return RoleType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<RoleType> summaryModel) {
        return new RoleSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }

    protected DetailsFragment createDetailsFragment() {

        if (canShowWizard(SystemObjectsType.ARCHETYPE_APPLICATION_ROLE)) {
            setUseWizardForCreating();
            return createRoleWizardFragment(ApplicationRoleWizardPanel.class);
        }

        if (canShowWizard(SystemObjectsType.ARCHETYPE_BUSINESS_ROLE)) {
            setUseWizardForCreating();
            return createRoleWizardFragment(BusinessRoleWizardPanel.class);
        }

        return super.createDetailsFragment();
    }

    private boolean canShowWizard(SystemObjectsType archetype) {
        return !isEditObject() && WebComponentUtil.hasArchetypeAssignment(
                getObjectDetailsModels().getObjectType(),
                archetype.value());
    }

    private DetailsFragment createRoleWizardFragment(Class<? extends AbstractWizardPanel> clazz) {
        return new DetailsFragment(ID_DETAILS_VIEW, ID_TEMPLATE_VIEW, PageRole.this) {
            @Override
            protected void initFragmentLayout() {
                try {
                    Constructor<? extends AbstractWizardPanel> constructor = clazz.getConstructor(String.class, WizardPanelHelper.class);
                    AbstractWizardPanel wizard = constructor.newInstance(ID_TEMPLATE, createRoleWizardPanelHelper());
                    add(wizard);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    LOGGER.error("Couldn't create panel by constructor for class " + clazz.getSimpleName()
                            + " with parameters type: String, ResourceWizardPanelHelper");
                }
            }
        };
    }

    private WizardPanelHelper<RoleType, FocusDetailsModels<RoleType>> createRoleWizardPanelHelper() {
        return new WizardPanelHelper<>(getObjectDetailsModels()) {

            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                navigateToNext(PageRoles.class);
            }

            @Override
            public IModel<PrismContainerValueWrapper<RoleType>> getValueModel() {
                return new ContainerValueWrapperFromObjectWrapperModel<>(
                        getDetailsModel().getObjectWrapperModel(), ItemPath.EMPTY_PATH);
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = new OperationResult(OPERATION_SAVE);
                saveOrPreviewPerformed(target, result, false);
                return result;
            }
        };
    }

    @Override
    protected FocusDetailsModels<RoleType> createObjectDetailsModels(PrismObject<RoleType> object) {
        return new FocusDetailsModels<>(createPrismObjectModel(object), this) {

            @Override
            protected boolean isReadonly() {
                return getReadonlyOverride() != null ? getReadonlyOverride() : super.isReadonly();
            }

            @Override
            protected WrapperContext createWrapperContext(Task task, OperationResult result) {
                WrapperContext ctx = new WrapperContext(task, result) {
                    @Override
                    protected boolean isIgnoredWizardPanel(ContainerPanelConfigurationType panelConfig) {
                        boolean useForAdd = WebComponentUtil.hasArchetypeAssignment(getObjectType(), SystemObjectsType.ARCHETYPE_APPLICATION_ROLE.value());

                        if ((AccessApplicationRoleStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
                                || AccessApplicationStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
                                || BasicInformationStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
                                || MembersWizardPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
                                || GovernanceMembersWizardPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
                                || ConstructionResourceStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
                                || BasicConstructionStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
                                || ConstructionGroupStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
                                || ConstructionOutboundMappingsStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier()))
                                && (PageRole.this.isEditObject()
                                || (!PageRole.this.isEditObject()
                                && OperationTypeType.ADD.equals(panelConfig.getApplicableForOperation())
                                && !useForAdd))) {
                            // UGLY HACK we need define visibility of panel in details menu
                            return true;
                        }
                        return false;
                    }
                };
                ctx.setCreateIfEmpty(true);
                ctx.setDetailsPageTypeConfiguration(getPanelConfigurations());

                if (WebComponentUtil.hasArchetypeAssignment(getObjectType(), SystemObjectsType.ARCHETYPE_APPLICATION_ROLE.value())) {
                    ctx.setConfigureMappingType(true);
                }
                return ctx;
            }
        };
    }
}
