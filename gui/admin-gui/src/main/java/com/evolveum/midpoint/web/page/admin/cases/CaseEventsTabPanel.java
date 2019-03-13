/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.session.ObjectTabStorage;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class CaseEventsTabPanel extends AbstractObjectTabPanel<CaseType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_EVENTS_PANEL = "caseEventsPanel";

    public CaseEventsTabPanel(String id, Form<ObjectWrapper<CaseType>> mainForm, LoadableModel<ObjectWrapper<CaseType>> objectWrapperModel, PageBase pageBase) {
        super(id, mainForm, objectWrapperModel, pageBase);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        ContainerWrapperFromObjectWrapperModel<CaseEventType, CaseType> eventsModel = new ContainerWrapperFromObjectWrapperModel<>(getObjectWrapperModel(), CaseType.F_EVENT);
        MultivalueContainerListPanel<CaseEventType, String> multivalueContainerListPanel =
                new MultivalueContainerListPanel<CaseEventType, String>(ID_EVENTS_PANEL,
                        eventsModel, UserProfileStorage.TableId.PAGE_CASE_EVENTS_TAB,
                        getEventsTabStorage()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void initPaging() {
//                        getWorkitemsTabStorage().setPaging(getPrismContext().queryFactory()
//                                .createPaging(0, ((int) CaseWorkItemsTablePanel.this.getPageBase().getItemsPerPage(getTableId()))));
                    }

                    @Override
                    protected ObjectQuery createQuery() {
                        return null;
                    }

                    @Override
                    protected boolean enableActionNewObject() {
                        return false;
                    }

                    @Override
                    protected boolean isSearchEnabled(){
                        return false;
                    }

                    @Override
                    protected List<IColumn<ContainerValueWrapper<CaseEventType>, String>> createColumns() {
                        return createCaseEventsColumns();
                    }

                    @Override
                    protected void itemPerformedForDefaultAction(AjaxRequestTarget target, IModel<ContainerValueWrapper<CaseEventType>> rowModel,
                                                                 List<ContainerValueWrapper<CaseEventType>> listItems) {

                    }

                    @Override
                    protected WebMarkupContainer getSearchPanel(String contentAreaId) {
                        return new WebMarkupContainer(contentAreaId);
                    }

                    @Override
                    protected List<ContainerValueWrapper<CaseEventType>> postSearch(
                            List<ContainerValueWrapper<CaseEventType>> workItems) {
                        return workItems;
                    }

                    @Override
                    protected List<SearchItemDefinition> initSearchableItems(PrismContainerDefinition<CaseEventType> containerDef) {
                        List<SearchItemDefinition> defs = new ArrayList<>();
                        return defs;
                    }

                };
        multivalueContainerListPanel.setOutputMarkupId(true);
        add(multivalueContainerListPanel);

        setOutputMarkupId(true);

    }

    private List<IColumn<ContainerValueWrapper<CaseEventType>, String>> createCaseEventsColumns(){
        List<IColumn<ContainerValueWrapper<CaseEventType>, String>> columns = new ArrayList<>();
        columns.add(new LinkColumn<ContainerValueWrapper<CaseEventType>>(createStringResource("CaseEventsTabPanel.initiatorRefColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<ContainerValueWrapper<CaseEventType>> rowModel) {
                return Model.of(WebComponentUtil.getReferencedObjectDisplayNamesAndNames(unwrapRowModel(rowModel).getInitiatorRef(), false));
            }

            @Override
            public boolean isEnabled(IModel<ContainerValueWrapper<CaseEventType>> rowModel) {
                //TODO should we check any authorization?
                return true;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ContainerValueWrapper<CaseEventType>> rowModel) {
            }
        });

        return columns;
    }

    private ObjectTabStorage getEventsTabStorage(){
        return getPageBase().getSessionStorage().getCaseEventsTabStorage();
    }

    private CaseEventType unwrapRowModel(IModel<ContainerValueWrapper<CaseEventType>> rowModel){
        return rowModel.getObject().getContainerValue().asContainerable();
    }
}
