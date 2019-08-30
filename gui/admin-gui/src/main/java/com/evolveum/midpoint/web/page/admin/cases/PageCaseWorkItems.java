/*
 * Copyright (c) 2010-2018 Evolveum et al.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchFormPanel;
import com.evolveum.midpoint.web.component.util.ContainerListDataProvider;
import com.evolveum.midpoint.web.page.admin.workflow.PageAdminWorkItems;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.cases.dto.CaseWorkItemDtoProvider;
import com.evolveum.midpoint.web.page.admin.cases.dto.SearchingUtils;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.reports.component.SingleValueChoosePanel;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.wf.util.QueryUtils;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_CREATE_TIMESTAMP;

/**
 * @author bpowers
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/workItems")
        },
        encoder = PageParametersEncoder.class,
        action = {
                @AuthorizationAction(actionUri = PageAdminWorkItems.AUTH_APPROVALS_ALL,
                        label = PageAdminWorkItems.AUTH_APPROVALS_ALL_LABEL,
                        description = PageAdminWorkItems.AUTH_APPROVALS_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ALL_WORK_ITEMS_URL,
                        label = "PageAttorneySelection.auth.workItems.label",
                        description = "PageAttorneySelection.auth.workItems.description")
        })
public class PageCaseWorkItems extends PageAdminCaseWorkItems {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageCaseWorkItems.class);

    private static final String DOT_CLASS = PageCaseWorkItems.class.getName() + ".";
    private static final String PARAMETER_CASE_ID = "caseId";
    private static final String PARAMETER_CASE_WORK_ITEM_ID = "caseWorkItemId";

    // Search Form
    private static final String ID_SEARCH_FILTER_FORM = "searchFilterForm";
    private static final String ID_SEARCH = "search";
    private static final String ID_SEARCH_FILTER_RESOURCE = "filterResource";
    private static final String ID_SEARCH_FILTER_ASSIGNEE_CONTAINER = "filterAssigneeContainer";
    private static final String ID_SEARCH_FILTER_ASSIGNEE = "filterAssignee";
    private static final String ID_SEARCH_FILTER_INCLUDE_CLOSED_CASES = "filterIncludeClosedCases";
    // Data Table
    private static final String ID_CASE_WORK_ITEMS_TABLE = "caseWorkItemsTable";
    private static final String ID_BUTTON_BAR = "buttonBar";
    // Buttons
    private static final String ID_CREATE_CASE_BUTTON = "createCaseButton";

    private LoadableModel<Search> searchModel = null;
    private PageParameters pageParameters = null;

    public PageCaseWorkItems() {
    }

    public PageCaseWorkItems(PageParameters pageParameters) {
        this.pageParameters = pageParameters;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        CaseWorkItemsPanel workItemsPanel;
        if (pageParameters != null) {
            workItemsPanel = new CaseWorkItemsPanel(ID_CASE_WORK_ITEMS_TABLE, CaseWorkItemsPanel.View.FULL_LIST, pageParameters){
                private static final long serialVersionUID = 1L;

                @Override
                protected ObjectFilter getCaseWorkItemsFilter(){
                    return QueryUtils.filterForNotClosedStateAndAssignees(getPrismContext().queryFor(CaseWorkItemType.class),
                            SecurityUtils.getPrincipalUser(),
                            OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS, getRelationRegistry())
                            .desc(F_CREATE_TIMESTAMP)
                            .buildFilter();
                }
            };
        } else {
            workItemsPanel = new CaseWorkItemsPanel(ID_CASE_WORK_ITEMS_TABLE, CaseWorkItemsPanel.View.FULL_LIST){
                private static final long serialVersionUID = 1L;

                @Override
                protected List<InlineMenuItem> createRowActions() {
                    List<InlineMenuItem> menu = super.createRowActions();

                    List<InlineMenuItem> additionalMenu = PageCaseWorkItems.this.createRowActions();
                    if (additionalMenu != null){
                        menu.addAll(additionalMenu);
                    }
                    return menu;
                }

                @Override
                protected ObjectFilter getCaseWorkItemsFilter(){
                    return PageCaseWorkItems.this.getCaseWorkItemsFilter();
                }
            };
        }
        workItemsPanel.setOutputMarkupId(true);
        add(workItemsPanel);
    }

    protected ObjectFilter getCaseWorkItemsFilter(){
        return null;
    }

    protected List<InlineMenuItem> createRowActions(){
        return new ArrayList<>();
    }

    protected CaseWorkItemsPanel getCaseWorkItemsTable() {
        return (CaseWorkItemsPanel) get(createComponentPath(ID_CASE_WORK_ITEMS_TABLE));
    }

    //todo peace of old code. not sure if it should be implemented
//    private Panel getCaseWorkItemsSearchField(String itemPath) {
//        return (Panel) get(createComponentPath(ID_SEARCH_FILTER_FORM, itemPath));
//    }
//
//    private ObjectQuery createQuery() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
//        ObjectQuery query;
//        boolean authorizedToSeeAll = isAuthorized(ModelAuthorizationAction.READ_ALL_WORK_ITEMS.getUrl());
//        S_FilterEntryOrEmpty queryStart = getPrismContext().queryFor(CaseWorkItemType.class);
//        if (all && authorizedToSeeAll) {
//            query = queryStart.build();
//        } else {
////             not authorized to see all => sees only allocated to him (not quite what is expected, but sufficient for the time being)
//            query = QueryUtils.filterForAssignees(queryStart, SecurityUtils.getPrincipalUser(),
//                    OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS, getRelationRegistry())
//                    .and().item(CaseWorkItemType.F_CLOSE_TIMESTAMP).isNull().build();
//        }
////        IsolatedCheckBoxPanel includeClosedCases = (IsolatedCheckBoxPanel) getCaseWorkItemsSearchField(ID_SEARCH_FILTER_INCLUDE_CLOSED_CASES);
////        if (includeClosedCases == null || !includeClosedCases.getValue()) {
//        query.addFilter(
//                getPrismContext().queryFor(CaseWorkItemType.class)
//                        .not()
//                        .item(PrismConstants.T_PARENT, CaseType.F_STATE)
//                        .eq(SchemaConstants.CASE_STATE_CLOSED)
//                        .build()
//                        .getFilter()
//        );
////        }
//
//        // Resource Filter
////        SingleValueChoosePanel<ObjectReferenceType, ObjectType> resourceChoice = (SingleValueChoosePanel) getCaseWorkItemsSearchField(ID_SEARCH_FILTER_RESOURCE);
////        if (resourceChoice != null) {
////            List<ObjectType> resources = resourceChoice.getModelObject();
////            if (resources != null && resources.size() > 0) {
////                ObjectType resource = resources.get(0);
////                if (resource != null) {
////                    query.addFilter(
//        // TODO MID-3581
////                        getPrismContext().queryFor(CaseWorkItemType.class)
////                                .item(PrismConstants.T_PARENT, CaseType.F_OBJECT_REF).ref(ObjectTypeUtil.createObjectRef(resource,
////		                        getPrismContext()).asReferenceValue()).buildFilter()
////                    );
////                }
////            }
////        }
//
//        return query;
//    }
//
//    private void initSearch() {
//        final Form searchFilterForm = new Form(ID_SEARCH_FILTER_FORM);
//        add(searchFilterForm);
//        searchFilterForm.setOutputMarkupId(true);
//
//        List<Class<? extends ObjectType>> allowedClasses = new ArrayList<>();
//        allowedClasses.add(ResourceType.class);
//        MultiValueChoosePanel<ObjectType> resource = new SingleValueChoosePanel<ObjectReferenceType, ObjectType>(
//                ID_SEARCH_FILTER_RESOURCE, allowedClasses, objectReferenceTransformer,
//                new PropertyModel<ObjectReferenceType>(Model.of(new ObjectViewDto()), ObjectViewDto.F_NAME)){
//
//            @Override
//            protected void choosePerformedHook(AjaxRequestTarget target, List<ObjectType> selected) {
//                super.choosePerformedHook(target, selected);
//                searchFilterPerformed(target);
//            }
//
//            @Override
//            protected void removePerformedHook(AjaxRequestTarget target, ObjectType value) {
//                super.removePerformedHook(target, value);
//                searchFilterPerformed(target);
//            }
//        };
//        searchFilterForm.add(resource);
//
//        allowedClasses = new ArrayList<>();
//        allowedClasses.add(UserType.class);
//        WebMarkupContainer assigneeContainer = new WebMarkupContainer(ID_SEARCH_FILTER_ASSIGNEE_CONTAINER);
//        MultiValueChoosePanel<ObjectType> assignee = new SingleValueChoosePanel<ObjectReferenceType, ObjectType>(
//                ID_SEARCH_FILTER_ASSIGNEE, allowedClasses, objectReferenceTransformer,
//                new PropertyModel<ObjectReferenceType>(Model.of(new ObjectViewDto()), ObjectViewDto.F_NAME)){
//
//            @Override
//            protected void choosePerformedHook(AjaxRequestTarget target, List<ObjectType> selected) {
//                super.choosePerformedHook(target, selected);
//                searchFilterPerformed(target);
//            }
//
//            @Override
//            protected void removePerformedHook(AjaxRequestTarget target, ObjectType value) {
//                super.removePerformedHook(target, value);
//                searchFilterPerformed(target);
//            }
//        };
//        assigneeContainer.add(assignee);
//        assigneeContainer.add(new VisibleEnableBehaviour() {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isVisible() {
//                return isAuthorizedToSeeAllCases();
//            }
//        });
//        searchFilterForm.add(assigneeContainer);
//
//        IsolatedCheckBoxPanel includeClosedCases = new IsolatedCheckBoxPanel(ID_SEARCH_FILTER_INCLUDE_CLOSED_CASES, new Model<Boolean>(false)) {
//            private static final long serialVersionUID = 1L;
//
//            public void onUpdate(AjaxRequestTarget target) {
//                searchFilterPerformed(target);
//            }
//        };
//        searchFilterForm.add(includeClosedCases);
//    }
//
//    private boolean isAuthorizedToSeeAllCases() {
//        boolean authorizedToSeeAll;
//		try {
//			authorizedToSeeAll = isAuthorized(ModelAuthorizationAction.READ_ALL_WORK_ITEMS.getUrl());
//			return all && authorizedToSeeAll;
//		} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException
//				| CommunicationException | ConfigurationException | SecurityViolationException e) {
//			// TODO handle more cleanly
//            throw new SystemException("Couldn't evaluate authoriztion: "+e.getMessage(), e);
//		}
//    }
//    //endregion
//
//    //region Actions
//    private void searchFilterPerformed(AjaxRequestTarget target) {
//        ObjectQuery query;
//        try {
//            query = createQuery();
//        } catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException
//				| ConfigurationException | SecurityViolationException e) {
//            // TODO handle more cleanly
//            throw new SystemException("Couldn't create case work item query", e);
//        }
//
//        Table panel = getCaseWorkItemsTable();
//        DataTable table = panel.getDataTable();
//        CaseWorkItemDtoProvider provider = (CaseWorkItemDtoProvider) table.getDataProvider();
//        provider.setQuery(query);
//        table.setCurrentPage(0);
//
//        target.add(getFeedbackPanel());
//        target.add((Component) getCaseWorkItemsTable());
//    }
//    //endregion
//
//    private Function<ObjectType, ObjectReferenceType> objectReferenceTransformer =
//            (Function<ObjectType, ObjectReferenceType> & Serializable) (ObjectType o) ->
//                    ObjectTypeUtil.createObjectRef(o, getPrismContext());
//
}
