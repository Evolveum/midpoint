/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.MembersDetailsPopupPanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.RoleAnalysisTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisAccessTabPanel extends BasePanel<RoleAnalysisOutlierPartitionType> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";
    IModel<RoleAnalysisOutlierType> outlierModel;

    public RoleAnalysisAccessTabPanel(
            @NotNull String id,
            @NotNull IModel<RoleAnalysisOutlierPartitionType> partitionModel,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {
        super(id, partitionModel);
        this.outlierModel = outlierModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        List<ITab> tabs = createTabs();
        RoleAnalysisTabbedPanel<ITab> tabPanel = new RoleAnalysisTabbedPanel<>(ID_PANEL, tabs, null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer newLink(String linkId, final int index) {
                return new AjaxSubmitLink(linkId) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        super.onError(target);
                        target.add(getPageBase().getFeedbackPanel());
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        super.onSubmit(target);

                        setSelectedTab(index);
                        if (target != null) {
                            target.add(findParent(TabbedPanel.class));
                        }
                        target.add(getPageBase().getFeedbackPanel());
                    }

                };
            }
        };
        tabPanel.setOutputMarkupId(true);
        tabPanel.setOutputMarkupPlaceholderTag(true);
        tabPanel.add(AttributeAppender.append("class", "p-0 pl-2 m-0"));
        container.add(tabPanel);
    }

    protected List<ITab> createTabs() {
        RoleAnalysisOutlierType outlier = getOutlierModel().getObject();
        Task simpleTask = getPageBase().createSimpleTask("loadOutlierDetails");
        OperationResult result = simpleTask.getResult();
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        PrismObject<UserType> userTypeObject = roleAnalysisService.getUserTypeObject(outlier.getTargetObjectRef().getOid(),
                simpleTask, result);

        if (userTypeObject == null) {
            return new ArrayList<>();
        }

        List<ITab> tabs = new ArrayList<>();
        tabs.add(new PanelTab(getPageBase().createStringResource("Direct access assignment"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                List<PrismObject<FocusType>> members = new ArrayList<>();
                UserType user = userTypeObject.asObjectable();
                List<AssignmentType> assignment = user.getAssignment();
                for (AssignmentType assignmentType : assignment) {
                    if (assignmentType.getTargetRef() != null
                            && assignmentType.getTargetRef().getType() != null
                            && assignmentType.getTargetRef().getType().equals(RoleType.COMPLEX_TYPE)) {
                        String oid = assignmentType.getTargetRef().getOid();
                        PrismObject<FocusType> role = roleAnalysisService.getFocusTypeObject(oid,
                                simpleTask, result);
                        members.add(role);
                    }
                }

                MembersDetailsPopupPanel membersDetailsPopupPanel = new MembersDetailsPopupPanel(panelId, Model.of("TODO"),
                        members, RoleAnalysisProcessModeType.ROLE){
                    @Override
                    protected boolean showTableAsCard() {
                        return false;
                    }
                };
                membersDetailsPopupPanel.setOutputMarkupId(true);
                return membersDetailsPopupPanel;
            }
        });

        tabs.add(new PanelTab(getPageBase().createStringResource("Indirect access assignment"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                List<PrismObject<FocusType>> members = new ArrayList<>();
                UserType user = userTypeObject.asObjectable();
                List<AssignmentType> assignment = user.getAssignment();
                Set<String> assignmentOids = new HashSet<>();
                for (AssignmentType assignmentType : assignment) {
                    if (assignmentType.getTargetRef() != null
                            && assignmentType.getTargetRef().getType() != null
                            && assignmentType.getTargetRef().getType().equals(RoleType.COMPLEX_TYPE)) {
                        assignmentOids.add(assignmentType.getTargetRef().getOid());
                    }
                }
                List<ObjectReferenceType> membershipRef = user.getRoleMembershipRef();
                for (ObjectReferenceType ref : membershipRef) {
                    if (ref != null
                            && ref.getType() != null
                            && ref.getType().equals(RoleType.COMPLEX_TYPE)) {
                        String oid = ref.getOid();
                        if (assignmentOids.contains(oid)) {
                            continue;
                        }
                        PrismObject<FocusType> role = roleAnalysisService.getFocusTypeObject(oid,
                                simpleTask, result);
                        members.add(role);
                    }
                }

                MembersDetailsPopupPanel membersDetailsPopupPanel = new MembersDetailsPopupPanel(panelId, Model.of("TODO"),
                        members, RoleAnalysisProcessModeType.ROLE){
                    @Override
                    protected boolean showTableAsCard() {
                        return false;
                    }
                };
                membersDetailsPopupPanel.setOutputMarkupId(true);
                return membersDetailsPopupPanel;
            }
        });

        tabs.add(new PanelTab(getPageBase().createStringResource("Duplicated access assignment"), new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                List<PrismObject<FocusType>> members = new ArrayList<>();
                RoleAnalysisOutlierType outlier = getOutlierModel().getObject();
                List<ObjectReferenceType> duplicatedRoleAssignment = outlier.getDuplicatedRoleAssignment();
                Set<String> duplicatedRoleAssignmentOids = new HashSet<>();
                for (ObjectReferenceType ref : duplicatedRoleAssignment) {
                    duplicatedRoleAssignmentOids.add(ref.getOid());
                }

                RoleAnalysisOutlierPartitionType partition = getPartitionModel().getObject();
                List<DetectedAnomalyResult> detectedAnomalyResult = partition.getDetectedAnomalyResult();
                for (DetectedAnomalyResult detectedAnomaly : detectedAnomalyResult) {
                    String oid = detectedAnomaly.getTargetObjectRef().getOid();
                    if (duplicatedRoleAssignmentOids.contains(oid)) {
                        PrismObject<FocusType> focusTypeObject = roleAnalysisService.getFocusTypeObject(oid, simpleTask, result);
                        members.add(focusTypeObject);
                    }
                }

                MembersDetailsPopupPanel membersDetailsPopupPanel = new MembersDetailsPopupPanel(panelId, Model.of("TODO"),
                        members, RoleAnalysisProcessModeType.ROLE){
                    @Override
                    protected boolean showTableAsCard() {
                        return false;
                    }
                };
                membersDetailsPopupPanel.setOutputMarkupId(true);
                return membersDetailsPopupPanel;
            }
        });

        return tabs;
    }

    public IModel<RoleAnalysisOutlierType> getOutlierModel() {
        return outlierModel;
    }

    private IModel<RoleAnalysisOutlierPartitionType> getPartitionModel() {
        return getModel();
    }
}
