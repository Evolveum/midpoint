/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.ClusterOptions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.test.ClusteringAction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.ArrayList;
import java.util.List;

public class SessionWizardPanel extends AbstractWizardPanel<RoleAnalysisSessionType, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> {

    private static final double DEFAULT_MIN_FREQUENCY = 0.3;
    private static final double DEFAULT_MAX_FREQUENCY = 1.0;

    public SessionWizardPanel(String id, WizardPanelHelper<RoleAnalysisSessionType, AssignmentHolderDetailsModel<RoleAnalysisSessionType>> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        getPageBase().getFeedbackPanel().add(VisibleEnableBehaviour.ALWAYS_INVISIBLE);
        add(createWizardFragment(new WizardPanel(getIdOfWizardPanel(), new WizardModel(createBasicSteps()))));
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

        PrismObject<RoleAnalysisClusterType> roleAnalysisClusterTypePrismObject =
                new RoleAnalysisClusterType().asPrismObject();

        LoadableDetachableModel<PrismObject<RoleAnalysisClusterType>> loadableDetachableModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismObject<RoleAnalysisClusterType> load() {
                return roleAnalysisClusterTypePrismObject;
            }
        };

        AssignmentHolderDetailsModel<RoleAnalysisClusterType> assignmentHolderDetailsModel
                = new AssignmentHolderDetailsModel<>(loadableDetachableModel, (PageBase) getPage());

        RoleAnalysisSessionOptionType roleAnalysisSessionOptionType = new RoleAnalysisSessionOptionType();
        roleAnalysisSessionOptionType.setSimilarityThreshold(80.0);
        roleAnalysisSessionOptionType.setMinUniqueMembersCount(5);
        roleAnalysisSessionOptionType.setMinMembersCount(10);
        roleAnalysisSessionOptionType.setProcessMode(RoleAnalysisProcessModeType.USER);
        roleAnalysisSessionOptionType.setMinPropertiesCount(10);
        roleAnalysisSessionOptionType.setMaxPropertiesCount(100);
        roleAnalysisSessionOptionType.setMinPropertiesOverlap(10);
        getHelper().getDetailsModel().getObjectType().setClusterOptions(roleAnalysisSessionOptionType);
        steps.add(new SessionSimpleObjectsWizardPanel(getHelper().getDetailsModel()) {
            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
            }

            @Override
            public boolean onNextPerformed(AjaxRequestTarget target) {
                RoleAnalysisSessionType roleAnalysisSession = getHelper().getDetailsModel().getObjectType();
                RoleAnalysisSessionOptionType clusterOptions = roleAnalysisSession.getClusterOptions();
                RoleAnalysisClusterType roleAnalysisCluster = roleAnalysisClusterTypePrismObject.asObjectable();
                roleAnalysisCluster.setDetectionOption(new RoleAnalysisDetectionOptionType());

                RoleAnalysisDetectionOptionType detectionOption = roleAnalysisCluster.getDetectionOption();
                detectionOption.setMinPropertiesOccupancy(clusterOptions.getMinPropertiesCount());
                detectionOption.setMinMembersOccupancy(clusterOptions.getMinMembersCount());
                detectionOption.setMinFrequencyThreshold(DEFAULT_MIN_FREQUENCY);
                detectionOption.setMaxFrequencyThreshold(DEFAULT_MAX_FREQUENCY);
                detectionOption.setDetectionProcessMode(RoleAnalysisDetectionProcessType.PARTIAL);
                return super.onNextPerformed(target);
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                SessionWizardPanel.this.onExitPerformed(target);
            }
        });

        steps.add(new SessionDetectionOptionsWizardPanel(assignmentHolderDetailsModel) {
            @Override
            public VisibleEnableBehaviour getBackBehaviour() {
                return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {

                //TODO just temporary. Add detect enum to schema and change clusteringAction.execute constructor.
                RoleAnalysisSessionType roleAnalysisSession = getHelper().getDetailsModel().getObjectType();
                RoleAnalysisClusterType roleAnalysisCluster = this.getDetailsModel().getObjectType();
                RoleAnalysisSessionOptionType roleAnalysisSessionClusterOptions = roleAnalysisSession.getClusterOptions();
                RoleAnalysisDetectionOptionType roleAnalysisClusterDetectionOption = roleAnalysisCluster.getDetectionOption();
                RoleAnalysisProcessModeType processMode = roleAnalysisSessionClusterOptions.getProcessMode();

                ObjectFilter objectFilter = null;

                if (roleAnalysisSessionClusterOptions.getFilter() != null) {
                    try {
                        objectFilter = getPrismContext().createQueryParser()
                                .parseFilter(FocusType.class, roleAnalysisSessionClusterOptions.getFilter());
                    } catch (SchemaException e) {
                        throw new RuntimeException(e);
                    }
                }

                ClusterOptions clusterOptions = new ClusterOptions((PageBase) getPage(),
                        roleAnalysisSessionClusterOptions.getSimilarityThreshold(),
                        roleAnalysisSessionClusterOptions.getMinUniqueMembersCount(),
                        roleAnalysisSessionClusterOptions.getMinMembersCount(),
                        roleAnalysisSessionClusterOptions.getMinPropertiesOverlap(),
                        objectFilter,
                        roleAnalysisSessionClusterOptions.getMinPropertiesCount(),
                        roleAnalysisSessionClusterOptions.getMaxPropertiesCount(),
                        roleAnalysisSessionClusterOptions.getProcessMode(),
                        roleAnalysisSession.getName().toString(),
                        roleAnalysisClusterDetectionOption.getDetectionProcessMode(),
                        roleAnalysisClusterDetectionOption.getMinMembersOccupancy(),
                        roleAnalysisClusterDetectionOption.getMinPropertiesOccupancy(),
                        roleAnalysisClusterDetectionOption.getMinFrequencyThreshold(),
                        roleAnalysisClusterDetectionOption.getMaxFrequencyThreshold());

                OperationResult operationResult = new OperationResult("ExecuteClustering");
                Task task = ((PageBase) getPage()).createSimpleTask("ExecuteClustering");
                ClusteringAction clusteringAction = new ClusteringAction(processMode);
                clusteringAction.execute((PageBase) getPage(), clusterOptions, operationResult, task);

//                com.evolveum.midpoint.gui.impl.page.admin.role.mining.test.ClusteringAction clusteringAction = new ClusteringAction(processMode);
//                clusteringAction.execute((PageBase) getPage(), clusterOptions, operationResult, task);

//                super.onSubmitPerformed(target);
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
