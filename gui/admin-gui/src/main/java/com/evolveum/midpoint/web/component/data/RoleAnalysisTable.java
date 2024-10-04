/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.gui.api.page.PageBase;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.data.column.RoleAnalysisIntersectionColumn;
import com.evolveum.midpoint.web.component.data.column.RoleAnalysisObjectColumn;
import com.evolveum.midpoint.web.component.data.mining.RoleAnalysisPaginRows;
import com.evolveum.midpoint.web.component.data.mining.RoleAnalysisPagingColumns;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.object.RoleAnalysisObjectUtils.executeChangesOnCandidateRole;

public class RoleAnalysisTable<B extends MiningBaseTypeChunk, A extends MiningBaseTypeChunk> extends BasePanel<RoleAnalysisObjectDto> implements Table {

    @Serial private static final long serialVersionUID = 1L;
    private static final String DOT_CLASS = RoleAnalysisTable.class.getName() + ".";
    private static final String OP_PROCESS_CANDIDATE_ROLE = DOT_CLASS + "processCandidate";

    public static final String PARAM_CANDIDATE_ROLE_ID = "candidateRoleId";
    public static final String PARAM_TABLE_SETTING = "tableSetting";

    private static final String ID_HEADER_FOOTER = "headerFooter";
    private static final String ID_HEADER_PAGING = "pagingFooterHeader";
    private static final String ID_HEADER = "header";
    private static final String ID_FOOTER = "footer";
    private static final String ID_TABLE = "table";
    private static final String ID_TABLE_CONTAINER = "tableContainer";

    private static final String ID_PAGING_FOOTER = "pagingFooter";

    private String additionalBoxCssClasses = null;

    //TODO what is this?
    boolean isRelationSelected = false;
    int mainChunkSize = 0;

    public RoleAnalysisTable(String id, LoadableModel<RoleAnalysisObjectDto> miningOperationChunk) {
        super(id, miningOperationChunk);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(OnDomReadyHeaderItem
                .forScript("MidPointTheme.initResponsiveTable(); MidPointTheme.initScaleResize('#tableScaleContainer');"));
    }

    private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer tableContainer = new WebMarkupContainer(ID_TABLE_CONTAINER);
        tableContainer.setOutputMarkupId(true);

        RoleMiningProvider<A> provider = createRoleMiningProvider();

        DataTable<A, String> table = new SelectableDataTable<>(ID_TABLE, initColumns(), provider, getItemsPerPage(null)) { //TODO tableId
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected Item<A> newRowItem(String id, int index, IModel<A> rowModel) {
                Item<A> item = super.newRowItem(id, index, rowModel);
                return customizeNewRowItem(item);
            }

        };
        table.setOutputMarkupId(true);
        table.setItemsPerPage(50);
        tableContainer.add(table);
        add(tableContainer);

        add(new Behavior() {
            @Override
            public void onConfigure(Component component) {
                initAdditionalChunkIfRequired();
                super.onConfigure(component);
            }
        });

        addHeaderToolbar(table, provider);

        add(createHeader(ID_HEADER));
        WebMarkupContainer footer = createRowsNavigation();
        footer.add(new VisibleBehaviour(() -> !hideFooterIfSinglePage() || provider.size() > getItemsPerPage()));
        add(footer);

        WebMarkupContainer footer2 = createColumnsNavigation(table);
        add(footer2);
    }

    //TODO check

    /**
     * Checks if the size of the main mining chunk has changed and if so, updates the data table columns accordingly.
     * This method is used to ensure that the data table columns are always in sync with the main mining chunk.
     * If the size of the main mining chunk has changed, it clears the existing data table columns and replaces them with new ones.
     * The new columns are initialized based on the updated size of the main mining chunk.
     * The data table is then replaced with a new one that has the updated columns and is marked for output in the next Ajax response.
     * This method uses the AjaxRequestTarget to perform the update operation if it is available.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void initAdditionalChunkIfRequired() {
        if (mainChunkSize != getModelObject().getMainMiningChunk().size()) {
            mainChunkSize = getModelObject().getMainMiningChunk().size();
            Optional<AjaxRequestTarget> ajaxRequestTarget = RequestCycle.get().find(AjaxRequestTarget.class);
            ajaxRequestTarget.ifPresent(target -> {
                var columns = initColumns(1, mainChunkSize);
                getDataTable().getColumns().clear();
                getDataTable().getColumns().addAll((List) columns);
                getDataTable().replaceWith(getDataTable().setOutputMarkupId(true));
            });
        }
    }

    private @NotNull RoleMiningProvider<A> createRoleMiningProvider() {

        ListModel<A> model = new ListModel<>() {

            @Override
            public List<A> getObject() {
                return getModelObject().getAdditionalMiningChunk();
            }
        };

        return new RoleMiningProvider<>(this, model, false);
    }

    private List<IColumn<A, String>> initColumns() {
        List<B> mainChunk = getModelObject().getMainMiningChunk();
        mainChunkSize = mainChunk.size();
        return initColumns(1, mainChunkSize);
    }

    public List<IColumn<A, String>> initColumns(int fromCol, long toCol) {
        List<IColumn<A, String>> columns = new ArrayList<>();
        columns.add(new RoleAnalysisObjectColumn<>(getModel(), getPageBase()) {

            @Override
            protected void setRelationSelected(boolean isRelationSelected) {
                RoleAnalysisTable.this.isRelationSelected = isRelationSelected;
            }

            @Override
            protected List<DetectedPattern> getSelectedPatterns() {
                return RoleAnalysisTable.this.getSelectedPatterns();
            }

            @Override
            protected void resetTable(AjaxRequestTarget target) {
                getModelObject().recomputeChunks(RoleAnalysisTable.this.getSelectedPatterns(), getPageBase());
                RoleAnalysisTable.this.refreshTable(target);
            }

            @Override
            protected void refreshTable(AjaxRequestTarget target) {
                RoleAnalysisTable.this.refreshTable(target);
            }

            @Override
            protected void refreshTableRows(AjaxRequestTarget target) {
                RoleAnalysisTable.this.refreshTableRows(target);
            }
        });

        IColumn<A, String> column;
        List<B> mainChunk = getModelObject().getMainMiningChunk();
        for (int i = fromCol - 1; i < toCol; i++) {

            B colChunk = mainChunk.get(i);

            column = new RoleAnalysisIntersectionColumn<>(colChunk, getModel(), getPageBase()) {

//                @Override
//                protected Set<String> getMarkPropertyObjects() {
//                    return RoleAnalysisTable.this.getMarkPropertyObjects();
//                }

                @Override
                protected void onUniquePatternDetectionPerform(AjaxRequestTarget target) {
                    RoleAnalysisTable.this.onUniquePatternDetectionPerform(target);
                }

                @Override
                protected void refreshTable(AjaxRequestTarget target) {
                    RoleAnalysisTable.this.refreshTable(target);
                }

                @Override
                protected void refreshTableRows(AjaxRequestTarget target) {
                    RoleAnalysisTable.this.refreshTableRows(target);
                }

                @Override
                protected void loadDetectedPattern(AjaxRequestTarget target) {
                    RoleAnalysisTable.this.loadDetectedPattern(target);
                }

                @Override
                protected void setRelationSelected(boolean isRelationSelected) {
                    RoleAnalysisTable.this.isRelationSelected = isRelationSelected;
                }

                @Override
                protected IModel<Map<String, String>> getColorPaletteModel() {
                    return RoleAnalysisTable.this.getColorPaletteModel();
                }

                @Override
                protected List<DetectedPattern> getSelectedPatterns() {
                    return RoleAnalysisTable.this.getSelectedPatterns();
                }
            };

            columns.add(column);
        }

        return columns;
    }

    private void addHeaderToolbar(DataTable<A, String> table, ISortableDataProvider<A, ?> provider) {
        RoleAnalysisTableHeadersToolbar<?> headersTop = new RoleAnalysisTableHeadersToolbar<>(table, provider) {

            @Override
            protected void refreshTable(AjaxRequestTarget target) {
                refreshTableRows(target);
            }
        };

        headersTop.setOutputMarkupId(true);
        table.addTopToolbar(headersTop);

//        }
    }

    public String getAdditionalBoxCssClasses() {
        return additionalBoxCssClasses;
    }

    public void setAdditionalBoxCssClasses(String boxCssClasses) {
        this.additionalBoxCssClasses = boxCssClasses;
    }

    protected Item<A> customizeNewRowItem(Item<A> item) {
        return item;
    }

    protected boolean hideFooterIfSinglePage() {
        return false;
    }

    @Override
    public DataTable<?, ?> getDataTable() {
        return (DataTable<?, ?>) get(ID_TABLE_CONTAINER).get(ID_TABLE);
    }

    @Override
    public UserProfileStorage.TableId getTableId() {
        //TODO  return UserProfileStorage.ROL;
        return null;
    }

    @Override
    public boolean enableSavePageSize() {
        return true;
    }

    @Override
    public void setItemsPerPage(int size) {
        getDataTable().setItemsPerPage(size);
    }

    @Override
    public int getItemsPerPage() {
        return (int) getDataTable().getItemsPerPage();
    }

    private int getItemsPerPage(UserProfileStorage.TableId tableId) {
        if (tableId == null) {
            return UserProfileStorage.DEFAULT_PAGING_SIZE;
        }
        MidPointAuthWebSession session = getSession();
        UserProfileStorage userProfile = session.getSessionStorage().getUserProfile();
        return userProfile.getPagingSize(tableId);
    }

    @Override
    public void setShowPaging(boolean show) {
        if (!show) {
            setItemsPerPage(Integer.MAX_VALUE);
        } else {
            setItemsPerPage(UserProfileStorage.DEFAULT_PAGING_SIZE);
            if (!getModelObject().isOutlierDetection()) {
                setItemsPerPage(100);
            }
        }
    }

    public WebMarkupContainer getHeader() {
        return (WebMarkupContainer) get(ID_HEADER);
    }

    public WebMarkupContainer getFooter() {
        return (WebMarkupContainer) get(ID_FOOTER);
    }

    protected Component createHeader(String headerId) {
        WebMarkupContainer header = new WebMarkupContainer(headerId);
        header.setVisible(false);
        header.setOutputMarkupId(true);
        return header;
    }

    protected WebMarkupContainer createRowsNavigation() {
        return new RoleAnalysisPaginRows(RoleAnalysisTable.ID_FOOTER, ID_PAGING_FOOTER, this, new PropertyModel<>(getModel(), RoleAnalysisObjectDto.F_DISPLAY_VALUE_OPTION), getDataTable()) {

            @Override
            protected boolean isPagingVisible() {
                return RoleAnalysisTable.this.isPagingVisible();
            }

            @Override
            protected void refreshTableRows(AjaxRequestTarget target) {
                RoleAnalysisTable.this.refreshTableRows(target);
            }

            @Override
            protected void resetTable(AjaxRequestTarget target) {
                RoleAnalysisTable.this.resetTable(target);
            }
        };
    }

    protected WebMarkupContainer createColumnsNavigation(DataTable<A, String> table) {
        return new RoleAnalysisPagingColumns(RoleAnalysisTable.ID_HEADER_FOOTER, ID_HEADER_PAGING, table, this) {

            @Override
            protected boolean isPagingVisible() {
                return RoleAnalysisTable.this.isPagingVisible();
            }

            @Override
            protected boolean getMigrationButtonVisibility() {
                return RoleAnalysisTable.this.getMigrationButtonVisibility();
            }

            @Override
            protected void refreshTable(long fromCol, long toCol, AjaxRequestTarget target) {
                RoleAnalysisTable.this.refreshTable(target);
            }

            @Override
            protected void onSubmitEditButton(AjaxRequestTarget target) {
                RoleAnalysisTable.this.processCandidateRole(target);
            }

            @Override
            protected @Nullable List<DetectedPattern> getSelectedPatterns() {
                return RoleAnalysisTable.this.getSelectedPatterns();
            }

            @Override
            protected int getColumnCount() {
                return getModelObject().getMainMiningChunk().size();
            }
        };
    }

    private void processCandidateRole(AjaxRequestTarget target) {
        RoleAnalysisObjectDto modelObject = getModelObject();
        MiningOperationChunk chunk = modelObject.getMininingOperationChunk();
        if (chunk == null) {
            warn(createStringResource("RoleAnalysis.candidate.not.selected").getString());
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        Task task = getPageBase().createSimpleTask(OP_PROCESS_CANDIDATE_ROLE);
        OperationResult result = task.getResult();

//        MiningOperationChunk chunk = miningOperationChunk.getObject();

        Set<PrismObject<RoleType>> candidateInducements = new HashSet<>();
        fillCandidateList(RoleType.class, candidateInducements, chunk.getMiningRoleTypeChunks(), task, result);

        Set<PrismObject<UserType>> candidateMembers = new HashSet<>();
        fillCandidateList(UserType.class, candidateMembers, chunk.getMiningUserTypeChunks(), task, result);

        RoleAnalysisClusterType cluster = modelObject.getCluster();
        Set<RoleAnalysisCandidateRoleType> candidateRoleToPerform = getCandidateRoleToPerform(cluster);
        if (candidateRoleToPerform != null) {
            @Nullable List<RoleAnalysisCandidateRoleType> candidateRole = new ArrayList<>(candidateRoleToPerform);
            if (candidateRole.size() == 1) {
                PageBase pageBase = getPageBase();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

                Set<AssignmentType> assignmentTypeSet = candidateInducements.stream()
                        .map(candidateInducement -> ObjectTypeUtil.createAssignmentTo(candidateInducement.getOid(), ObjectTypes.ROLE))
                        .collect(Collectors.toSet());

                executeChangesOnCandidateRole(roleAnalysisService, pageBase, target,
                        cluster.asPrismObject(),
                        candidateRole,
                        candidateMembers,
                        assignmentTypeSet,
                        task,
                        result
                );

                result.computeStatus();
                getPageBase().showResult(result);
                navigateToClusterCandidateRolePanel(cluster);
                return;
            }
        }

        PrismObject<RoleType> businessRole = new RoleType().asPrismObject();

        List<BusinessRoleDto> roleApplicationDtos = new ArrayList<>();

        for (PrismObject<UserType> member : candidateMembers) {
            BusinessRoleDto businessRoleDto = new BusinessRoleDto(member,
                    businessRole, candidateInducements, getPageBase());
            roleApplicationDtos.add(businessRoleDto);
        }

        BusinessRoleApplicationDto operationData = new BusinessRoleApplicationDto(
                cluster.asPrismObject(), businessRole, roleApplicationDtos, candidateInducements);

        if (!getSelectedPatterns().isEmpty() && getSelectedPatterns().get(0).getId() != null) {
            operationData.setPatternId(getSelectedPatterns().get(0).getId());
        }

        List<BusinessRoleDto> businessRoleDtos = operationData.getBusinessRoleDtos();
        Set<PrismObject<RoleType>> inducement = operationData.getCandidateRoles();
        if (!inducement.isEmpty() && !businessRoleDtos.isEmpty()) {
            PrismObject<RoleType> roleToCreate = operationData.getBusinessRole();
            PageRole pageRole = new PageRole(roleToCreate, operationData);
            setResponsePage(pageRole);
        } else {
            warn(createStringResource("RoleAnalysis.candidate.not.selected").getString());
            target.add(getPageBase().getFeedbackPanel());
        }
    }

    private @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRoleToPerform(RoleAnalysisClusterType cluster) {
        if (getSelectedPatterns().size() > 1) {
            return null;
        } else if (getSelectedPatterns().size() == 1) {
            DetectedPattern detectedPattern = getSelectedPatterns().get(0);
            Long id = detectedPattern.getId();
            List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();
            for (RoleAnalysisCandidateRoleType candidateRole : candidateRoles) {
                if (candidateRole.getId().equals(id)) {
                    return Collections.singleton(candidateRole);
                }
            }
        }

        return getCandidateRole();
    }

    private <F extends FocusType, CH extends MiningBaseTypeChunk> void fillCandidateList(Class<F> type,
            Set<PrismObject<F>> candidateList,
            List<CH> miningSimpleChunk,
            Task task,
            OperationResult result) {
        for (CH chunk : miningSimpleChunk) {
            if (!chunk.getStatus().equals(RoleAnalysisOperationMode.INCLUDE)) {
                continue;
            }
            List<String> members = RoleType.class.equals(type) ? chunk.getRoles() : chunk.getUsers();
            for (String memberOid : members) {
                PrismObject<F> roleObject = WebModelServiceUtils.loadObject(type, memberOid, getPageBase(), task, result);
                if (roleObject != null) {
                    candidateList.add(roleObject);
                }
            }
        }
    }

    private void navigateToClusterCandidateRolePanel(@NotNull RoleAnalysisClusterType cluster) {
        PageParameters parameters = new PageParameters();
        String clusterOid = cluster.getOid();
        parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        parameters.add("panelId", "candidateRoles");
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil.getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    protected boolean isPagingVisible() {
        return true;
    }

    protected String getPaginationCssClass() {
        return "pagination-sm";
    }

    @Override
    public void setCurrentPage(ObjectPaging paging) {
        WebComponentUtil.setCurrentPage(this, paging);
    }

    @Override
    public void setCurrentPage(long page) {
        getDataTable().setCurrentPage(page);
    }

    protected void resetTable(AjaxRequestTarget target) {
        //getModel().reset(); //TODO
        getModelObject().recomputeChunks(getSelectedPatterns(), getPageBase());
        refreshTable(target);
    }

    protected void refreshTableRows(AjaxRequestTarget target) {
        target.add(RoleAnalysisTable.this);
    }

    protected void refreshTable(AjaxRequestTarget target) {
        var columns = initColumns();
        refresh(columns, target);
    }

    protected void refreshTable(int fromCol, int toCol, AjaxRequestTarget target) {
        var columns = initColumns(fromCol, toCol);
        refresh(columns, target);
    }

    private void refresh(List<IColumn<A, String>> columns, AjaxRequestTarget target) {
        getDataTable().getColumns().clear();
        getDataTable().getColumns().addAll((List) columns);
        target.add(RoleAnalysisTable.this);
    }

    protected boolean getMigrationButtonVisibility() {
        Set<RoleAnalysisCandidateRoleType> candidateRole = getCandidateRole();
        if (candidateRole != null && candidateRole.size() > 1) {
            return false;
        }

        if (getSelectedPatterns().size() > 1) {
            return false;
        }

        return isRelationSelected;
    }

    private @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRole() {
        List<String> candidateRoleContainerId = getCandidateRoleContainerId();

        Set<RoleAnalysisCandidateRoleType> candidateRoleTypes = new HashSet<>();
        if (candidateRoleContainerId != null && !candidateRoleContainerId.isEmpty()) {
            RoleAnalysisClusterType clusterType = getModelObject().getCluster();
            List<RoleAnalysisCandidateRoleType> candidateRoles = clusterType.getCandidateRoles();

            for (RoleAnalysisCandidateRoleType candidateRole : candidateRoles) {
                if (candidateRoleContainerId.contains(candidateRole.getId().toString())) {
                    candidateRoleTypes.add(candidateRole);
                }
            }
            if (!candidateRoleTypes.isEmpty()) {
                return candidateRoleTypes;
            }
            return null;
        }
        return null;

    }

    public @Nullable List<String> getCandidateRoleContainerId() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_CANDIDATE_ROLE_ID);
        if (!stringValue.isNull()) {
            String[] split = stringValue.toString().split(",");
            return Arrays.asList(split);
        }
        return null;
    }

    protected List<DetectedPattern> getSelectedPatterns() {
        return new ArrayList<>();
    }

    protected IModel<Map<String, String>> getColorPaletteModel() {
        return new LoadableModel<>(false) {

            @Override
            protected Map<String, String> load() {
                return new HashMap<>();
            }
        };
    }

//    protected Set<String> getMarkPropertyObjects(){
//        return null;
//    }

    protected void loadDetectedPattern(AjaxRequestTarget target) {
        //override in subclass
    }

    //TODO check. When pattern is detected during user-permission table manipulation,
    // it is necessary to refresh operation panel, because new discovered
    // pattern is not part of the current operation panel model (TBD include or not).
    protected void onUniquePatternDetectionPerform(AjaxRequestTarget target) {
        //override in subclass
    }

}
