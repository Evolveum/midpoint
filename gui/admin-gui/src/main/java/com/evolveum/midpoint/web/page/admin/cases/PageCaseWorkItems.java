/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.cases.dto.CaseWorkItemDto;
import com.evolveum.midpoint.web.page.admin.cases.dto.CaseWorkItemDtoProvider;
import com.evolveum.midpoint.web.page.admin.cases.dto.SearchingUtils;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.reports.component.SingleValueChoosePanel;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.wf.util.QueryUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.isAuthorized;

/**
 * @author bpowers
 */
public abstract class PageCaseWorkItems extends PageAdminCaseWorkItems {

    private static final Trace LOGGER = TraceManager.getTrace(PageCaseWorkItems.class);

    private static final String DOT_CLASS = PageCaseWorkItems.class.getName() + ".";
    private static final String PARAMETER_CASE_ID = "caseId";
    private static final String PARAMETER_CASE_WORK_ITEM_ID = "caseWorkItemId";

    // Search Form
    private static final String ID_SEARCH_FILTER_FORM = "searchFilterForm";
    private static final String ID_SEARCH_FILTER_RESOURCE = "filterResource";
    private static final String ID_SEARCH_FILTER_ASSIGNEE_CONTAINER = "filterAssigneeContainer";
    private static final String ID_SEARCH_FILTER_ASSIGNEE = "filterAssignee";
    private static final String ID_SEARCH_FILTER_INCLUDE_CLOSED_CASES = "filterIncludeClosedCases";
    // Data Table
    private static final String ID_CASE_WORK_ITEMS_TABLE = "caseWorkItemsTable";
    private static final String ID_TABLE_HEADER = "tableHeader";
    private static final String ID_TABLE_BOX = "box";
    private static final String ID_TABLE_BOX_HEADER = "header";
    // Buttons
    private static final String ID_CREATE_CASE_BUTTON = "createCaseButton";

    private boolean all;

    public PageCaseWorkItems(boolean all) {
        this.all = all;
        initLayout();
    }

    //region Data
    private CaseWorkItemDtoProvider createProvider() {
        CaseWorkItemDtoProvider provider = new CaseWorkItemDtoProvider(PageCaseWorkItems.this);
        try {
            provider.setQuery(createQuery());
        } catch (SchemaException e) {
            // TODO handle more cleanly
            throw new SystemException("Couldn't create case work item query", e);
        }
        provider.setSort(SearchingUtils.WORK_ITEM_DEADLINE, SortOrder.ASCENDING);// default sorting
        return provider;
    }

    private ObjectQuery createQuery() throws SchemaException {
        ObjectQuery query;
        boolean authorizedToSeeAll = isAuthorized(ModelAuthorizationAction.READ_ALL_WORK_ITEMS.getUrl());
        S_FilterEntryOrEmpty q = QueryBuilder.queryFor(CaseWorkItemType.class, getPrismContext());
//        S_AtomicFilterExit query = queryStart.asc(PrismConstants.T_PARENT, CaseType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP).;
        if (all && authorizedToSeeAll) {
            query = q.build();
        } else {
            // not authorized to see all => sees only allocated to him (not quite what is expected, but sufficient for the time being)
            query = QueryUtils.filterForAssignees(q, SecurityUtils.getPrincipalUser(),
                    OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS)
                    .and().item(CaseWorkItemType.F_CLOSE_TIMESTAMP).isNull().build();
        }
        CheckBoxPanel includeClosedCases = (CheckBoxPanel) getCaseWorkItemsSearchField(ID_SEARCH_FILTER_INCLUDE_CLOSED_CASES);
        if (includeClosedCases == null || !includeClosedCases.getValue()) {
            query.addFilter(
                QueryBuilder.queryFor(CaseWorkItemType.class, getPrismContext())
                            .item(PrismConstants.T_PARENT, CaseType.F_STATE).eq("open").build().getFilter()
            );
        }

        // Resource Filter
        SingleValueChoosePanel<ObjectReferenceType, ObjectType> resourceChoice = (SingleValueChoosePanel) getCaseWorkItemsSearchField(ID_SEARCH_FILTER_RESOURCE);
        if (resourceChoice != null) {
            List<ObjectType> resources = resourceChoice.getModelObject();
            if (resources != null && resources.size() > 0) {
                ObjectType resource = resources.get(0);
                if (resource != null) {
                    query.addFilter(
                        QueryBuilder.queryFor(CaseWorkItemType.class, getPrismContext())
                                .item(PrismConstants.T_PARENT, CaseType.F_OBJECT_REF).ref(ObjectTypeUtil.createObjectRef(resource).asReferenceValue()).buildFilter()
                    );
                }
            }
        }

        // Assignee Filter
        SingleValueChoosePanel<ObjectReferenceType, ObjectType> assigneeChoice = (SingleValueChoosePanel) getCaseWorkItemsSearchField(createComponentPath(ID_SEARCH_FILTER_ASSIGNEE_CONTAINER, ID_SEARCH_FILTER_ASSIGNEE));
        if (assigneeChoice != null) {
            List<ObjectType> assignees = assigneeChoice.getModelObject();
            if (assignees != null && assignees.size() > 0) {
                ObjectType assignee = assignees.get(0);
                if (assignee != null) {
                    query.addFilter(
                        QueryBuilder.queryFor(CaseWorkItemType.class, getPrismContext())
                                .item(CaseWorkItemType.F_ASSIGNEE_REF).ref(ObjectTypeUtil.createObjectRef(assignee).asReferenceValue()).buildFilter()
                    );
                }
            }
        }

        return query;
    }

    private String getCurrentUserOid() {
        try {
            return getSecurityEnforcer().getPrincipal().getOid();
        } catch (SecurityViolationException e) {
            // TODO handle more cleanly
            throw new SystemException("Couldn't get currently logged user OID", e);
        }
    }
    //endregion

    //region Layout
    private void initLayout() {
        CaseWorkItemDtoProvider provider = createProvider();
        int itemsPerPage = (int) getItemsPerPage(UserProfileStorage.TableId.PAGE_CASE_WORK_ITEMS_PANEL);
        BoxedTablePanel<CaseWorkItemDto> table = new BoxedTablePanel<CaseWorkItemDto>(ID_CASE_WORK_ITEMS_TABLE, provider, initColumns(),
                UserProfileStorage.TableId.PAGE_CASE_WORK_ITEMS_PANEL, itemsPerPage) {

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                return new SearchFragment(headerId, ID_TABLE_HEADER, PageCaseWorkItems.this, PageCaseWorkItems.this);
            }
        };
        table.setShowPaging(true);
        table.setOutputMarkupId(true);
        table.setItemsPerPage(itemsPerPage);        // really don't know why this is necessary, as e.g. in PageRoles the size setting works without it
        add(table);

        initButtons();
    }

    private List<IColumn<CaseWorkItemDto, String>> initColumns() {
        List<IColumn<CaseWorkItemDto, String>> columns = new ArrayList<>();

        IColumn<CaseWorkItemDto, String> column;

        column = new LinkColumn<CaseWorkItemDto>(createStringResource("PageCaseWorkItems.table.description"), CaseWorkItemDto.F_DESCRIPTION) {
            @Override
            public void onClick(AjaxRequestTarget target, IModel<CaseWorkItemDto> rowModel) {
                PageParameters parameters = new PageParameters();
                parameters.add(PARAMETER_CASE_ID, rowModel.getObject().getCase().getOid());
                parameters.add(PARAMETER_CASE_WORK_ITEM_ID, rowModel.getObject().getWorkItemId());
                navigateToNext(PageCaseWorkItem.class, parameters);
            }
        };
        columns.add(column);

        column = new PropertyColumn<>(
                createStringResource("PageCaseWorkItems.table.objectName"),
                SearchingUtils.CASE_OBJECT_NAME, CaseWorkItemDto.F_OBJECT_NAME);
        columns.add(column);

        column = new PropertyColumn<>(createStringResource("PageCaseWorkItems.table.actor"), CaseWorkItemDto.F_ASSIGNEES);
        columns.add(column);

        column = new PropertyColumn<CaseWorkItemDto, String>(
                createStringResource("PageCaseWorkItems.table.openTimestamp"),
                SearchingUtils.CASE_OPEN_TIMESTAMP, CaseWorkItemDto.F_OPEN_TIMESTAMP) {
            @Override
            public void populateItem(Item<ICellPopulator<CaseWorkItemDto>> item, String componentId, IModel<CaseWorkItemDto> rowModel) {
                CaseWorkItemDto dto = rowModel.getObject();
                XMLGregorianCalendar createdCal = dto.getOpenTimestamp();
                final Date created;
                if (createdCal != null) {
                    created = createdCal.toGregorianCalendar().getTime();
                    item.add(AttributeModifier.replace("title", WebComponentUtil.getLocalizedDate(created, DateLabelComponent.LONG_MEDIUM_STYLE)));
                    item.add(new TooltipBehavior());
                } else {
                    created = null;
                }
                item.add(new Label(componentId, new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        return WebComponentUtil.getLocalizedDate(created, DateLabelComponent.LONG_MEDIUM_STYLE);
                    }
                }));
            }
        };
        columns.add(column);

        column = new PropertyColumn<CaseWorkItemDto, String>(
                createStringResource("PageCaseWorkItems.table.deadline"),
                SearchingUtils.WORK_ITEM_DEADLINE, CaseWorkItemDto.F_DEADLINE) {
            @Override
            public void populateItem(Item<ICellPopulator<CaseWorkItemDto>> item, String componentId, IModel<CaseWorkItemDto> rowModel) {
                CaseWorkItemDto dto = rowModel.getObject();
                XMLGregorianCalendar deadlineCal = dto.getDeadline();
                final Date deadline;
                if (deadlineCal != null) {
                    deadline = deadlineCal.toGregorianCalendar().getTime();
                    item.add(AttributeModifier.replace("title", WebComponentUtil.getLocalizedDate(deadline, DateLabelComponent.LONG_MEDIUM_STYLE)));
                    item.add(new TooltipBehavior());
                } else {
                    deadline = null;
                }
                item.add(new Label(componentId, new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        return WebComponentUtil.getLocalizedDate(deadline, DateLabelComponent.LONG_MEDIUM_STYLE);
                    }
                }));
            }
        };
        columns.add(column);

        column = new PropertyColumn<CaseWorkItemDto, String>(
                createStringResource("PageCaseWorkItems.table.closeTimestamp"),
                SearchingUtils.WORK_ITEM_CLOSE_TIMESTAMP, CaseWorkItemDto.F_CLOSE_TIMESTAMP) {
            @Override
            public void populateItem(Item<ICellPopulator<CaseWorkItemDto>> item, String componentId, IModel<CaseWorkItemDto> rowModel) {
                CaseWorkItemDto dto = rowModel.getObject();
                XMLGregorianCalendar closedCal = dto.getCloseTimestamp();
                final Date closed;
                if (closedCal != null) {
                    closed = closedCal.toGregorianCalendar().getTime();
                    item.add(AttributeModifier.replace("title", WebComponentUtil.getLocalizedDate(closed, DateLabelComponent.LONG_MEDIUM_STYLE)));
                    item.add(new TooltipBehavior());
                } else {
                    closed = null;
                }
                item.add(new Label(componentId, new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        return WebComponentUtil.getLocalizedDate(closed, DateLabelComponent.LONG_MEDIUM_STYLE);
                    }
                }));
            }
        };
        columns.add(column);

        column = new PropertyColumn<>(
                createStringResource("PageCaseWorkItems.table.state"), CaseWorkItemDto.F_STATE);
        columns.add(column);

        return columns;
    }

    private Table getCaseWorkItemsTable() {
        return (Table) get(createComponentPath(ID_CASE_WORK_ITEMS_TABLE));
    }

    private Panel getCaseWorkItemsSearchField(String itemPath) {
        return (Panel) get(createComponentPath(ID_CASE_WORK_ITEMS_TABLE, ID_TABLE_BOX, ID_TABLE_BOX_HEADER, ID_SEARCH_FILTER_FORM, itemPath));
    }

    private void initButtons() {
        AjaxButton createCase = new AjaxButton(ID_CREATE_CASE_BUTTON, createStringResource("PageCaseWorkItems.button.createCase")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                navigateToNext(PageCase.class);
            }

        };
        add(createCase);
    }

    private boolean isAuthorizedToSeeAllCases(){
        boolean authorizedToSeeAll = isAuthorized(ModelAuthorizationAction.READ_ALL_WORK_ITEMS.getUrl());
        return all && authorizedToSeeAll;
    }
    //endregion

    //region Actions
    private void searchFilterPerformed(AjaxRequestTarget target) {
        ObjectQuery query;
        try {
            query = createQuery();
        } catch (SchemaException e) {
            // TODO handle more cleanly
            throw new SystemException("Couldn't create case work item query", e);
        }

        Table panel = getCaseWorkItemsTable();
        DataTable table = panel.getDataTable();
        CaseWorkItemDtoProvider provider = (CaseWorkItemDtoProvider) table.getDataProvider();
        provider.setQuery(query);
        table.setCurrentPage(0);

        target.add(getFeedbackPanel());
        target.add((Component) getCaseWorkItemsTable());
    }
    //endregion

    private static class SearchFragment extends Fragment {
        PageCaseWorkItems page;
        public SearchFragment(String id, String markupId, MarkupContainer markupProvider, PageCaseWorkItems page) {
            super(id, markupId, markupProvider);
            this.page = page;

            initLayout();
        }

        private void initLayout() {
            final Form searchFilterForm = new Form(ID_SEARCH_FILTER_FORM);
            add(searchFilterForm);
            searchFilterForm.setOutputMarkupId(true);

            List<Class<? extends ObjectType>> allowedClasses = new ArrayList<>();
            allowedClasses.add(ResourceType.class);
            MultiValueChoosePanel<ObjectType> resource = new SingleValueChoosePanel<ObjectReferenceType, ObjectType>(
                    ID_SEARCH_FILTER_RESOURCE, allowedClasses, objectReferenceTransformer,
                    new PropertyModel<ObjectReferenceType>(Model.of(new ObjectViewDto()), ObjectViewDto.F_NAME)){

                @Override
                protected void choosePerformedHook(AjaxRequestTarget target, List<ObjectType> selected) {
                    super.choosePerformedHook(target, selected);
                    page.searchFilterPerformed(target);
                }

                @Override
                protected void removePerformedHook(AjaxRequestTarget target, ObjectType value) {
                    super.removePerformedHook(target, value);
                    page.searchFilterPerformed(target);
                }
            };
            searchFilterForm.add(resource);

            allowedClasses = new ArrayList<>();
            allowedClasses.add(UserType.class);
            WebMarkupContainer assigneeContainer = new WebMarkupContainer(ID_SEARCH_FILTER_ASSIGNEE_CONTAINER);
            MultiValueChoosePanel<ObjectType> assignee = new SingleValueChoosePanel<ObjectReferenceType, ObjectType>(
                    ID_SEARCH_FILTER_ASSIGNEE, allowedClasses, objectReferenceTransformer,
                    new PropertyModel<ObjectReferenceType>(Model.of(new ObjectViewDto()), ObjectViewDto.F_NAME)){

                @Override
                protected void choosePerformedHook(AjaxRequestTarget target, List<ObjectType> selected) {
                    super.choosePerformedHook(target, selected);
                    page.searchFilterPerformed(target);
                }

                @Override
                protected void removePerformedHook(AjaxRequestTarget target, ObjectType value) {
                    super.removePerformedHook(target, value);
                    page.searchFilterPerformed(target);
                }
            };
            assigneeContainer.add(assignee);
            assigneeContainer.add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return page.isAuthorizedToSeeAllCases();
                }
            });
            searchFilterForm.add(assigneeContainer);

            CheckBoxPanel includeClosedCases = new CheckBoxPanel(ID_SEARCH_FILTER_INCLUDE_CLOSED_CASES, new Model<Boolean>(false)) {
                private static final long serialVersionUID = 1L;

                public void onUpdate(AjaxRequestTarget target) {
                    if (page != null) {
                        page.searchFilterPerformed(target);
                    }
                }
            };
            searchFilterForm.add(includeClosedCases);
        }

        private Function<ObjectType, ObjectReferenceType> objectReferenceTransformer =
                (Function<ObjectType, ObjectReferenceType> & Serializable) (ObjectType o) ->
                        ObjectTypeUtil.createObjectRef(o);
    }
}
