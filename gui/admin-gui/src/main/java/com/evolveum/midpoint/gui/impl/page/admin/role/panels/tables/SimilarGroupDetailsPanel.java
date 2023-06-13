/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkTruncatePanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MiningType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class SimilarGroupDetailsPanel extends Panel {

    private static final String ID_DATATABLE = "datatable_extra";

    public SimilarGroupDetailsPanel(String id, List<PrismObject<MiningType>> miningTypeList,
            List<PrismObject<RoleType>> rolePrismObjectList, String targetOid, boolean sortable) {
        super(id);
        BoxedTablePanel<PrismObject<MiningType>> components = generateTableRM(miningTypeList,
                rolePrismObjectList, targetOid, sortable);
        components.setOutputMarkupId(true);

        add(components);


    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    public BoxedTablePanel<PrismObject<MiningType>> generateTableRM(List<PrismObject<MiningType>> groupsOid,
            List<PrismObject<RoleType>> rolePrismObjectList, String targetOid, boolean sortable) {

        RoleMiningProvider<PrismObject<MiningType>> provider = new RoleMiningProvider<>(
                this, new ListModel<>(groupsOid) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<PrismObject<MiningType>> object) {
                super.setObject(object);
            }

        }, sortable);

        if (sortable) {
            provider.setSort(MiningType.F_ROLES.toString(), SortOrder.ASCENDING);
        }
        BoxedTablePanel<PrismObject<MiningType>> table = new BoxedTablePanel<>(
                ID_DATATABLE, provider, initColumnsRM(rolePrismObjectList, targetOid),
                null, true, true);
        table.setOutputMarkupId(true);

        return table;
    }

    public List<IColumn<PrismObject<MiningType>, String>> initColumnsRM(List<PrismObject<RoleType>> rolePrismObjectList,
            String targetOid) {

        List<IColumn<PrismObject<MiningType>, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismObject<MiningType>> rowModel) {

                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractExportableColumn<>(Model.of("Group")) {

            @Override
            public String getSortProperty() {
                return MiningType.F_OID.getLocalPart();
            }

            @Override
            public IModel<?> getDataModel(IModel<PrismObject<MiningType>> iModel) {
                return null;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<PrismObject<MiningType>>> item, String componentId,
                    IModel<PrismObject<MiningType>> rowModel) {

                item.add(AttributeAppender.replace("class", " overflow-auto"));
                item.add(new AttributeAppender("style", " width:150px"));

                Label label = new Label(componentId, rowModel.getObject().getOid());
                String oid = rowModel.getObject().getValue().getOid();
                item.add(label);
                if (oid.equals(targetOid)) {
                    item.add(new AttributeAppender("class", " table-primary"));
                }

            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId, Model.of("Group")).add(
                        new AttributeAppender("style",
                                "  writing-mode: vertical-lr;  -webkit-transform: rotate(-270deg);"));
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name";
            }
        });

        IColumn<PrismObject<MiningType>, String> column;
        for (PrismObject<RoleType> roleTypePrismObject : rolePrismObjectList) {
            RoleType roleType = roleTypePrismObject.asObjectable();
            String oid = roleType.getOid();
            String name = "" + roleType.getName().toString();

            column = new AbstractColumn<>(createStringResource(name)) {

                @Override
                public void populateItem(Item<ICellPopulator<PrismObject<MiningType>>> cellItem,
                        String componentId, IModel<PrismObject<MiningType>> model) {

                    tableStyle(cellItem);

                    List<String> roleMembers = model.getObject().asObjectable().getRoles();
                    if (roleMembers.contains(oid)) {
                        filledCell(cellItem, componentId);
                    } else {
                        emptyCell(cellItem, componentId);
                    }
                }

                @Override
                public Component getHeader(String componentId) {

                    DisplayType displayType = GuiDisplayTypeUtil.createDisplayType(
                            WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));

                    return new AjaxLinkTruncatePanel(componentId,
                            createStringResource(name), createStringResource(name), displayType) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {

                            PageParameters parameters = new PageParameters();
                            parameters.add(OnePageParameterEncoder.PARAMETER, oid);
                            ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                        }

                        @Override
                        public boolean isEnabled() {
                            return true;
                        }
                    };
                }

            };
            columns.add(column);
        }

        return columns;
    }

    private void tableStyle(@NotNull Item<?> cellItem) {
        MarkupContainer parentContainer = cellItem.getParent().getParent();
        parentContainer.add(AttributeAppender.replace("class", "d-flex"));
        parentContainer.add(AttributeAppender.replace("style", "height:40px"));

        cellItem.add(AttributeAppender.append("style", "width:40px; height:40px; border: 1px solid #f4f4f4;"));
        cellItem.add(AttributeAppender.remove("class"));
    }


    private void emptyCell(@NotNull Item<?> cellItem, String componentId) {
        cellItem.add(new EmptyPanel(componentId));
    }

    private void filledCell(@NotNull Item<?> cellItem, String componentId) {
        cellItem.add(new AttributeAppender("class", " table-dark"));
        cellItem.add(new EmptyPanel(componentId));
    }

}
