/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import java.io.Serial;
import java.util.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCandidateRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

public class RoleAnalysisTable<B extends MiningBaseTypeChunk, A extends MiningBaseTypeChunk> extends BasePanel<RoleAnalysisObjectDto> implements Table {

    @Serial private static final long serialVersionUID = 1L;

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
        tableContainer.add(table);
        add(tableContainer);

        addHeaderToolbar(table, provider);

        add(createHeader(ID_HEADER));
        WebMarkupContainer footer = createRowsNavigation();
        footer.add(new VisibleBehaviour(() -> !hideFooterIfSinglePage() || provider.size() > getItemsPerPage()));
        add(footer);

        WebMarkupContainer footer2 = createColumnsNavigation(table);
        add(footer2);
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
         return initColumns(1, mainChunk.size());
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
            protected boolean isOutlierDetection() {
                return RoleAnalysisTable.this.isOutlierDetection();
            }

            @Override
            protected void resetTable(AjaxRequestTarget target) {
                getModelObject().recomputeChunks(getPageBase());
                RoleAnalysisTable.this.refreshTable(target);
            }

            @Override
            protected void refreshTable(AjaxRequestTarget target) {
                RoleAnalysisTable.this.refreshTable(target);
            }
        });


        IColumn<A, String> column;
        List<B> mainChunk = getModelObject().getMainMiningChunk();
        for (int i = fromCol - 1; i < toCol; i++) {

            B colChunk = mainChunk.get(i);

            column = new RoleAnalysisIntersectionColumn<>(colChunk, getModel(), getPageBase()) {

                @Override
                protected Set<String> getMarkMemberObjects() {
                    return RoleAnalysisTable.this.getMarkMemberObjects();
                }

                @Override
                protected Set<String> getMarkPropertyObjects() {
                    return RoleAnalysisTable.this.getMarkPropertyObjects();
                }


                @Override
                protected void refreshTable(AjaxRequestTarget target) {
                    RoleAnalysisTable.this.refreshTable(target);
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
        if (getModelObject().isOutlierDetection()) {
            TableHeadersToolbar<?> headersTop = new TableHeadersToolbar<>(table, provider) {

                @Override
                protected void refreshTable(AjaxRequestTarget target) {
                    super.refreshTable(target);
                    target.add(getFooter());
                }
            };

            headersTop.setOutputMarkupId(true);
            table.addTopToolbar(headersTop);
        } else {
            RoleAnalysisTableHeadersToolbar<?> headersTop = new RoleAnalysisTableHeadersToolbar<>(table, provider) {

                @Override
                protected void refreshTable(AjaxRequestTarget target) {
                    target.add(getFooter());
                    refreshTableRows(target);
                }
            };

            headersTop.setOutputMarkupId(true);
            table.addTopToolbar(headersTop);

        }
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
        return new RoleAnalysisPaginRows(RoleAnalysisTable.ID_FOOTER, ID_PAGING_FOOTER,this, new PropertyModel<>(getModel(), RoleAnalysisObjectDto.F_DISPLAY_VALUE_OPTION), getDataTable()) {

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

            protected int getColumnCount() {
                return getModelObject().getMainMiningChunk().size();
            }
        };
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
        getModelObject().recomputeChunks(getPageBase());
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
        if (candidateRole != null) {
            if (candidateRole.size() > 1) {
                return false;
            }
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

    public List<String> getCandidateRoleContainerId() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_CANDIDATE_ROLE_ID);
        if (!stringValue.isNull()) {
            String[] split = stringValue.toString().split(",");
            return Arrays.asList(split);
        }
        return null;
    }

    protected
     List<DetectedPattern> getSelectedPatterns() {
        return new ArrayList<>();
    }

    protected IModel<Map<String, String>> getColorPaletteModel() {
        return null;
    }

    public boolean isOutlierDetection() {
        return false;
    }

    protected Set<String> getMarkMemberObjects() {
        return null;
    }

    protected Set<String> getMarkPropertyObjects(){
        return null;
    }

    protected void loadDetectedPattern(AjaxRequestTarget target) {
//
////        this.isRelationSelected = false;
//        MiningOperationChunk chunk = getModelObject().getMininingOperationChunk();
//
//
//        List<MiningUserTypeChunk> users = chunk.getMiningUserTypeChunks();
//        List<MiningRoleTypeChunk> roles = chunk.getMiningRoleTypeChunks();
//
//        refreshCells(chunk.getProcessMode(), users, roles, chunk.getMinFrequency(), chunk.getMaxFrequency());
//
//
//
//        if (isPatternDetected()) {
//            Task task = getPageBase().createSimpleTask("InitPattern");
//            OperationResult result = task.getResult();
//
//            getPageBase().getRoleAnalysisService().updateChunkWithPatterns(chunk, getSelectedPatterns(), task, result);
//        }
//
//        refreshTable(target);
    }

    private boolean isPatternDetected() {
        return !getSelectedPatterns().isEmpty();
    }

}
