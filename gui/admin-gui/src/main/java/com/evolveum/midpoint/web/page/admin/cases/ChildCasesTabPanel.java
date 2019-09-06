/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by honchar
 */
public class ChildCasesTabPanel extends AbstractObjectTabPanel<CaseType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_CHILD_CASES_PANEL = "childCasesPanel";

    public ChildCasesTabPanel(String id, Form<PrismObjectWrapper<CaseType>> mainForm, LoadableModel<PrismObjectWrapper<CaseType>> objectWrapperModel) {
        super(id, mainForm, objectWrapperModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        MainObjectListPanel<CaseType, CompiledObjectCollectionView> table = new MainObjectListPanel<CaseType, CompiledObjectCollectionView>(ID_CHILD_CASES_PANEL,
                CaseType.class, UserProfileStorage.TableId.PAGE_CASE_CHILD_CASES_TAB, Collections.emptyList(), getPageBase()) {

//            @Override
//            protected IColumn<SelectableBean<CaseType>, String> createCheckboxColumn() {
//                return null;
//            }

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, CaseType caseInstance) {
                PageParameters pageParameters = new PageParameters();
                pageParameters.add(OnePageParameterEncoder.PARAMETER, caseInstance.getOid());
                ChildCasesTabPanel.this.getPageBase().navigateToNext(PageCase.class, pageParameters);
            }

            @Override
            protected List<IColumn<SelectableBean<CaseType>, String>> createColumns() {
                List<IColumn<SelectableBean<CaseType>, String>> columns = new ArrayList<IColumn<SelectableBean<CaseType>, String>>();

                IColumn column = new PropertyColumn(createStringResource("pageCases.table.description"), "value.description");
                columns.add(column);

                column = new PropertyColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.state"), CaseType.F_STATE.getLocalPart(), "value.state");
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


                };
                columns.add(column);
                return columns;
            }

            @Override
            protected boolean isCreateNewObjectEnabled(){
                return false;
            }

            @Override
            protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
                if (query == null) {
                    query = ChildCasesTabPanel.this.getPageBase().getPrismContext().queryFactory().createQuery();
                }
                ObjectQuery queryFilter = ChildCasesTabPanel.this.getPageBase().getPrismContext().queryFor(CaseType.class)
                        .item(CaseType.F_PARENT_REF)
                        .ref(getObjectWrapper().getOid())
                        .build();
                query.addFilter(queryFilter.getFilter());
                return query;
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                return null;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu(){
                return new ArrayList<>();
            }

        };
        table.setOutputMarkupId(true);
        add(table);
    }
}
