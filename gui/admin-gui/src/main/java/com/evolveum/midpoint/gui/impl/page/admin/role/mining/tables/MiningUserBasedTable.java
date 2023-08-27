/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.RoleAnalysisObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.TableCellFillOperation.updateFrequencyUserBased;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.TableCellFillOperation.updateUserBasedTableData;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.Tools.applySquareTableCell;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.Tools.getScaleScript;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.MembersDetailsPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.component.data.SpecialBoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkTruncatePanelAction;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkIconPanelStatus;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class MiningUserBasedTable extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";
    OperationResult result = new OperationResult("GetObject");

    int fromCol;
    int toCol;
    int specialColumnCount;
    PrismObject<RoleAnalysisClusterType> cluster;

    MiningOperationChunk miningOperationChunk;

    public MiningUserBasedTable(String id, MiningOperationChunk miningOperationChunk, double minFrequency, DetectedPattern intersection,
            double maxFrequency, List<ObjectReferenceType> reductionObjects, RoleAnalysisSortMode roleAnalysisSortMode,
            PrismObject<RoleAnalysisClusterType> cluster) {
        super(id);

        this.cluster = cluster;
        this.miningOperationChunk = miningOperationChunk;
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

        SpecialBoxedTablePanel<MiningRoleTypeChunk> table = generateTable(provider, users, minFrequency, maxFrequency,
                intersection, reductionObjects, roleAnalysisSortMode);

        add(table);
    }

    String valueTitle = null;
    int columnPageCount = 100;

    public SpecialBoxedTablePanel<MiningRoleTypeChunk> generateTable(RoleMiningProvider<MiningRoleTypeChunk> provider,
            List<MiningUserTypeChunk> users,
            double minFrequency, double maxFrequency,
            DetectedPattern intersection, List<ObjectReferenceType> reductionObjects,
            RoleAnalysisSortMode roleAnalysisSortMode) {

        SpecialBoxedTablePanel<MiningRoleTypeChunk> table = new SpecialBoxedTablePanel<>(
                ID_DATATABLE, provider, initColumns(users, minFrequency, intersection, maxFrequency, reductionObjects),
                null, true, true, specialColumnCount, roleAnalysisSortMode) {
            @Override
            public void onChange(String value, AjaxRequestTarget target) {
                String[] rangeParts = value.split(" - ");
                valueTitle = value;
                fromCol = Integer.parseInt(rangeParts[0]);
                toCol = Integer.parseInt(rangeParts[1]);
                getTable().replaceWith(generateTable(provider, users, minFrequency, maxFrequency, intersection, reductionObjects,
                        roleAnalysisSortMode));
                target.add(getTable().setOutputMarkupId(true));
                target.appendJavaScript(getScaleScript());
            }

            @Override
            public BusinessRoleApplicationDto getOperationData() {

                OperationResult operationResult = new OperationResult("PerformPatternCreation");
                if (miningOperationChunk == null) {
                    return null;
                }

                List<AssignmentType> roleAssignments = new ArrayList<>();

                List<MiningRoleTypeChunk> simpleMiningRoleTypeChunks = miningOperationChunk.getSimpleMiningRoleTypeChunks();
                for (MiningRoleTypeChunk roleChunk : simpleMiningRoleTypeChunks) {
                    if (roleChunk.getStatus().equals(RoleAnalysisOperationMode.ADD)) {
                        for (String roleOid : roleChunk.getRoles()) {
                            PrismObject<RoleType> roleObject = getRoleTypeObject(getPageBase(), roleOid, operationResult);
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
                            PrismObject<UserType> userObject = getUserTypeObject(getPageBase(), userOid, operationResult);
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

                getTable().replaceWith(generateTable(provider, users, minFrequency, maxFrequency, intersection,
                        reductionObjects, sortMode));
                target.add(getTable().setOutputMarkupId(true));
                target.appendJavaScript(getScaleScript());
            }

            @Override
            public int onChangeSize(int value, AjaxRequestTarget target) {
                columnPageCount = value;
                fromCol = 1;
                toCol = Math.min(value, specialColumnCount);
                valueTitle = "0 - " + toCol;

                getTable().replaceWith(generateTable(provider, users, minFrequency, maxFrequency, intersection,
                        reductionObjects, roleAnalysisSortMode));
                target.add(getTable().setOutputMarkupId(true));
                target.appendJavaScript(getScaleScript());
                return value;
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

    public List<IColumn<MiningRoleTypeChunk, String>> initColumns(List<MiningUserTypeChunk> users, double minFrequency,
            DetectedPattern intersection, double maxFrequency, List<ObjectReferenceType> reductionObjects) {

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
                        for (String s : elements) {
                            objects.add(getFocusTypeObject((PageBase) getPage(), s, result));
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

                AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(componentId, new LoadableModel<>() {
                    @Override
                    protected Object load() {
                        return Model.of(getCompressStatus());
                    }
                }) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onPerform(target);
                        target.add(this);
                    }
                };
                ajaxLinkPanel.setOutputMarkupId(true);
                ajaxLinkPanel.setOutputMarkupPlaceholderTag(true);
                add(ajaxLinkPanel);

                return ajaxLinkPanel.add(
                        new AttributeAppender("style",
                                " writing-mode: vertical-lr;  -webkit-transform: rotate(45deg);"));
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
                    updateUserBasedTableData(cellItem, componentId, model, rowUsers, colUsers, intersection, colRoleAnalysisOperationMode, userChunk);
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
                                objects.add(getFocusTypeObject((PageBase) getPage(), s, result));
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
        return ((SpecialBoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    protected SpecialBoxedTablePanel<?> getTable() {
        return ((SpecialBoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

    protected void resetTable(AjaxRequestTarget target) {

    }

    protected String getCompressStatus() {
        return "COMPRESS MODE";
    }

    protected void onPerform(AjaxRequestTarget ajaxRequestTarget) {
    }

}
