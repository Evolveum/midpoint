/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getFocusTypeObject;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.TableCellFillOperation.updateFrequencyUserBased;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.TableCellFillOperation.updateUserBasedTableData;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.data.SpecialBoxedTablePanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.details.objects.MembersDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection.DetectedPattern;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.MiningUserTypeChunk;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkTruncatePanelAction;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkIconPanelStatus;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;

public class MiningUserBasedTable extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";
    OperationResult result = new OperationResult("GetObject");

    int fromCol;
    int toCol;
    int specialColumnCount;

    public MiningUserBasedTable(String id, List<MiningRoleTypeChunk> roles,
            List<MiningUserTypeChunk> users, boolean sortable, double frequency, DetectedPattern intersection,
            double maxFrequency, RoleAnalysisSearchModeType searchMode) {
        super(id);

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
        }, sortable);

        if (sortable) {
            provider.setSort(UserType.F_NAME.toString(), SortOrder.ASCENDING);
        }

        SpecialBoxedTablePanel<MiningRoleTypeChunk> table = generateTable(provider, users, frequency,
                intersection, maxFrequency, searchMode);

        add(table);
    }

    String valueTitle = null;
    int columnPageCount = 100;

    public SpecialBoxedTablePanel<MiningRoleTypeChunk> generateTable(RoleMiningProvider<MiningRoleTypeChunk> provider,
            List<MiningUserTypeChunk> users, double frequency, DetectedPattern intersection,
            double maxFrequency, RoleAnalysisSearchModeType searchMode) {

        SpecialBoxedTablePanel<MiningRoleTypeChunk> table = new SpecialBoxedTablePanel<>(
                ID_DATATABLE, provider, initColumns(users, frequency, intersection, maxFrequency, searchMode),
                null, true, true, specialColumnCount) {
            @Override
            public void onChange(String value, AjaxRequestTarget target) {
                String[] rangeParts = value.split(" - ");
                valueTitle = value;
                fromCol = Integer.parseInt(rangeParts[0]);
                toCol = Integer.parseInt(rangeParts[1]);
                getTable().replaceWith(generateTable(provider, users, frequency, intersection, maxFrequency, searchMode));
                target.add(getTable().setOutputMarkupId(true));
                target.appendJavaScript(getScaleScript());
            }

            @Override
            public int onChangeSize(int value, AjaxRequestTarget target) {
                columnPageCount = value;
                fromCol = 1;
                toCol = Math.min(value, specialColumnCount);
                valueTitle = "0 - " + toCol;

                getTable().replaceWith(generateTable(provider, users, frequency, intersection, maxFrequency, searchMode));
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
        table.setItemsPerPage(100);
        table.setOutputMarkupId(true);

        return table;
    }

    public List<IColumn<MiningRoleTypeChunk, String>> initColumns(List<MiningUserTypeChunk> users, double minFrequency,
            DetectedPattern intersection, double maxFrequency, RoleAnalysisSearchModeType searchMode) {

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
                        .createDisplayType(WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
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
                                Model.of("Analyzed members details panel"), objects, "ROLE") {
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
                    protected ClusterObjectUtils.Status load() {
                        return rowModel.getObject().getStatus();
                    }
                }) {
                    @Override
                    protected ClusterObjectUtils.Status onClickPerformed(AjaxRequestTarget target, ClusterObjectUtils.Status status) {

                        ClusterObjectUtils.Status status1 = rowModel.getObject().getStatus();
                        if (status1.equals(ClusterObjectUtils.Status.NEUTRAL)) {
                            rowModel.getObject().setStatus(ClusterObjectUtils.Status.ADD);
                        } else if (status1.equals(ClusterObjectUtils.Status.ADD)) {
                            rowModel.getObject().setStatus(ClusterObjectUtils.Status.REMOVE);
                        } else if (status1.equals(ClusterObjectUtils.Status.REMOVE)) {
                            rowModel.getObject().setStatus(ClusterObjectUtils.Status.NEUTRAL);
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
                    tableStyle(cellItem);
                    List<String> rowUsers = model.getObject().getUsers();
                    ClusterObjectUtils.Status colStatus = userChunk.getStatus();
                    updateUserBasedTableData(cellItem, componentId, model, rowUsers, searchMode, colUsers, intersection, colStatus, userChunk);
                }

                @Override
                public Component getHeader(String componentId) {

                    List<String> elements = userChunk.getUsers();

                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));

                    String title = userChunk.getChunkName();
                    return new AjaxLinkTruncatePanelAction(componentId,
                            createStringResource(title), createStringResource(title), displayType,
                            new LoadableDetachableModel<>() {
                                @Override
                                protected ClusterObjectUtils.Status load() {
                                    return userChunk.getStatus();
                                }
                            }) {

                        @Override
                        protected ClusterObjectUtils.Status onClickPerformedAction(AjaxRequestTarget target, ClusterObjectUtils.Status status) {
                            ClusterObjectUtils.Status status1 = userChunk.getStatus();
                            if (status1.equals(ClusterObjectUtils.Status.NEUTRAL)) {
                                userChunk.setStatus(ClusterObjectUtils.Status.ADD);
                            } else if (status1.equals(ClusterObjectUtils.Status.ADD)) {
                                userChunk.setStatus(ClusterObjectUtils.Status.REMOVE);
                            } else if (status1.equals(ClusterObjectUtils.Status.REMOVE)) {
                                userChunk.setStatus(ClusterObjectUtils.Status.NEUTRAL);
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
                                    Model.of("Analyzed members details panel"), objects, "ROLE") {
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
