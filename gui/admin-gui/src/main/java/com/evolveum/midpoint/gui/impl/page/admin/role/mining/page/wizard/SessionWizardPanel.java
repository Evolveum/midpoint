/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.wizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangesExecutorImpl;
import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.ClusteringAction;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class SessionWizardPanel extends AbstractWizardPanel<RoleAnalysisSessionType, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final double DEFAULT_MIN_FREQUENCY = 30;
    private static final double DEFAULT_MAX_FREQUENCY = 100;

    public SessionWizardPanel(String id, WizardPanelHelper<RoleAnalysisSessionType, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        getPageBase().getFeedbackPanel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);

        add(createChoiceFragment(new ProcessModeChoiceStepPanel(getIdOfChoicePanel(), getHelper().getDetailsModel()) {
            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                showWizardFragment(target, new WizardPanel(getIdOfWizardPanel(),
                        new WizardModel(createBasicSteps())));
                super.onSubmitPerformed(target);
            }

        }));

    }

    private List<WizardStep> createBasicSteps() {
        List<WizardStep> steps = new ArrayList<>();
        steps.add(new BasicSessionInformationStepPanel(getHelper().getDetailsModel()) {
            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                SessionWizardPanel.this.onExitPerformed(target);
            }

        });

        steps.add(new SessionSimpleObjectsWizardPanel(getHelper().getDetailsModel()) {
            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
            }

            @Override
            public boolean onNextPerformed(AjaxRequestTarget target) {
//                RoleAnalysisSessionType roleAnalysisSession = getHelper().getDetailsModel().getObjectWrapper()
//                        .getObject().asObjectable();
//                AbstractAnalysisSessionOptionType sessionOptionType = getSessionOptionType(roleAnalysisSession);
//                RoleAnalysisDetectionOptionType defaultDetectionOption = new RoleAnalysisDetectionOptionType();
//                if (roleAnalysisSession.getProcessMode().equals(RoleAnalysisProcessModeType.ROLE)) {
//                    defaultDetectionOption.setMinUserOccupancy(sessionOptionType.getMinPropertiesOverlap());
//                    defaultDetectionOption.setMinRolesOccupancy(sessionOptionType.getMinMembersCount());
//                } else {
//                    defaultDetectionOption.setMinRolesOccupancy(sessionOptionType.getMinPropertiesOverlap());
//                    defaultDetectionOption.setMinUserOccupancy(sessionOptionType.getMinMembersCount());
//                }
//
//                defaultDetectionOption.setFrequencyRange(new RangeType()
//                        .min(DEFAULT_MIN_FREQUENCY)
//                        .max(DEFAULT_MAX_FREQUENCY));
//                defaultDetectionOption.setDetectionProcessMode(RoleAnalysisDetectionProcessType.PARTIAL);
//
//                roleAnalysisSession.setDefaultDetectionOption(defaultDetectionOption);
                return super.onNextPerformed(target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                SessionWizardPanel.this.onExitPerformed(target);
            }
        });

        steps.add(new SessionDetectionOptionsWizardPanel(getHelper().getDetailsModel()) {
            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {

//                RoleAnalysisSessionType roleAnalysisSession = getHelper().getDetailsModel().getObjectWrapper()
//                        .getObject().asObjectable();
//
//                RoleAnalysisProcessModeType processMode = roleAnalysisSession.getProcessMode();
//
//                OperationResult operationResult = new OperationResult("ExecuteClustering");
//                Task task = ((PageBase) getPage()).createSimpleTask("ExecuteClustering");
//                ClusteringAction clusteringAction = new ClusteringAction(processMode);
//                clusteringAction.execute((PageBase) getPage(), roleAnalysisSession, operationResult, task);
                PrismObject<RoleAnalysisSessionType> session = getDetailsModel().getObjectWrapper().getObject();

                OperationResult result = new OperationResult("ImportSessionObject");
                Task task = getPageBase().createSimpleTask("Import Session object");

                Collection<ObjectDelta<? extends ObjectType>> deltas;
                try {
                    deltas = getHelper().getDetailsModel().collectDeltas(result);
                } catch (SchemaException e) {
                    throw new RuntimeException(e);
                }

                Collection<ObjectDeltaOperation<? extends ObjectType>> objectDeltaOperations = new ObjectChangesExecutorImpl()
                        .executeChanges(deltas, false, task, result, target);

                RoleAnalysisProcessModeType processMode = session.asObjectable().getProcessMode();

                String sessionOid = ObjectDeltaOperation.findAddDeltaOidRequired(objectDeltaOperations,
                        RoleAnalysisSessionType.class);

                ClusteringAction clusteringAction = new ClusteringAction(processMode);
                clusteringAction.execute((PageBase) getPage(), sessionOid, result, task);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                SessionWizardPanel.this.onExitPerformed(target);
            }
        });

//        List<BusinessRoleApplicationDto> patterns = getAssignmentHolderModel().getPatternDeltas();
//        if (CollectionUtils.isNotEmpty(patterns)) {
//            steps.add(new ExsitingAccessApplicationRoleStepPanel<>(getAssignmentHolderModel()) {
//
//                @Override
//                protected void onExitPerformed(AjaxRequestTarget target) {
//                    SessionWizardPanel.this.onExitPerformed(target);
//                }
//
//                @Override
//                public VisibleEnableBehaviour getBackBehaviour() {
//                    return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
//                }
//            });
//
//            steps.add(new CandidateMembersPanel<>(getAssignmentHolderModel()) {
//
//                @Override
//                protected void onExitPerformed(AjaxRequestTarget target) {
//                    SessionWizardPanel.this.onExitPerformed(target);
//                }
//
//                @Override
//                public VisibleEnableBehaviour getBackBehaviour() {
//                    return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
//                }
//            });
//
//            steps.add(new AccessApplicationRoleStepPanel(getHelper().getDetailsModel()) {
//                @Override
//                protected void onSubmitPerformed(AjaxRequestTarget target) {
//                    super.onSubmitPerformed(target);
//                    SessionWizardPanel.this.onFinishBasicWizardPerformed(target);
//                }
//
//                @Override
//                protected boolean isSubmitEnable() {
//                    return getHelper().getDetailsModel().getPatternDeltas() != null;
//                }
//
//                @Override
//                protected void onExitPerformed(AjaxRequestTarget target) {
//                    SessionWizardPanel.this.onExitPerformed(target);
//                }
//            });
//
//        }

        return steps;
    }

    private void onFinishBasicWizardPerformed(AjaxRequestTarget target) {
        OperationResult result = onSavePerformed(target);
        if (!result.isError()) {
//            WebComponentUtil.createToastForCreateObject(target, RoleType.COMPLEX_TYPE);
            exitToPreview(target);
        }
    }

    private void exitToPreview(AjaxRequestTarget target) {
//        showChoiceFragment(
//                target,
//                new RoleWizardPreviewPanel<>(getIdOfChoicePanel(), getHelper().getDetailsModel(), PreviewTileType.class) {
//                    @Override
//                    protected void onTileClickPerformed(PreviewTileType value, AjaxRequestTarget target) {
//                        switch (value) {
//                            case CONFIGURE_MEMBERS:
//                                showMembersPanel(target);
//                                break;
//                            case CONFIGURE_GOVERNANCE_MEMBERS:
//                                showGovernanceMembersPanel(target);
//                                break;
//                        }
//                    }
//                });
    }

    private void showGovernanceMembersPanel(AjaxRequestTarget target) {
//        showChoiceFragment(target, new GovernanceMembersWizardPanel(
//                getIdOfChoicePanel(),
//                getAssignmentHolderModel()) {
//            @Override
//            protected void onExitPerformed(AjaxRequestTarget target) {
//                super.onExitPerformed(target);
//                exitToPreview(target);
//            }
//        });
    }

    private void showMembersPanel(AjaxRequestTarget target) {
//        showChoiceFragment(target, new MembersWizardPanel(
//                getIdOfChoicePanel(),
//                getAssignmentHolderModel()) {
//            @Override
//            protected void onExitPerformed(AjaxRequestTarget target) {
//                super.onExitPerformed(target);
//                exitToPreview(target);
//            }
//        });
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
