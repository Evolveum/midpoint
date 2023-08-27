/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.recomputeRoleAnalysisClusterDetectionOptions;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.Tools.getScaleScript;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.ClusterSummaryPanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

//TODO correct authorizations
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysisCluster", matchUrlForSecurity = "/admin/roleAnalysisCluster")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(
                actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL,
                label = "PageAdminRoles.auth.roleAll.label",
                description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(
                actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL,
                label = "PageRole.auth.role.label",
                description = "PageRole.auth.role.description") })

public class PageRoleAnalysisCluster extends AbstractPageObjectDetails<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnDomReadyHeaderItem.forScript(getScaleScript()));
    }

    @Override
    public StringResourceModel setSaveButtonTitle() {
        return ((PageBase) getPage()).createStringResource("PageAnalysisCluster.button.save");
    }

    @Override
    public void savePerformed(AjaxRequestTarget target) {

        //TODO
        OperationResult result = new OperationResult("ImportSessionObject");

        String clusterOid = getObjectDetailsModels().getObjectType().getOid();

        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectWrapper().getObject().asObjectable();

        DetectionOption detectionOption = new DetectionOption(cluster);

        recomputeRoleAnalysisClusterDetectionOptions(clusterOid, (PageBase) getPage(), detectionOption, result);

        Task task = ((PageBase) getPage()).createSimpleTask("Pattern detection");
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
            //TODO
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

    @Override
    public void afterDeletePerformed(AjaxRequestTarget target) {
        PageBase pageBase = (PageBase) getPage();
        Task task = pageBase.createSimpleTask("Recompute object");
        OperationResult result = task.getResult();

        RoleAnalysisClusterType cluster = getModelWrapperObject().getObjectOld().asObjectable();
        ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();
        RoleAnalysisObjectUtils.recomputeSessionStatic(result, roleAnalysisSessionRef.getOid(), cluster, pageBase);
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

