/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applySquareTableCell;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applyTableScaleScript;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.MembersDetailsPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.RoleAnalysisTable;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkTruncatePanelAction;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkIconPanelStatus;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleAnalysisRoleBasedTable extends BasePanel<String> {

    private static final String ID_DATATABLE = "datatable";

    private static final String DOT_CLASS = RoleAnalysisRoleBasedTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
    private final OperationResult result = new OperationResult(OP_PREPARE_OBJECTS);
    private String valueTitle = null;
    private int currentPageView = 0;
    private int columnPageCount = 100;
    private int fromCol = 1;
    private int toCol = 100;
    private int specialColumnCount;
    private final MiningOperationChunk miningOperationChunk;
    double minFrequency;
    double maxFrequency;
    DetectedPattern detectedPattern;

    RoleAnalysisSortMode roleAnalysisSortMode;

    public RoleAnalysisRoleBasedTable(String id,
            MiningOperationChunk miningOperationChunk,
            DetectedPattern detectedPattern,
            RoleAnalysisSortMode roleAnalysisSortMode,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster) {
        super(id);

        this.roleAnalysisSortMode = roleAnalysisSortMode;
        this.detectedPattern = detectedPattern;
        this.miningOperationChunk = miningOperationChunk;

        RoleAnalysisClusterType clusterObject = cluster.asObjectable();
        RoleAnalysisDetectionOptionType detectionOption = clusterObject.getDetectionOption();
        RangeType frequencyRange = detectionOption.getFrequencyRange();

        if (frequencyRange != null) {
            this.minFrequency = frequencyRange.getMin() / 100;
            this.maxFrequency = frequencyRange.getMax() / 100;
        }

        initLayout(cluster);
    }

    private void initLayout(PrismObject<RoleAnalysisClusterType> cluster) {
        List<ObjectReferenceType> resolvedPattern = cluster.asObjectable().getResolvedPattern();
        List<MiningUserTypeChunk> users = miningOperationChunk.getMiningUserTypeChunks(roleAnalysisSortMode);
        List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(roleAnalysisSortMode);

        specialColumnCount = roles.size();
        toCol = Math.min(toCol, specialColumnCount);

        if (isPatternDetected()) {
            initRoleBasedDetectionPattern(users, roles, detectedPattern, minFrequency, maxFrequency);
        }

        RoleMiningProvider<MiningUserTypeChunk> provider = createRoleMiningProvider(users);
        RoleAnalysisTable<MiningUserTypeChunk> table = generateTable(provider, roles, resolvedPattern, cluster);
        add(table);
    }

    private RoleMiningProvider<MiningUserTypeChunk> createRoleMiningProvider(List<MiningUserTypeChunk> users) {
        ListModel<MiningUserTypeChunk> model = new ListModel<>(users) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<MiningUserTypeChunk> object) {
                super.setObject(object);
            }
        };

        return new RoleMiningProvider<>(this, model, false);
    }

    public RoleAnalysisTable<MiningUserTypeChunk> generateTable(RoleMiningProvider<MiningUserTypeChunk> provider,
            List<MiningRoleTypeChunk> roles, List<ObjectReferenceType> reductionObjects,
            PrismObject<RoleAnalysisClusterType> cluster) {

        RoleAnalysisTable<MiningUserTypeChunk> table = new RoleAnalysisTable<>(
                ID_DATATABLE, provider, initColumns(roles, reductionObjects),
                null, true, specialColumnCount, roleAnalysisSortMode) {
            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {

                RepeatingView repeatingView = new RepeatingView(id);
                repeatingView.setOutputMarkupId(true);

                CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                        "fa fa-search", LayeredIconCssStyle.IN_ROW_STYLE);

                AjaxCompositedIconButton detectionPanel = new AjaxCompositedIconButton(
                        repeatingView.newChildId(), iconBuilder.build(), createStringResource("")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        showDetectedPatternPanel(target);
                    }

                };
                detectionPanel.add(AttributeAppender.append("class", "btn btn-default btn-sm"));

                repeatingView.add(detectionPanel);

                AjaxIconButton refreshIcon = new AjaxIconButton(
                        repeatingView.newChildId(),
                        new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                        createStringResource("MainObjectListPanel.refresh")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onRefresh(cluster);
                    }
                };
                refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));

                repeatingView.add(refreshIcon);

                return repeatingView;
            }

            @Override
            public void onChange(String value, AjaxRequestTarget target, int currentPage) {
                currentPageView = currentPage;
                valueTitle = value;
                String[] rangeParts = value.split(" - ");
                fromCol = Integer.parseInt(rangeParts[0]);
                toCol = Integer.parseInt(rangeParts[1]);
                getTable().replaceWith(generateTable(provider, roles, reductionObjects, cluster));
                target.add(getTable().setOutputMarkupId(true));
            }

            @Override
            public BusinessRoleApplicationDto getOperationData() {

                if (miningOperationChunk == null) {
                    return null;
                }
                Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);

                List<AssignmentType> roleAssignments = new ArrayList<>();

                List<MiningRoleTypeChunk> simpleMiningRoleTypeChunks = miningOperationChunk.getSimpleMiningRoleTypeChunks();
                for (MiningRoleTypeChunk roleChunk : simpleMiningRoleTypeChunks) {
                    if (roleChunk.getStatus().equals(RoleAnalysisOperationMode.INCLUDE)) {
                        for (String roleOid : roleChunk.getRoles()) {
                            PrismObject<RoleType> roleObject = getPageBase().getRoleAnalysisService()
                                    .getRoleTypeObject(roleOid, task, result);
                            if (roleObject != null) {
                                roleAssignments.add(ObjectTypeUtil.createAssignmentTo(roleOid, ObjectTypes.ROLE));
                            }
                        }
                    }
                }

                PrismObject<RoleType> businessRole = getPageBase().getRoleAnalysisService()
                        .generateBusinessRole(roleAssignments, PolyStringType.fromOrig(""));

                List<BusinessRoleDto> roleApplicationDtos = new ArrayList<>();

                List<MiningUserTypeChunk> simpleMiningUserTypeChunks = miningOperationChunk.getSimpleMiningUserTypeChunks();

                for (MiningUserTypeChunk userChunk : simpleMiningUserTypeChunks) {
                    if (userChunk.getStatus().equals(RoleAnalysisOperationMode.INCLUDE)) {
                        for (String userOid : userChunk.getUsers()) {
                            PrismObject<UserType> userObject = getPageBase().getRoleAnalysisService()
                                    .getUserTypeObject(userOid, task, result);
                            if (userObject != null) {
                                roleApplicationDtos.add(new BusinessRoleDto(userObject,
                                        businessRole, getPageBase()));
                            }
                        }
                    }
                }

                return new BusinessRoleApplicationDto(cluster, businessRole, roleApplicationDtos);
            }

            @Override
            public void onChangeSortMode(RoleAnalysisSortMode sortMode, AjaxRequestTarget target) {
                roleAnalysisSortMode = sortMode;
                List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(roleAnalysisSortMode);
                List<MiningUserTypeChunk> users = miningOperationChunk.getMiningUserTypeChunks(roleAnalysisSortMode);
                RoleMiningProvider<MiningUserTypeChunk> provider = new RoleMiningProvider<>(
                        this, new ListModel<>(users) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void setObject(List<MiningUserTypeChunk> object) {
                        super.setObject(object);
                    }
                }, false);

                getTable().replaceWith(generateTable(provider, roles, reductionObjects, cluster));
                target.add(getTable().setOutputMarkupId(true));
                target.appendJavaScript(applyTableScaleScript());
            }

            @Override
            public void onChangeSize(int value, AjaxRequestTarget target) {
                currentPageView = 0;
                columnPageCount = value;
                fromCol = 1;
                toCol = Math.min(value, specialColumnCount);
                valueTitle = "0 - " + toCol;

                getTable().replaceWith(generateTable(provider, roles, reductionObjects, cluster));
                target.add(getTable().setOutputMarkupId(true));
                target.appendJavaScript(applyTableScaleScript());
            }

            @Override
            public String getColumnPagingTitle() {
                if (valueTitle == null) {
                    return super.getColumnPagingTitle();
                } else {
                    return valueTitle;
                }
            }

            @Override
            protected int getCurrentPage() {
                return currentPageView;
            }

            @Override
            public int getColumnPageCount() {
                return columnPageCount;
            }

        };
        table.setItemsPerPage(50);
        table.setOutputMarkupId(true);

        return table;
    }

    public List<IColumn<MiningUserTypeChunk, String>> initColumns(List<MiningRoleTypeChunk> roles,
            List<ObjectReferenceType> reductionObjects) {

        List<IColumn<MiningUserTypeChunk, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header role-mining-no-border p-2";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<MiningUserTypeChunk> rowModel) {
                return GuiDisplayTypeUtil
                        .createDisplayType(IconAndStylesUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public String getSortProperty() {
                return UserType.F_NAME.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<MiningUserTypeChunk>> item, String componentId,
                    IModel<MiningUserTypeChunk> rowModel) {

                item.add(AttributeAppender.replace("class", " "));
                item.add(new AttributeAppender("style", " width:150px"));

                List<String> elements = rowModel.getObject().getUsers();

                updateFrequencyBased(rowModel, minFrequency, maxFrequency);

                String title = rowModel.getObject().getChunkName();
                AjaxLinkPanel analyzedMembersDetailsPanel = new AjaxLinkPanel(componentId,
                        createStringResource(title)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);

                        List<PrismObject<FocusType>> objects = new ArrayList<>();
                        for (String objectOid : elements) {
                            objects.add(getPageBase().getRoleAnalysisService()
                                    .getFocusTypeObject(objectOid, task, result));
                        }
                        MembersDetailsPanel detailsPanel = new MembersDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("Analyzed members details panel"), objects, RoleAnalysisProcessModeType.USER) {
                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);
                            }
                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                    }

                };
                analyzedMembersDetailsPanel.add(
                        AttributeAppender.replace("class", "d-inline-block text-truncate"));
                analyzedMembersDetailsPanel.add(AttributeAppender.replace("style", "width:145px"));
                analyzedMembersDetailsPanel.setOutputMarkupId(true);
                item.add(analyzedMembersDetailsPanel);
            }

            @Override
            public Component getHeader(String componentId) {

                CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-expand",
                        LayeredIconCssStyle.IN_ROW_STYLE);
                AjaxCompositedIconSubmitButton compressButton = new AjaxCompositedIconSubmitButton(componentId,
                        iconBuilder.build(),
                        new LoadableDetachableModel<>() {
                            @Override
                            protected String load() {
                                if (RoleAnalysisChunkMode.valueOf(getCompressStatus()).equals(RoleAnalysisChunkMode.COMPRESS)) {
                                    return getString("RoleMining.operation.panel.expand.button.title");
                                } else {
                                    return getString("RoleMining.operation.panel.compress.button.title");
                                }
                            }
                        }) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public CompositedIcon getIcon() {

                        CompositedIconBuilder iconBuilder;
                        if (RoleAnalysisChunkMode.valueOf(getCompressStatus()).equals(RoleAnalysisChunkMode.COMPRESS)) {
                            iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-expand",
                                    LayeredIconCssStyle.IN_ROW_STYLE);
                        } else {
                            iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-compress",
                                    LayeredIconCssStyle.IN_ROW_STYLE);
                        }

                        return iconBuilder.build();
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        onPerform(target);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        target.add(((PageBase) getPage()).getFeedbackPanel());
                    }
                };
                compressButton.titleAsLabel(true);
                compressButton.setOutputMarkupId(true);
                compressButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                compressButton.add(AttributeAppender.append("style",
                        "  writing-mode: vertical-lr;  -webkit-transform: rotate(90deg);"));

                return compressButton;
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name role-mining-no-border p-2";
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public String getSortProperty() {
                return UserType.F_NAME.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<MiningUserTypeChunk>> item, String componentId,
                    IModel<MiningUserTypeChunk> rowModel) {

                item.add(AttributeAppender.replace(
                        "style", " overflow-wrap: break-word !important; word-break: inherit;"));

                LinkIconPanelStatus linkIconPanel = new LinkIconPanelStatus(componentId, new LoadableDetachableModel<>() {
                    @Override
                    protected RoleAnalysisOperationMode load() {
                        return rowModel.getObject().getStatus();
                    }
                }) {
                    @Override
                    protected RoleAnalysisOperationMode onClickPerformed(
                            AjaxRequestTarget target,
                            RoleAnalysisOperationMode status) {
                        rowModel.getObject().setStatus(status.toggleStatus());
                        target.add(getTable().setOutputMarkupId(true));
                        return rowModel.getObject().getStatus();
                    }
                };

                linkIconPanel.setOutputMarkupId(true);
                item.add(linkIconPanel);
            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header role-mining-no-border p-2";
            }
        });

        IColumn<MiningUserTypeChunk, String> column;
        for (int i = fromCol - 1; i < toCol; i++) {
            MiningRoleTypeChunk roleChunk = roles.get(i);

            column = new AbstractColumn<>(createStringResource("")) {

                @Override
                public void populateItem(Item<ICellPopulator<MiningUserTypeChunk>> cellItem,
                        String componentId, IModel<MiningUserTypeChunk> model) {
                    applySquareTableCell(cellItem);

                    String cellColor = resolveCellColor(model.getObject(), roleChunk);
                    updateCellMiningStatus(cellItem, componentId, cellColor);

                }

                @Override
                public String getCssClass() {
                    return " p-2";
                }

                @Override
                public Component getHeader(String componentId) {

                    List<String> roles = roleChunk.getRoles();

                    String defaultBlackIcon = IconAndStylesUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE);
                    CompositedIconBuilder compositedIconBuilder = new CompositedIconBuilder().setBasicIcon(defaultBlackIcon,
                            LayeredIconCssStyle.IN_ROW_STYLE);

                    for (ObjectReferenceType ref : reductionObjects) {
                        if (roles.contains(ref.getOid())) {
                            compositedIconBuilder.setBasicIcon(defaultBlackIcon + " " + GuiStyleConstants.GREEN_COLOR,
                                    LayeredIconCssStyle.IN_ROW_STYLE);
                            IconType icon = new IconType();
                            icon.setCssClass(GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_SUCCESS_COLORED
                                    + " " + GuiStyleConstants.GREEN_COLOR);
                            compositedIconBuilder.appendLayerIcon(icon, IconCssStyle.BOTTOM_RIGHT_FOR_COLUMN_STYLE);
                            break;
                        }
                    }

                    CompositedIcon compositedIcon = compositedIconBuilder.build();

                    String title = roleChunk.getChunkName();

                    return new AjaxLinkTruncatePanelAction(componentId,
                            createStringResource(title), createStringResource(title), compositedIcon,
                            new LoadableDetachableModel<>() {
                                @Override
                                protected RoleAnalysisOperationMode load() {
                                    return roleChunk.getStatus();
                                }
                            }) {

                        @Override
                        protected RoleAnalysisOperationMode onClickPerformedAction(
                                AjaxRequestTarget target,
                                RoleAnalysisOperationMode status) {
                            roleChunk.setStatus(status.toggleStatus());
                            target.add(getTable().setOutputMarkupId(true));
                            return roleChunk.getStatus();
                        }

                        @Override
                        public void onClick(AjaxRequestTarget target) {

                            Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);

                            List<PrismObject<FocusType>> objects = new ArrayList<>();
                            for (String objectOid : roles) {
                                objects.add(getPageBase().getRoleAnalysisService()
                                        .getFocusTypeObject(objectOid, task, result));
                            }
                            MembersDetailsPanel detailsPanel = new MembersDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                    Model.of("Analyzed members details panel"), objects, RoleAnalysisProcessModeType.ROLE) {
                                @Override
                                public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                    super.onClose(ajaxRequestTarget);
                                }
                            };
                            ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                        }

                    };
                }

            };
            columns.add(column);

        }

        return columns;
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    public DataTable<?, ?> getDataTable() {
        return ((RoleAnalysisTable<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    protected RoleAnalysisTable<MiningOperationChunk> getTable() {
        return ((RoleAnalysisTable<MiningOperationChunk>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

    protected void resetTable(AjaxRequestTarget target) {

    }

    protected String getCompressStatus() {
        return "COMPRESS MODE";
    }

    protected void onPerform(AjaxRequestTarget ajaxRequestTarget) {
    }

    protected void showDetectedPatternPanel(AjaxRequestTarget target) {

    }

    public void loadDetectedPattern(AjaxRequestTarget target, DetectedPattern detectedPattern) {
        this.detectedPattern = detectedPattern;
        List<MiningUserTypeChunk> users = miningOperationChunk.getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
        List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);

        if (isPatternDetected()) {
            initRoleBasedDetectionPattern(users, roles, this.detectedPattern, minFrequency, maxFrequency);
        }

        target.add(getTable().setOutputMarkupId(true));
    }

    private void onRefresh(PrismObject<RoleAnalysisClusterType> cluster) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, cluster.getOid());
        parameters.add("panelId", "clusterDetails");
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    private boolean isPatternDetected() {
        return detectedPattern != null && detectedPattern.getUsers() != null;
    }

    public RoleAnalysisSortMode getRoleAnalysisSortMode() {
        return roleAnalysisSortMode;
    }

}
