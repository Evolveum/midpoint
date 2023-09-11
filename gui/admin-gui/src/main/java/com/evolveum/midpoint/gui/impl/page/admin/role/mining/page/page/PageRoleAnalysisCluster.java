/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.recomputeRoleAnalysisClusterDetectionOptions;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.component.AssignmentHolderOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.ClusterSummaryPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.Serial;

//TODO correct authorizations
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysisCluster", matchUrlForSecurity = "/admin/roleAnalysisCluster")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                label = "PageRoleAnalysis.auth.roleAnalysisAll.label",
                description = "PageRoleAnalysis.auth.roleAnalysisAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_CLUSTER_URL,
                label = "PageRoleAnalysis.auth.roleAnalysisCluster.label",
                description = "PageRoleAnalysis.auth.roleAnalysisCluster.description")
})

public class PageRoleAnalysisCluster extends PageAssignmentHolderDetails<RoleAnalysisClusterType, AssignmentHolderDetailsModel<RoleAnalysisClusterType>> {

    private static final String DOT_CLASS = PageRoleAnalysisCluster.class.getName() + ".";
    private static final String OP_PATTERN_DETECTION = DOT_CLASS + "patternDetection";
    private static final String OP_RECOMPUTE_SESSION_STAT = DOT_CLASS + "recomputeSessionStatistic";

    @Override
    protected AssignmentHolderOperationalButtonsPanel<RoleAnalysisClusterType> createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<RoleAnalysisClusterType>> wrapperModel) {
        return super.createButtonsPanel(id, wrapperModel);
    }

    @Override
    public void addAdditionalButtons(RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_OBJECT_TASK_ICON, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton detection = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(), iconBuilder.build(),
                setDetectionButtonTitle()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                detectionPerform(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        detection.titleAsLabel(true);
        detection.setOutputMarkupId(true);
        detection.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        repeatingView.add(detection);

        Form<?> form = detection.findParent(Form.class);
        if (form != null) {
            form.setDefaultButton(detection);
        }
    }

    public void detectionPerform(AjaxRequestTarget target) {

        //TODO
        OperationResult result = new OperationResult(OP_PATTERN_DETECTION);

        String clusterOid = getObjectDetailsModels().getObjectType().getOid();

        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectWrapper().getObject().asObjectable();

        Task task = ((PageBase) getPage()).createSimpleTask(OP_PATTERN_DETECTION);
        ModelService modelService = ((PageBase) getPage()).getModelService();
        DetectionOption detectionOption = new DetectionOption(cluster);

        recomputeRoleAnalysisClusterDetectionOptions(modelService, clusterOid, detectionOption, task, result);

        executeDetectionTask(result, task, clusterOid);

        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil.getObjectDetailsPage(RoleAnalysisClusterType.class);
        ((PageBase) getPage()).navigateToNext(detailsPageClass, params);

        ((PageBase) getPage()).showResult(result);
        target.add(getFeedbackPanel());
    }

    private void executeDetectionTask(OperationResult result, Task task, String clusterOid) {
        try {
            ActivityDefinitionType activity = createActivity(clusterOid);

            getModelInteractionService().submit(
                    activity,
                    ActivitySubmissionOptions.create()
                            .withTaskTemplate(new TaskType()
                                    .name("Pattern detection  (" + clusterOid + ")"))
                            .withArchetypes(
                                    SystemObjectsType.ARCHETYPE_UTILITY_TASK.value()),
                    task, result);

        } catch (CommonException e) {
            LOGGER.error("Couldn't execute Cluster Detection Task {}", clusterOid, e);
            result.recordPartialError(e);
        } finally {
            result.recordSuccessIfUnknown();
        }
    }

    private ActivityDefinitionType createActivity(String clusterOid) {

        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setType(RoleAnalysisClusterType.COMPLEX_TYPE);
        objectReferenceType.setOid(clusterOid);

        RoleAnalysisPatternDetectionWorkDefinitionType rdw = new RoleAnalysisPatternDetectionWorkDefinitionType();
        rdw.setClusterRef(objectReferenceType);

        return new ActivityDefinitionType()
                .work(new WorkDefinitionsType()
                        .roleAnalysisPatternDetection(rdw));
    }

    public StringResourceModel setDetectionButtonTitle() {
        return ((PageBase) getPage()).createStringResource("PageAnalysisCluster.button.save");
    }

    @Override
    public void afterDeletePerformed(AjaxRequestTarget target) {
        PageBase pageBase = (PageBase) getPage();
        Task task = pageBase.createSimpleTask(OP_RECOMPUTE_SESSION_STAT);
        OperationResult result = task.getResult();

        RoleAnalysisClusterType cluster = getModelWrapperObject().getObjectOld().asObjectable();
        ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();
        RoleAnalysisObjectUtils.recomputeSessionStatic(pageBase.getModelService(), roleAnalysisSessionRef.getOid(), cluster, task, result);

    }

    public PageRoleAnalysisCluster() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    public Class<RoleAnalysisClusterType> getType() {
        return RoleAnalysisClusterType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<RoleAnalysisClusterType> summaryModel) {
        return new ClusterSummaryPanel(id, summaryModel, null);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageMiningOperation.title");
    }

}

