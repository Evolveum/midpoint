/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.ExtractIntersections.businessRoleDetection;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.ClusterAlgorithmUtils.loadDefaultIntersection;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getClusterTypeObject;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSearchModeType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.ExtractJaccard;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.ExecuteSearchPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.ProcessBusinessRolePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.DetectedPattern;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningOperationChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningUserTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.panel.MiningIntersectionTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.panel.MiningRoleBasedTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.panel.MiningUserBasedTable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;


@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/miningOperation", matchUrlForSecurity = "/admin/miningOperation")
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

public class PageMiningOperation extends PageAdmin {

    private static final String ID_DATATABLE = "datatable_extra";
    private static final String ID_DATATABLE_INTERSECTIONS = "table_intersection";
    private static final String ID_PROCESS_BUTTON = "process_selections_id";

    public static final String PARAMETER_OID = "oid";
    public static final String PARAMETER_MODE = "mode";
    public static final String PARAMETER_SEARCH_MODE = "searchMode";
    public static final String PARAMETER_SORT = "sort";

    double minFrequency = 0.3;

    Integer minOccupancy = 5;
    double maxFrequency = 1.0;
    Integer minIntersection = 10;
    List<DetectedPattern> mergedIntersection = new ArrayList<>();
    AjaxButton processButton;
    DetectedPattern intersection = null;
    boolean compress = true;

    String compressMode = "COMPRESS MODE";

    OperationResult result = new OperationResult("GetObject");

    MiningOperationChunk miningOperationChunk;

    List<MiningRoleTypeChunk> miningRoleTypeChunks;
    List<MiningUserTypeChunk> miningUserTypeChunks;
    ClusterObjectUtils.SORT sortMode;

    RoleAnalysisSearchModeType searchMode = RoleAnalysisSearchModeType.INTERSECTION;

    String getPageParameterOid() {
        PageParameters params = getPageParameters();
        return params.get(PARAMETER_OID).toString();
    }

    RoleAnalysisProcessModeType getPageParameterMode() {
        PageParameters params = getPageParameters();
        if (params.get(PARAMETER_MODE).toString().equals(RoleAnalysisProcessModeType.USER.value())) {
            return RoleAnalysisProcessModeType.USER;
        }
        return RoleAnalysisProcessModeType.ROLE;
    }

    RoleAnalysisSearchModeType getPageParameterSearchMode() {
        PageParameters params = getPageParameters();
        if (params.get(PARAMETER_SEARCH_MODE).toString().equals(RoleAnalysisSearchModeType.INTERSECTION.value())) {
            return RoleAnalysisSearchModeType.INTERSECTION;
        }
        return RoleAnalysisSearchModeType.JACCARD;
    }

    int getPageParameterSort() {
        PageParameters params = getPageParameters();
        return params.get(PARAMETER_SORT).toInteger();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnDomReadyHeaderItem.forScript(getScaleScript()));
    }

    public PageMiningOperation() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        miningOperationChunk = new MiningOperationChunk();
        if (getPageParameterSort() <= 500) {
            sortMode = ClusterObjectUtils.SORT.JACCARD;
        } else {
            sortMode = ClusterObjectUtils.SORT.NONE;
        }

        searchMode = getPageParameterSearchMode();

        long start = startTimer("LOAD DATA");
        RoleAnalysisClusterType cluster = getClusterTypeObject((PageBase) getPage(), getPageParameterOid()).asObjectable();
        mergedIntersection = loadDefaultIntersection(cluster);
        loadMiningTableData();
        endTimer(start, "LOAD DATA");

        start = startTimer("LOAD TABLE");
        loadMiningTable(miningRoleTypeChunks, miningUserTypeChunks, searchMode);
        endTimer(start, "LOAD TABLE");

        add(generateTableIntersection(ID_DATATABLE_INTERSECTIONS, mergedIntersection).setOutputMarkupId(true));

        AjaxButton ajaxButton = executeBusinessSearchPanel();
        add(ajaxButton);

        AjaxButton sortButton = executeJaccardSorting();
        add(sortButton);

        initProcessButton();

        add(processButton);

    }

    private void initProcessButton() {
        processButton = new AjaxButton(ID_PROCESS_BUTTON, createStringResource("RoleMining.button.title.process")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                ProcessBusinessRolePanel detailsPanel = new ProcessBusinessRolePanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("TO DO: details"), miningRoleTypeChunks, miningUserTypeChunks, getPageParameterMode()) {
                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }
                };
                ((PageBase) getPage()).showMainPopup(detailsPanel, ajaxRequestTarget);

            }

        };

        processButton.setOutputMarkupId(true);
        processButton.setOutputMarkupPlaceholderTag(true);
        processButton.setVisible(false);
    }

    @NotNull
    private AjaxButton executeBusinessSearchPanel() {
        AjaxButton ajaxButton = new AjaxButton("business_role_mining") {
            @Override
            public void onClick(AjaxRequestTarget target) {

                ExecuteSearchPanel detailsPanel = new ExecuteSearchPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Analyzed members details panel")) {
                    @Override
                    public void performAction(AjaxRequestTarget target) {
                        minOccupancy = getMinOccupancy();
                        minFrequency = getMinFrequency();
                        maxFrequency = getMaxFrequency();
                        minIntersection = getMinIntersection();
                        intersection = null;
                        searchMode = getSearchModeSelected();
                        if (searchMode.equals(RoleAnalysisSearchModeType.JACCARD)) {
                            mergedIntersection = ExtractJaccard.businessRoleDetection(miningRoleTypeChunks, miningUserTypeChunks,
                                    minFrequency, maxFrequency,
                                    minIntersection, minOccupancy, getPageParameterMode(), getSimilarity());
                        } else {
                            mergedIntersection = businessRoleDetection(miningRoleTypeChunks, miningUserTypeChunks, minFrequency,
                                    maxFrequency,
                                    minIntersection, minOccupancy, getPageParameterMode());
                        }
                        getIntersectionTable().replaceWith(generateTableIntersection(ID_DATATABLE_INTERSECTIONS,
                                mergedIntersection));
                        target.add(getIntersectionTable().setOutputMarkupId(true));

                        updateMiningTable(target, true, searchMode, miningRoleTypeChunks, miningUserTypeChunks);
                    }

                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }
                };
                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
            }
        };
        ajaxButton.setOutputMarkupId(true);
        return ajaxButton;
    }

    private AjaxButton executeJaccardSorting() {

        AjaxButton ajaxButton = new AjaxButton("jaccard_sort") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                sortMode = ClusterObjectUtils.SORT.JACCARD;

                miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(sortMode);
                miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(sortMode);

                updateMiningTable(target, true, searchMode, miningRoleTypeChunks, miningUserTypeChunks);
                target.add(this.setVisible(false));
            }
        };

        ajaxButton.setOutputMarkupId(true);
        ajaxButton.setVisible(sortMode.equals(ClusterObjectUtils.SORT.NONE));
        return ajaxButton;
    }

    private void loadMiningTableData() {
        RoleAnalysisClusterType cluster = getClusterTypeObject((PageBase) getPage(), getPageParameterOid()).asObjectable();

        //TODO should only be used on a gui request?
        // In the case of large datasets, Jaccard sorting is
        // time-consuming. Or progress (loading) bar?

        miningOperationChunk.run(cluster, (PageBase) getPage(), getPageParameterMode(), result, compress, true);
        miningRoleTypeChunks = miningOperationChunk.getMiningRoleTypeChunks(sortMode);
        miningUserTypeChunks = miningOperationChunk.getMiningUserTypeChunks(sortMode);

    }

    private void loadMiningTable(List<MiningRoleTypeChunk> miningRoleTypeChunks, List<MiningUserTypeChunk> miningUserTypeChunks,
            RoleAnalysisSearchModeType searchMode) {
        if (getPageParameterMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            MiningRoleBasedTable boxedTablePanel = generateMiningRoleBasedTable(miningRoleTypeChunks,
                    miningUserTypeChunks, false, minFrequency, null, maxFrequency, searchMode);
            boxedTablePanel.setOutputMarkupId(true);
            add(boxedTablePanel);
        } else if (getPageParameterMode().equals(RoleAnalysisProcessModeType.USER)) {
            MiningUserBasedTable boxedTablePanel = generateMiningUserBasedTable(miningRoleTypeChunks,
                    miningUserTypeChunks, false, minFrequency, null, maxFrequency);
            boxedTablePanel.setOutputMarkupId(true);
            add(boxedTablePanel);
        }

    }

    private void updateMiningTable(AjaxRequestTarget target, boolean resetStatus, RoleAnalysisSearchModeType searchMode,
            List<MiningRoleTypeChunk> miningRoleTypeChunks, List<MiningUserTypeChunk> miningUserTypeChunks) {

        if (resetStatus) {
            for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
                miningRoleTypeChunk.setStatus(ClusterObjectUtils.Status.NEUTRAL);
            }
            for (MiningUserTypeChunk miningUserTypeChunk : miningUserTypeChunks) {
                miningUserTypeChunk.setStatus(ClusterObjectUtils.Status.NEUTRAL);
            }
        }

        if (getPageParameterMode().equals(RoleAnalysisProcessModeType.ROLE)) {
            MiningRoleBasedTable boxedTablePanel = generateMiningRoleBasedTable(miningRoleTypeChunks,
                    miningUserTypeChunks, false, minFrequency, intersection, maxFrequency, searchMode);
            boxedTablePanel.setOutputMarkupId(true);
            getMiningRoleBasedTable().replaceWith(boxedTablePanel);
            target.appendJavaScript(getScaleScript());
            target.add(getMiningRoleBasedTable().setOutputMarkupId(true));

        } else if (getPageParameterMode().equals(RoleAnalysisProcessModeType.USER)) {

            MiningUserBasedTable boxedTablePanel = generateMiningUserBasedTable(miningRoleTypeChunks,
                    miningUserTypeChunks, false, minFrequency, intersection, maxFrequency);
            boxedTablePanel.setOutputMarkupId(true);
            getMiningUserBasedTable().replaceWith(boxedTablePanel);
            target.appendJavaScript(getScaleScript());
            target.add(getMiningUserBasedTable().setOutputMarkupId(true));
        }

    }

    public MiningUserBasedTable generateMiningUserBasedTable(List<MiningRoleTypeChunk> roles,
            List<MiningUserTypeChunk> users, boolean sortable, double frequency, DetectedPattern intersection, double maxFrequency) {
        return new MiningUserBasedTable(ID_DATATABLE, roles, users, sortable, frequency, intersection, maxFrequency, searchMode) {
            @Override
            public void resetTable(AjaxRequestTarget target) {
                updateMiningTable(target, false, searchMode, miningRoleTypeChunks, miningUserTypeChunks);
            }

            @Override
            protected String getCompressStatus() {
                return compressMode;
            }

            @Override
            protected void onPerform(AjaxRequestTarget ajaxRequestTarget) {
                if (compress) {
                    compress = false;
                    compressMode = "EXPAND MODE";
                } else {
                    compress = true;
                    compressMode = "COMPRESS MODE";
                }
                loadMiningTableData();
                updateMiningTable(ajaxRequestTarget, false, searchMode, miningRoleTypeChunks, miningUserTypeChunks);
                ajaxRequestTarget.add(this);
            }
        };
    }

    public MiningRoleBasedTable generateMiningRoleBasedTable(List<MiningRoleTypeChunk> roles,
            List<MiningUserTypeChunk> users, boolean sortable, double frequency, DetectedPattern intersection,
            double maxFrequency, RoleAnalysisSearchModeType searchMode) {
        return new MiningRoleBasedTable(ID_DATATABLE, roles, users, sortable, frequency, intersection, maxFrequency, searchMode) {
            @Override
            public void resetTable(AjaxRequestTarget target) {
                updateMiningTable(target, false, searchMode, miningRoleTypeChunks, miningUserTypeChunks);
            }

            @Override
            protected String getCompressStatus() {
                return compressMode;
            }

            @Override
            protected void onPerform(AjaxRequestTarget ajaxRequestTarget) {
                if (compress) {
                    compress = false;
                    compressMode = "EXPAND MODE";
                } else {
                    compress = true;
                    compressMode = "COMPRESS MODE";
                }
                loadMiningTableData();

                updateMiningTable(ajaxRequestTarget, false, searchMode, miningRoleTypeChunks, miningUserTypeChunks);
                ajaxRequestTarget.add(this);
            }
        };
    }

    public Component generateTableIntersection(String id, List<DetectedPattern> miningSets) {

        MiningIntersectionTable components = new MiningIntersectionTable(id, miningSets) {
            @Override
            protected void onLoad(AjaxRequestTarget ajaxRequestTarget, IModel<DetectedPattern> rowModel) {
                intersection = rowModel.getObject();

                updateMiningTable(ajaxRequestTarget, true, searchMode, miningRoleTypeChunks, miningUserTypeChunks);
                processButton.setVisible(true);
                ajaxRequestTarget.add(processButton);
            }
        };

        components.setOutputMarkupId(true);
        return components;
    }

    protected Component getIntersectionTable() {
        return get(((PageBase) getPage()).createComponentPath(ID_DATATABLE_INTERSECTIONS));
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageMiningOperation.title");
    }

    protected MiningRoleBasedTable getMiningRoleBasedTable() {
        return (MiningRoleBasedTable) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE));
    }

    protected MiningUserBasedTable getMiningUserBasedTable() {
        return (MiningUserBasedTable) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE));
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}

