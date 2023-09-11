/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.updateFrequencyUserBased;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.updateUserBasedTableData;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applySquareTableCell;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applyTableScaleScript;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
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

public class RoleAnalysisUserBasedTable extends Panel {

    private static final String ID_DATATABLE = "datatable";
    private static final String DOT_CLASS = RoleAnalysisUserBasedTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
    private final OperationResult result = new OperationResult(OP_PREPARE_OBJECTS);

    private String valueTitle = null;
    private int currentPageView = 0;

    private int columnPageCount = 100;
    private int fromCol;
    private int toCol;
    private int specialColumnCount;

    private final MiningOperationChunk miningOperationChunk;

    double minFrequency;
    double maxFrequency;
    DetectedPattern analysedPattern;
    RoleAnalysisSortMode roleAnalysisSortMode;

    public RoleAnalysisUserBasedTable(String id,
            MiningOperationChunk miningOperationChunk,
            DetectedPattern intersection,
            RoleAnalysisSortMode roleAnalysisSortMode,
            PrismObject<RoleAnalysisClusterType> cluster) {
        super(id);

        this.roleAnalysisSortMode = roleAnalysisSortMode;
        this.analysedPattern = intersection;

        RoleAnalysisClusterType clusterObject = cluster.asObjectable();

        RoleAnalysisDetectionOptionType detectionOption = clusterObject.getDetectionOption();

        RangeType frequencyRange = detectionOption.getFrequencyRange();

        if (frequencyRange != null) {
            this.minFrequency = frequencyRange.getMin() / 100;
            this.maxFrequency = frequencyRange.getMax() / 100;
        }

        this.miningOperationChunk = miningOperationChunk;

        initLayout(cluster);
    }


//    @Override
//    public void renderHead(IHeaderResponse response) {
//        response.render(OnDomReadyHeaderItem.forScript("MidPointTheme.initTableZoom('#role-mining-table');"));
//    }



    private void initLayout(PrismObject<RoleAnalysisClusterType> cluster) {

        List<ObjectReferenceType> resolvedPattern = cluster.asObjectable().getResolvedPattern();

        List<MiningUserTypeChunk> users = miningOperationChunk.getMiningUserTypeChunks(roleAnalysisSortMode);
        List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(roleAnalysisSortMode);

        fromCol = 1;
        toCol = 100;
        specialColumnCount = users.size();

        if (specialColumnCount < toCol) {
            toCol = specialColumnCount;
        }

        RoleMiningProvider<MiningRoleTypeChunk> provider = new RoleMiningProvider<>(
                this, new ListModel<>(roles) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<MiningRoleTypeChunk> object) {
                super.setObject(object);
            }
        }, false);

        RoleAnalysisTable<MiningRoleTypeChunk> table = generateTable(provider, users,
                resolvedPattern, cluster);

        add(table);
    }

    public RoleAnalysisTable<MiningRoleTypeChunk> generateTable(RoleMiningProvider<MiningRoleTypeChunk> provider,
            List<MiningUserTypeChunk> users, List<ObjectReferenceType> reductionObjects, PrismObject<RoleAnalysisClusterType> cluster) {

        RoleAnalysisTable<MiningRoleTypeChunk> table = new RoleAnalysisTable<>(
                ID_DATATABLE, provider, initColumns(users, reductionObjects),
                null, true, specialColumnCount, roleAnalysisSortMode) {

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {

                RepeatingView repeatingView = new RepeatingView(id);
                repeatingView.setOutputMarkupId(true);

                CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-search", LayeredIconCssStyle.IN_ROW_STYLE);

                AjaxCompositedIconButton createNewObjectButton = new AjaxCompositedIconButton(repeatingView.newChildId(), iconBuilder.build(),
                        createStringResource("")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        showDetectedPatternPanel(target);
                    }

                };
                createNewObjectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));

                repeatingView.add(createNewObjectButton);

                AjaxIconButton refreshIcon = new AjaxIconButton(repeatingView.newChildId(), new Model<>(GuiStyleConstants.CLASS_RECONCILE),
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
                String[] rangeParts = value.split(" - ");
                valueTitle = value;
                fromCol = Integer.parseInt(rangeParts[0]);
                toCol = Integer.parseInt(rangeParts[1]);
                getTable().replaceWith(generateTable(provider, users, reductionObjects, cluster));
                target.add(getTable().setOutputMarkupId(true));
                target.appendJavaScript(applyTableScaleScript());
            }

            @Override
            public BusinessRoleApplicationDto getOperationData() {

                if (miningOperationChunk == null) {
                    return null;
                }

                List<AssignmentType> roleAssignments = new ArrayList<>();

                List<MiningRoleTypeChunk> simpleMiningRoleTypeChunks = miningOperationChunk.getSimpleMiningRoleTypeChunks();
                for (MiningRoleTypeChunk roleChunk : simpleMiningRoleTypeChunks) {
                    if (roleChunk.getStatus().equals(RoleAnalysisOperationMode.ADD)) {
                        for (String roleOid : roleChunk.getRoles()) {
                            PrismObject<RoleType> roleObject = getRoleTypeObject(getPageBase(), roleOid, result);
                            if (roleObject != null) {
                                roleAssignments.add(ObjectTypeUtil.createAssignmentTo(roleOid, ObjectTypes.ROLE));
                            }
                        }
                    }
                }

                PrismObject<RoleType> businessRole = generateBusinessRole((PageBase) getPage(), roleAssignments, "");

                List<BusinessRoleDto> roleApplicationDtos = new ArrayList<>();

                List<MiningUserTypeChunk> simpleMiningUserTypeChunks = miningOperationChunk.getSimpleMiningUserTypeChunks();

                for (MiningUserTypeChunk userChunk : simpleMiningUserTypeChunks) {
                    if (userChunk.getStatus().equals(RoleAnalysisOperationMode.ADD)) {
                        for (String userOid : userChunk.getUsers()) {
                            PrismObject<UserType> userObject = getUserTypeObject(getPageBase(), userOid, result);
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

                List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(sortMode);
                List<MiningUserTypeChunk> users = miningOperationChunk.getMiningUserTypeChunks(sortMode);
                RoleMiningProvider<MiningRoleTypeChunk> provider = new RoleMiningProvider<>(
                        this, new ListModel<>(roles) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void setObject(List<MiningRoleTypeChunk> object) {
                        super.setObject(object);
                    }
                }, false);

                getTable().replaceWith(generateTable(provider, users,
                        reductionObjects, cluster));
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

                getTable().replaceWith(generateTable(provider, users,
                        reductionObjects, cluster));
                target.add(getTable().setOutputMarkupId(true));
                target.appendJavaScript(applyTableScaleScript());
            }

            @Override
            protected int getCurrentPage() {
                return currentPageView;
            }

            @Override
            public int getColumnPageCount() {
                return columnPageCount;
            }

            @Override
            public String getColumnPagingTitle() {
                if (valueTitle == null) {
                    return super.getColumnPagingTitle();
                } else {
                    return valueTitle;
                }
            }
        };
        table.setItemsPerPage(50);
        table.setOutputMarkupId(true);

        return table;
    }

    public List<IColumn<MiningRoleTypeChunk, String>> initColumns(List<MiningUserTypeChunk> users,
            List<ObjectReferenceType> reductionObjects) {

        List<IColumn<MiningRoleTypeChunk, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header role-mining-no-border";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<MiningRoleTypeChunk> rowModel) {
                return GuiDisplayTypeUtil
                        .createDisplayType(IconAndStylesUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
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
            public void populateItem(Item<ICellPopulator<MiningRoleTypeChunk>> item, String componentId,
                    IModel<MiningRoleTypeChunk> rowModel) {

                item.add(AttributeAppender.replace("class", " "));
                item.add(new AttributeAppender("style", " width:150px"));

                List<String> elements = rowModel.getObject().getRoles();

                for (ObjectReferenceType ref : reductionObjects) {
                    if (elements.contains(ref.getOid())) {
                        item.add(new AttributeAppender("class", " table-info"));
                        break;
                    }
                }

                updateFrequencyUserBased(rowModel, minFrequency, maxFrequency);

                String title = rowModel.getObject().getChunkName();
                AjaxLinkPanel analyzedMembersDetailsPanel = new AjaxLinkPanel(componentId,
                        createStringResource(title)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        List<PrismObject<FocusType>> objects = new ArrayList<>();
                        for (String oid : elements) {
                            objects.add(getFocusTypeObject(getPageBase(), oid, result));
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
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name role-mining-no-border";
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
            public void populateItem(Item<ICellPopulator<MiningRoleTypeChunk>> item, String componentId,
                    IModel<MiningRoleTypeChunk> rowModel) {

                item.add(AttributeAppender.replace("style", " overflow-wrap: break-word !important; word-break: inherit;"));

                LinkIconPanelStatus linkIconPanel = new LinkIconPanelStatus(componentId, new LoadableDetachableModel<>() {
                    @Override
                    protected RoleAnalysisOperationMode load() {
                        return rowModel.getObject().getStatus();
                    }
                }) {
                    @Override
                    protected RoleAnalysisOperationMode onClickPerformed(AjaxRequestTarget target, RoleAnalysisOperationMode status) {

                        RoleAnalysisOperationMode roleAnalysisOperationMode1 = rowModel.getObject().getStatus();
                        if (roleAnalysisOperationMode1.equals(RoleAnalysisOperationMode.NEUTRAL)) {
                            rowModel.getObject().setStatus(RoleAnalysisOperationMode.ADD);
                        } else if (roleAnalysisOperationMode1.equals(RoleAnalysisOperationMode.ADD)) {
                            rowModel.getObject().setStatus(RoleAnalysisOperationMode.REMOVE);
                        } else if (roleAnalysisOperationMode1.equals(RoleAnalysisOperationMode.REMOVE)) {
                            rowModel.getObject().setStatus(RoleAnalysisOperationMode.NEUTRAL);
                        }
                        resetTable(target);
                        return rowModel.getObject().getStatus();
                    }
                };

                linkIconPanel.setOutputMarkupId(true);
                item.add(linkIconPanel);
            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header role-mining-no-border";
            }
        });

        IColumn<MiningRoleTypeChunk, String> column;
        for (int i = fromCol - 1; i < toCol; i++) {
            MiningUserTypeChunk userChunk = users.get(i);
            List<String> colUsers = userChunk.getUsers();

            column = new AbstractColumn<>(createStringResource("")) {

                @Override
                public void populateItem(Item<ICellPopulator<MiningRoleTypeChunk>> cellItem,
                        String componentId, IModel<MiningRoleTypeChunk> model) {
                    applySquareTableCell(cellItem);
                    List<String> rowUsers = model.getObject().getUsers();
                    RoleAnalysisOperationMode colRoleAnalysisOperationMode = userChunk.getStatus();
                    updateUserBasedTableData(cellItem, componentId, model, rowUsers, colUsers, analysedPattern,
                            colRoleAnalysisOperationMode, userChunk);
                }

                @Override
                public Component getHeader(String componentId) {

                    List<String> elements = userChunk.getUsers();

                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            IconAndStylesUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));

                    String title = userChunk.getChunkName();
                    return new AjaxLinkTruncatePanelAction(componentId,
                            createStringResource(title), createStringResource(title), displayType,
                            new LoadableDetachableModel<>() {
                                @Override
                                protected RoleAnalysisOperationMode load() {
                                    return userChunk.getStatus();
                                }
                            }) {

                        @Override
                        protected RoleAnalysisOperationMode onClickPerformedAction(AjaxRequestTarget target, RoleAnalysisOperationMode roleAnalysisOperationMode) {
                            RoleAnalysisOperationMode roleAnalysisOperationMode1 = userChunk.getStatus();
                            if (roleAnalysisOperationMode1.equals(RoleAnalysisOperationMode.NEUTRAL)) {
                                userChunk.setStatus(RoleAnalysisOperationMode.ADD);
                            } else if (roleAnalysisOperationMode1.equals(RoleAnalysisOperationMode.ADD)) {
                                userChunk.setStatus(RoleAnalysisOperationMode.REMOVE);
                            } else if (roleAnalysisOperationMode1.equals(RoleAnalysisOperationMode.REMOVE)) {
                                userChunk.setStatus(RoleAnalysisOperationMode.NEUTRAL);
                            }

                            resetTable(target);
                            return userChunk.getStatus();
                        }

                        @Override
                        public void onClick(AjaxRequestTarget target) {

                            List<PrismObject<FocusType>> objects = new ArrayList<>();
                            for (String s : elements) {
                                objects.add(getFocusTypeObject(getPageBase(), s, result));
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

    protected RoleAnalysisTable<?> getTable() {
        return ((RoleAnalysisTable<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
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

    private void onRefresh(PrismObject<RoleAnalysisClusterType> cluster) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, cluster.getOid());
        parameters.add("panelId", "clusterDetails");
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

}
