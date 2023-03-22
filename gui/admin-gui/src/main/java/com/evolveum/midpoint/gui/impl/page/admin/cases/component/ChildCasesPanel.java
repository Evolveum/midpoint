/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by honchar
 */
@PanelType(name = "childCases")
@PanelInstance(identifier = "childCases",
        display = @PanelDisplay(label = "PageCase.childCasesTab"))
@Counter(provider = ChildrenCasesCounter.class)
public class ChildCasesPanel extends AbstractObjectMainPanel<CaseType, AssignmentHolderDetailsModel<CaseType>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_CHILD_CASES_PANEL = "childCasesPanel";

    public ChildCasesPanel(String id, AssignmentHolderDetailsModel<CaseType> objectWrapperModel, ContainerPanelConfigurationType config) {
        super(id, objectWrapperModel, config);
    }

    protected void initLayout() {
        setOutputMarkupId(true);

        MainObjectListPanel<CaseType> table = new MainObjectListPanel<CaseType>(ID_CHILD_CASES_PANEL,
                CaseType.class, Collections.emptyList()) {

            @Override
            protected List<IColumn<SelectableBean<CaseType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<CaseType>, String>> columns = new ArrayList<IColumn<SelectableBean<CaseType>, String>>();

                IColumn column = new PropertyColumn(createStringResource("pageCases.table.description"), "value.description");
                columns.add(column);

                columns.add(ColumnUtils.createCaseActorsColumn(ChildCasesPanel.this.getPageBase()));

                column = new PropertyColumn<SelectableBeanImpl<CaseType>, String>(createStringResource("pageCases.table.state"), CaseType.F_STATE.getLocalPart(), "value.state"){
                    @Override
                    public String getCssClass() {
                        return "mp-w-sm-2 mp-w-md-1";
                    }
                };
                columns.add(column);

                column = new AbstractExportableColumn<SelectableBean<CaseType>, String>(
                        createStringResource("pageCases.table.workitems")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> cellItem,
                                             String componentId, IModel<SelectableBean<CaseType>> model) {
                        cellItem.add(new Label(componentId,
                                model.getObject().getValue() != null && model.getObject().getValue().getWorkItem() != null ?
                                        model.getObject().getValue().getWorkItem().size() : null));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<CaseType>> rowModel) {
                        return Model.of(rowModel.getObject().getValue() != null && rowModel.getObject().getValue().getWorkItem() != null ?
                                Integer.toString(rowModel.getObject().getValue().getWorkItem().size()) : "");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);
                return (List) columns;
            }

            @Override
            protected boolean isCreateNewObjectVisible(){
                return false;
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<CaseType>> createProvider() {
                return createSelectableBeanObjectDataProvider(() -> getChildCasesQuery(), null);
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_CASE_CHILD_CASES_TAB;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu(){
                return new ArrayList<>();
            }

            @Override
            protected IColumn<SelectableBean<CaseType>, String> createCheckboxColumn() {
                return null;
            }
        };
        table.setOutputMarkupId(true);
        add(table);
    }

    private ObjectQuery getChildCasesQuery() {
        ObjectQuery queryFilter = ChildCasesPanel.this.getPageBase().getPrismContext().queryFor(CaseType.class)
                .item(CaseType.F_PARENT_REF)
                .ref(getObjectWrapper().getOid())
                .build();
        return ChildCasesPanel.this.getPageBase().getPrismContext().queryFactory().createQuery(queryFilter.getFilter());
    }
}
