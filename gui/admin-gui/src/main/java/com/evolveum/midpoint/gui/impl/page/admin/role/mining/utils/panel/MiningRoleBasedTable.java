/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getFocusTypeObject;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.panel.TableCellFillOperation.updateFrequencyRoleBased;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.panel.TableCellFillOperation.updateRoleBasedTableData;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.IntersectionObject;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
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
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.MembersDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningUserTypeChunk;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkTruncatePanelAction;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkIconPanelStatus;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class MiningRoleBasedTable extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";
    OperationResult result = new OperationResult("GetObject");

    public MiningRoleBasedTable(String id,
            List<MiningRoleTypeChunk> roles, List<MiningUserTypeChunk> users, boolean sortable, double frequency, IntersectionObject intersection, double maxFrequency, ClusterObjectUtils.SearchMode searchMode) {
        super(id);
        RoleMiningProvider<MiningUserTypeChunk> provider = new RoleMiningProvider<>(
                this, new ListModel<>(users) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<MiningUserTypeChunk> object) {
                super.setObject(object);
            }
        }, sortable);

        if (sortable) {
            provider.setSort(UserType.F_NAME.toString(), SortOrder.ASCENDING);
        }

        BoxedTablePanel<MiningUserTypeChunk> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumns(roles, frequency, intersection, maxFrequency, searchMode),
                null, true, true);
        table.setItemsPerPage(100);
        table.setOutputMarkupId(true);
        add(table);
    }

    public List<IColumn<MiningUserTypeChunk, String>> initColumns(List<MiningRoleTypeChunk> roles, double minFrequency,
            IntersectionObject intersection, double maxFrequency, ClusterObjectUtils.SearchMode searchMode) {

        List<IColumn<MiningUserTypeChunk, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header role-mining-no-border";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<MiningUserTypeChunk> rowModel) {
                return GuiDisplayTypeUtil
                        .createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(createStringResource("")) {

            @Override
            public String getSortProperty() {
                return UserType.F_NAME.getLocalPart();
            }

            @Override
            public IModel<?> getDataModel(IModel<MiningUserTypeChunk> iModel) {
                return null;
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

                updateFrequencyRoleBased(rowModel, minFrequency, maxFrequency);

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
                                Model.of("Analyzed members details panel"), objects, "USER") {
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
                                "  writing-mode: vertical-lr;  -webkit-transform: rotate(45deg);"));
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
            public void populateItem(Item<ICellPopulator<MiningUserTypeChunk>> item, String componentId,
                    IModel<MiningUserTypeChunk> rowModel) {

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

        IColumn<MiningUserTypeChunk, String> column;
        for (MiningRoleTypeChunk roleChunk : roles) {
            List<String> colRoles = roleChunk.getRoles();

            column = new AbstractColumn<>(createStringResource("")) {

                @Override
                public void populateItem(Item<ICellPopulator<MiningUserTypeChunk>> cellItem,
                        String componentId, IModel<MiningUserTypeChunk> model) {
                    tableStyle(cellItem);
                    List<String> rowRoles = model.getObject().getRoles();
                    ClusterObjectUtils.Status colStatus = roleChunk.getStatus();
                    updateRoleBasedTableData(cellItem, componentId, model, rowRoles, searchMode,
                            colStatus, colRoles, intersection, roleChunk);

                }

                @Override
                public Component getHeader(String componentId) {

                    List<String> elements = roleChunk.getRoles();

                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));

                    String title = roleChunk.getChunkName();

                    return new AjaxLinkTruncatePanelAction(componentId,
                            createStringResource(title), createStringResource(title), displayType,
                            new LoadableDetachableModel<>() {
                                @Override
                                protected ClusterObjectUtils.Status load() {
                                    return roleChunk.getStatus();
                                }
                            }) {

                        @Override
                        protected ClusterObjectUtils.Status onClickPerformedAction(AjaxRequestTarget target, ClusterObjectUtils.Status status) {
                            ClusterObjectUtils.Status status1 = roleChunk.getStatus();
                            if (status1.equals(ClusterObjectUtils.Status.NEUTRAL)) {
                                roleChunk.setStatus(ClusterObjectUtils.Status.ADD);
                            } else if (status1.equals(ClusterObjectUtils.Status.ADD)) {
                                roleChunk.setStatus(ClusterObjectUtils.Status.REMOVE);
                            } else if (status1.equals(ClusterObjectUtils.Status.REMOVE)) {
                                roleChunk.setStatus(ClusterObjectUtils.Status.NEUTRAL);
                            }
                            resetTable(target);
                            return roleChunk.getStatus();
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
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    protected BoxedTablePanel<?> getTable() {
        return ((BoxedTablePanel<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

    protected void resetTable(AjaxRequestTarget target) {

    }

    protected String getCompressStatus() {
        return "COMPRESS MODE";
    }

    protected void onPerform(AjaxRequestTarget ajaxRequestTarget) {
    }
}
