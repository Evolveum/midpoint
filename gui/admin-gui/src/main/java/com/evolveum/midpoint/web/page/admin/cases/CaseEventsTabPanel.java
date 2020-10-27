/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.session.SessionStorage;

import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

/**
 * Created by honchar
 */
public class CaseEventsTabPanel extends AbstractObjectTabPanel<CaseType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_EVENTS_PANEL = "caseEventsPanel";

    public CaseEventsTabPanel(String id, MidpointForm<PrismObjectWrapper<CaseType>> mainForm, LoadableModel<PrismObjectWrapper<CaseType>> objectWrapperModel, PageBase pageBase) {
        super(id, mainForm, objectWrapperModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        PrismContainerWrapperModel<CaseType, CaseEventType> eventsModel = PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), CaseType.F_EVENT);
        MultivalueContainerListPanel<CaseEventType> multivalueContainerListPanel =
                new MultivalueContainerListPanel<CaseEventType>(ID_EVENTS_PANEL, CaseEventType.class) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected ObjectQuery createQuery() {
                        return null;
                    }

                    @Override
                    protected boolean isCreateNewObjectVisible() {
                        return false;
                    }

                    @Override
                    protected IModel<PrismContainerWrapper<CaseEventType>> getContainerModel() {
                        return eventsModel;
                    }

                    @Override
                    protected boolean isSearchVisible(){
                        return false;
                    }

                    @Override
                    protected List<IColumn<PrismContainerValueWrapper<CaseEventType>, String>> createDefaultColumns() {
                        return createCaseEventsColumns();
                    }

                    @Override
                    protected void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<CaseEventType>> rowModel,
                                                                 List<PrismContainerValueWrapper<CaseEventType>> listItems) {

                    }

                    @Override
                    protected List<SearchItemDefinition> initSearchableItems(PrismContainerDefinition<CaseEventType> containerDef) {
                        List<SearchItemDefinition> defs = new ArrayList<>();
                        return defs;
                    }

                    @Override
                    protected String getStorageKey() {
                        return SessionStorage.KEY_CASE_EVENTS_TAB;
                    }

                    @Override
                    protected UserProfileStorage.TableId getTableId() {
                        return UserProfileStorage.TableId.PAGE_CASE_EVENTS_TAB;
                    }
                };
        multivalueContainerListPanel.setOutputMarkupId(true);
        add(multivalueContainerListPanel);

        setOutputMarkupId(true);

    }

    private List<IColumn<PrismContainerValueWrapper<CaseEventType>, String>> createCaseEventsColumns(){
        List<IColumn<PrismContainerValueWrapper<CaseEventType>, String>> columns = new ArrayList<>();
        columns.add(new AjaxLinkColumn<PrismContainerValueWrapper<CaseEventType>>(createStringResource("CaseEventsTabPanel.initiatorRefColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<CaseEventType>> rowModel) {
                return Model.of(WebComponentUtil.getReferencedObjectDisplayNamesAndNames(unwrapRowModel(rowModel).getInitiatorRef(), false));
            }

            @Override
            public boolean isEnabled(IModel<PrismContainerValueWrapper<CaseEventType>> rowModel) {
                return false;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<CaseEventType>> rowModel) {
            }
        });
        columns.add(new AbstractColumn<PrismContainerValueWrapper<CaseEventType>, String>(createStringResource("CaseEventsTabPanel.stageNumber")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseEventType>>> cellItem,
                                     String componentId, IModel<PrismContainerValueWrapper<CaseEventType>> rowModel) {

                CaseEventType caseEventType = rowModel.getObject().getRealValue();
                cellItem.add(new Label(componentId,
                        caseEventType != null ? caseEventType.getStageNumber() : ""));
            }
        });

        columns.add(new AbstractColumn<PrismContainerValueWrapper<CaseEventType>, String>(createStringResource("CaseEventsTabPanel.Iteration")){
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<CaseEventType>>> cellItem,
                                     String componentId, IModel<PrismContainerValueWrapper<CaseEventType>> rowModel) {

                CaseEventType caseEventType = rowModel.getObject().getRealValue();
                cellItem.add(new Label(componentId,
                        caseEventType != null ? caseEventType.getIteration() : ""));
            }
        });

        return columns;
    }

    private CaseEventType unwrapRowModel(IModel<PrismContainerValueWrapper<CaseEventType>> rowModel){
        return rowModel.getObject().getRealValue();
    }
}
