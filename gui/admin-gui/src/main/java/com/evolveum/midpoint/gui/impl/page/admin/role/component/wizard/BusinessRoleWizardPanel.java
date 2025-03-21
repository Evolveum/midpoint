/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard;

import java.util.*;

import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * @author lskublik
 */
public class BusinessRoleWizardPanel extends AbstractWizardPanel<RoleType, AbstractRoleDetailsModel<RoleType>> {

    public BusinessRoleWizardPanel(String id, WizardPanelHelper<RoleType, AbstractRoleDetailsModel<RoleType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        getPageBase().getFeedbackPanel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        add(createWizardFragment(new WizardPanel(getIdOfWizardPanel(), new WizardModel(createBasicSteps()))));
    }

    private List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new BasicInformationStepPanel(getHelper().getDetailsModel()) {
            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                BusinessRoleWizardPanel.this.onExitPerformed(target);
            }
        });

        BusinessRoleApplicationDto patterns = getAssignmentHolderModel().getPatternDeltas();
        boolean isRoleMigration = patterns != null && CollectionUtils.isNotEmpty(patterns.getUserMembers());

        if (isRoleMigration) {
            initRoleMiningMigrationStep(patterns, steps);
        } else {

            steps.add(new AccessApplicationRoleStepPanel(getHelper().getDetailsModel()) {
                @Override
                protected void onSubmitPerformed(AjaxRequestTarget target) {
                    super.onSubmitPerformed(target);
                    BusinessRoleWizardPanel.this.onFinishBasicWizardPerformed(target);
                }

                @Override
                protected SelectableBeanObjectDataProvider<RoleType> createProvider(SelectableBeanObjectDataProvider<RoleType> defaultProvider) {
                    return super.createProvider(defaultProvider);
                }

                @Override
                protected boolean isMandatory() {
                    return false;
                }

                @Override
                protected void onExitPerformed(AjaxRequestTarget target) {
                    BusinessRoleWizardPanel.this.onExitPerformed(target);
                }
            });
        }

        return steps;
    }

    private void initRoleMiningMigrationStep(@NotNull BusinessRoleApplicationDto patterns, List<WizardStep> steps) {
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        RoleType businessRole = patterns.getBusinessRole().asObjectable();
        Set<ObjectReferenceType> candidateRoles = patterns.getRoleInducements();
        businessRole.getInducement().clear();

        Task task = getPageBase().createSimpleTask("Load selected roles");
        Set<RoleType> initialSelectedRoles = new HashSet<>();
        IModel<List<AbstractMap.SimpleEntry<String, String>>> selectedItems = Model.ofList(new ArrayList<>());
        for (ObjectReferenceType roleRef : candidateRoles) {
            PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(roleRef.getOid(), task, task.getResult());
            if (roleTypeObject != null) {
                initialSelectedRoles.add(roleTypeObject.asObjectable());
                selectedItems.getObject().add(
                        new AbstractMap.SimpleEntry<>(
                                roleRef.getOid(),
                                WebComponentUtil.getDisplayNameOrName(roleTypeObject)));
            }
        }

        steps.add(new AccessApplicationRoleStepPanel(getHelper().getDetailsModel(), selectedItems) {
            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                super.onSubmitPerformed(target);
                BusinessRoleWizardPanel.this.onFinishBasicWizardPerformed(target);
            }

            @Override
            protected void processSelectOrDeselectItem(
                    @NotNull IModel<List<AbstractMap.SimpleEntry<String, String>>> selectedItems,
                    @NotNull SelectableBean<RoleType> value,
                    @NotNull SelectableBeanObjectDataProvider<RoleType> provider,
                    @NotNull AjaxRequestTarget target) {
                refreshSubmitAndNextButton(target);

                RoleType applicationRole = value.getValue();
                String oid = applicationRole.getOid();
                if (value.isSelected()) {
                    selectedItems.getObject().add(
                            new AbstractMap.SimpleEntry<>(
                                    oid,
                                    WebComponentUtil.getDisplayNameOrName(applicationRole.asPrismObject())));
                    provider.getSelected().add(applicationRole);
                } else {
                    selectedItems.getObject().removeIf(entry -> entry.getKey().equals(oid));
                    provider.getSelected().removeIf(entry -> entry.getOid().equals(oid));
                }
            }

            @Override
            protected SelectableBeanObjectDataProvider<RoleType> createProvider(SelectableBeanObjectDataProvider<RoleType> defaultProvider) {
                return super.createProvider(defaultProvider);
            }

            @Override
            protected @NotNull Set<RoleType> initialSelectedObjects() {
                return initialSelectedRoles;
            }

            @Override
            protected ObjectQuery getCustomQuery() {
                List<String> alreadySelectedRoles = candidateRoles.stream()
                        .map(ObjectReferenceType::getOid)
                        .toList();

                return PrismContext.get().queryFor(RoleType.class)
                        .id(alreadySelectedRoles.toArray(new String[0]))
                        .or()
                        .item(AssignmentHolderType.F_ARCHETYPE_REF).ref(SystemObjectsType.ARCHETYPE_APPLICATION_ROLE.value())
                        .build();
            }

            @Override
            protected boolean isSubmitEnable() {
                return true;
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                BusinessRoleWizardPanel.this.onExitPerformed(target);
            }

        });
    }

    private void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
        OperationResult result = onSavePerformed(target);
        if (!result.isError()) {
//            WebComponentUtil.createToastForCreateObject(target, RoleType.COMPLEX_TYPE);
            exitToPreview(target);
        }
    }

    private void exitToPreview(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new RoleWizardChoicePanel<>(getIdOfChoicePanel(), getHelper().getDetailsModel(), PreviewTileType.class) {
                    @Override
                    protected void onTileClickPerformed(PreviewTileType value, AjaxRequestTarget target) {
                        switch (value) {
                            case CONFIGURE_MEMBERS -> showMembersPanel(target);
                            case CONFIGURE_GOVERNANCE_MEMBERS -> showGovernanceMembersPanel(target);
                        }
                    }

                    @Override
                    protected void onExitPerformed(AjaxRequestTarget target) {
                        BusinessRoleWizardPanel.this.onExitPerformed(target);
                    }
                });
    }

    private void showGovernanceMembersPanel(AjaxRequestTarget target) {
        showChoiceFragment(target, new GovernanceMembersWizardPanel(
                getIdOfChoicePanel(),
                getAssignmentHolderModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }
        });
    }

    private void showMembersPanel(AjaxRequestTarget target) {
        showChoiceFragment(target, new MembersWizardPanel(
                getIdOfChoicePanel(),
                getAssignmentHolderModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                exitToPreview(target);
            }
        });
    }

    enum PreviewTileType implements TileEnum {

        CONFIGURE_GOVERNANCE_MEMBERS("fa fa-users"),
        CONFIGURE_MEMBERS("fa fa-users");

        private final String icon;

        PreviewTileType(String icon) {
            this.icon = icon;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }
}
