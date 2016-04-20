/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.FocusBrowserPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;

/**
 * Creates a panel with the list of focus type items
 * with the possibility to filter by user (show only
 * assigned to the specified user items), to search
 * through the list and to reset the list to the
 * initial state
 * 
 * @author Kate Honchar
 */
public class MultipleAssignmentSelector<F extends FocusType, H extends FocusType> extends BasePanel<List<AssignmentEditorDto>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(MultipleAssignmentSelector.class);
    private static final String DOT_CLASS = MultipleAssignmentSelector.class.getName() + ".";
    private static final String OPERATION_LOAD_AVAILABLE_ROLES = DOT_CLASS + "loadAvailableRoles";
    private static final String OPERATION_LOAD_FILTER_OBJECT = DOT_CLASS + "loadFilterObject";

    private static final String ID_USER_CHOOSER_DIALOG = "userChooserDialog";
    private static final String ID_TABLE = "table";
    private static final String ID_FILTER_BUTTON_CONTAINER = "filterButtonContainer";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH = "search";
    private static final int ITEMS_PER_PAGE = 10;
    private static final String ID_FILTER_BY_USER_BUTTON = "filterByUserButton";
    private static final String ID_LABEL = "label";
    private static final String ID_DELETE_BUTTON = "deleteButton";

    private boolean showDialog = true;
    private IModel<Search> searchModel;
    private BaseSortableDataProvider provider;
    private Class type;
    private Class<H> targetFocusClass;
    private String labelValue ="";
    private IModel<ObjectFilter> filterModel = null;
    private ObjectFilter authorizedRolesFilter = null;
    private ObjectQuery searchQuery = null;
    private PrismObject<F> focus;
    private H filterObject = null;

    public MultipleAssignmentSelector(String id, IModel<List<AssignmentEditorDto>> selectorModel, BaseSortableDataProvider provider,
                                      Class<H> targetFocusClass, Class type, PrismObject<F> focus) {
        super(id, selectorModel);
        this.provider = provider == null ? getListDataProvider(null) : provider;
        this.type = type;
        this.focus=focus;
        this.targetFocusClass = targetFocusClass;
        filterModel = getFilterModel();
        searchModel = new LoadableModel<Search>(false) {

            @Override
            public Search load() {
                Search search =  SearchFactory.createSearch(RoleType.class, getPageBase().getPrismContext(), false);
                return search;
            }
        };

        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer filterButtonContainer = new WebMarkupContainer(ID_FILTER_BUTTON_CONTAINER);
        AjaxLink<String> filterByUserButton = new AjaxLink<String>(ID_FILTER_BY_USER_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (showDialog) {
                    labelValue = createStringResource("MultipleAssignmentSelector.filterByUser").getString();
                    initUserDialog(createStringResource("MultipleAssignmentSelector.filterByUser"), target);
                }
                showDialog = true;
            }
        };
        filterButtonContainer.add(filterByUserButton);

        labelValue = createStringResource("MultipleAssignmentSelector.filterByUser").getString();
        Label label = new Label(ID_LABEL, createLabelModel());
        label.setRenderBodyOnly(true);
        filterByUserButton.add(label);

        AjaxLink deleteButton = new AjaxLink(ID_DELETE_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                labelValue = createStringResource("MultipleAssignmentSelector.filterByUser").getString();
                showDialog = false;
                deleteFilterPerformed(target);
            }
        };
        filterByUserButton.add(deleteButton);
        add(filterButtonContainer);

        initSearchPanel();
        add(initTablePanel());
    }

    private Component createRowLink(String id, final IModel<SelectableBean<AssignmentEditorDto>> rowModel) {
        AjaxLink<SelectableBean<AssignmentEditorDto>> button = new AjaxLink<SelectableBean<AssignmentEditorDto>>(id, rowModel) {

            @Override
            public IModel<?> getBody() {
                AssignmentEditorDto dto =(AssignmentEditorDto) rowModel.getObject();
                String name = StringUtils.isNotEmpty(dto.getNameForTargetObject()) ?
                        dto.getNameForTargetObject() : dto.getName();
                return new Model<String>(name);
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                LOGGER.trace("{} CLICK: {}", this, rowModel.getObject());
                toggleRow(rowModel);
                target.add(this);
            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                if (rowModel.getObject().isSelected()) {
                    tag.put("class", "list-group-item active");
                } else {
                    tag.put("class", "list-group-item");
                }
                String description = ((AssignmentEditorDto) rowModel.getObject()).getDescription();
                if (description != null) {
                    tag.put("title", description);
                }
            }
        };
        button.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                return ((AssignmentEditorDto)rowModel.getObject()).getStatus() != UserDtoStatus.DELETE;
            }
        });
        button.setOutputMarkupId(true);
        return button;
    }

    private List<IColumn<SelectableBean<AssignmentEditorDto>, String>> initColumns() {
        List<IColumn<SelectableBean<AssignmentEditorDto>, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<SelectableBean<AssignmentEditorDto>, String>(new Model()) {
            public void populateItem(Item<ICellPopulator<SelectableBean<AssignmentEditorDto>>> cellItem, String componentId,
                                     IModel<SelectableBean<AssignmentEditorDto>> rowModel) {
                cellItem.add(createRowLink(componentId, rowModel));
            }
        });

        return columns;
    }

    private void updateBoxedTablePanelStyles(BoxedTablePanel panel) {
        panel.getDataTable().add(new AttributeModifier("class", ""));
        panel.getDataTable().add(new AttributeAppender("style", "width: 100%;"));
        panel.getDataTableContainer().add(new AttributeAppender("style", "min-height: 415px;"));
        panel.getFooterPaging().getParent().add(new AttributeModifier("class", "col-md-10"));
    }

    public BaseSortableDataProvider getProvider() {
        return provider;
    }

    private void toggleRow(IModel<SelectableBean<AssignmentEditorDto>> rowModel){
        rowModel.getObject().setSelected(!rowModel.getObject().isSelected());
        List<AssignmentEditorDto> providerList = ((BaseSortableDataProvider) getProvider()).getAvailableData();
        for (AssignmentEditorDto dto : providerList){
            if (dto.equals(rowModel.getObject())){
                dto.setSelected(rowModel.getObject().isSelected());
                break;
            }
        }

    }

    private void initSearchPanel(){
        final Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);
        searchForm.setOutputMarkupId(true);

        SearchPanel search = new SearchPanel(ID_SEARCH, (IModel) searchModel) {

            @Override
            public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
                MultipleAssignmentSelector.this.searchPerformed(query, target);
            }
        };
        searchForm.add(search);

    }

    private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
        searchQuery = query;

        BoxedTablePanel panel = getTable();
        panel.setCurrentPage(null);
        provider.setQuery(query);
        target.add(panel);
    }

    public BoxedTablePanel getTable() {
        return (BoxedTablePanel) get(ID_TABLE);
    }

    private BoxedTablePanel initTablePanel(){
        List<IColumn<SelectableBean<AssignmentEditorDto>, String>> columns = initColumns();

        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns,
                UserProfileStorage.TableId.TABLE_ROLES, ITEMS_PER_PAGE);
        updateBoxedTablePanelStyles(table);
        //hide footer menu
        table.getFooterMenu().setVisible(false);
        //hide footer count label
        table.getFooterCountLabel().setVisible(false);
        table.setOutputMarkupId(true);
        return table;
    }

    private void initUserDialog(IModel<String> title, AjaxRequestTarget target) {

        List<QName> supportedTypes = new ArrayList<>();
        supportedTypes.add(getPageBase().getPrismContext().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(targetFocusClass).getTypeName());
        FocusBrowserPanel<H> focusBrowser = new FocusBrowserPanel<H>(getPageBase().getMainPopupBodyId(),
                targetFocusClass, supportedTypes, false, getPageBase()) {
            @Override
    		protected void onSelectPerformed(AjaxRequestTarget target, H filterUser) {
                super.onSelectPerformed(target, filterUser);
                filterObject = filterUser;
                filterByUserPerformed();
                 replaceTable(target);

                 labelValue += " " + filterUser.getName().toString();
                 target.add(getFilterButton());
    		}
    	};
       getPageBase().showMainPopup(focusBrowser, title, target, 900, 500);
    }


    private void filterByUserPerformed(){
        //we need to load filter object (e.g. user) totally because we
        //need to know its assignments' tenantRef data (name)
        //and orgRef data (name)
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_FILTER_OBJECT);
        OperationResult result = task.getResult();
        PrismObject<H> loadedObject = WebModelServiceUtils.loadObject(targetFocusClass, filterObject.getOid(), null, getPageBase(), task,
                result);
        provider =  getListDataProvider(loadedObject != null ? loadedObject.asObjectable() : filterObject);
    }

    private void deleteFilterPerformed(AjaxRequestTarget target){
        filterObject = null;
        provider = getAvailableAssignmentsDataProvider();
        provider.setQuery(searchQuery);
        replaceTable(target);
    }

    private  void replaceTable(AjaxRequestTarget target){
        BoxedTablePanel table = initTablePanel();
        MultipleAssignmentSelector.this.replace(table);
        target.add(MultipleAssignmentSelector.this);
    }


    public void setFilterButtonVisibility(boolean isVisible){
        getFilterButton().setVisible(isVisible);
    }

    private IModel<String> createLabelModel(){
        return new IModel<String>() {
            @Override
            public String getObject() {
                return labelValue;
            }

            @Override
            public void setObject(String s) {
            }

            @Override
            public void detach() {
            }
        };
    }

    private Component getFilterButton(){
        return get(createComponentPath(ID_FILTER_BUTTON_CONTAINER, ID_FILTER_BY_USER_BUTTON));
    }

    public <T extends FocusType> BaseSortableDataProvider getListDataProvider(final FocusType focus) {
        BaseSortableDataProvider provider;
//        if (filterObject == null){
//            provider = getAvailableAssignmentsDataProvider();
//        } else {
            provider = new ListDataProvider<AssignmentEditorDto>(this, new IModel<List<AssignmentEditorDto>>() {
                @Override
                public List<AssignmentEditorDto> getObject() {
                    return getAvailableAssignmentsDataList(focus);
                }

                @Override
                public void setObject(List<AssignmentEditorDto> list) {
                }

                @Override
                public void detach() {

                }
            });
//        }
        return provider;
    }

    private <T extends FocusType> List<AssignmentEditorDto> getAvailableAssignmentsDataList(FocusType focus){
        ObjectQuery query = provider.getQuery() == null ? new ObjectQuery() : provider.getQuery();

        List<AssignmentEditorDto> assignmentsList = getListProviderDataList();
        if (assignmentsList == null) {
            if (focus != null) {
                assignmentsList = getAssignmentEditorDtoList(focus.getAssignment());
            } else {
                assignmentsList = getModelObject();
            }
        }
        List<AssignmentEditorDto> currentAssignments = getAssignmentsByType(assignmentsList);

        if (filterModel != null && filterModel.getObject() != null) {
            query.addFilter(filterModel.getObject());
        }
        return applyQueryToListProvider(query, currentAssignments);
    }

    protected List<AssignmentEditorDto> getListProviderDataList(){
        return null;
    }

    private List<AssignmentEditorDto> getAssignmentsByType(List<AssignmentEditorDto> assignmentsList) {
        List<AssignmentEditorDto> currentUsersAssignments = new ArrayList<>();
        for (AssignmentEditorDto dto : assignmentsList) {
            if (dto.getType().equals(AssignmentEditorDtoType.getType(type)) && !dto.getStatus().equals(UserDtoStatus.DELETE)) {
                currentUsersAssignments.add(dto);
            }
        }
        return currentUsersAssignments;
    }

    private List<AssignmentEditorDto> applyQueryToListProvider(ObjectQuery query, List<AssignmentEditorDto> providerList){
        ObjectDataProvider temporaryProvider = new ObjectDataProvider(MultipleAssignmentSelector.this, type);
        List<AssignmentEditorDto> displayAssignmentsList = new ArrayList<>();
        temporaryProvider.setQuery(query);
        for (AssignmentEditorDto dto : providerList) {
            Iterator it = temporaryProvider.internalIterator(0, temporaryProvider.size());
            while (it.hasNext()) {
                SelectableBean selectableBean = (SelectableBean) it.next();
                F object = (F) selectableBean.getValue();
                if (object.getOid().equals(dto.getTargetRef().getOid())) {
                    displayAssignmentsList.add(dto);
                    break;
                }
            }
        }
        return displayAssignmentsList;
    }

    private  IModel<ObjectFilter> getFilterModel(){
        return new IModel<ObjectFilter>() {
            @Override
            public ObjectFilter getObject() {
                if (authorizedRolesFilter == null){
                    initRolesFilter();
                }
                return authorizedRolesFilter;
            }

            @Override
            public void setObject(ObjectFilter objectFilter) {

            }

            @Override
            public void detach() {

            }
        };
    }

    private void initRolesFilter (){
        LOGGER.debug("Loading roles which the current user has right to assign");
        OperationResult result = new OperationResult(OPERATION_LOAD_AVAILABLE_ROLES);
        try {
            PageBase pb = getPageBase();
            ModelInteractionService mis = pb.getModelInteractionService();
            RoleSelectionSpecification roleSpec = mis.getAssignableRoleSpecification(focus, result);
            authorizedRolesFilter = roleSpec.getFilter();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load available roles", ex);
            result.recordFatalError("Couldn't load available roles", ex);
        } finally {
            result.recomputeStatus();
        }
        if (!result.isSuccess() && !result.isHandledError()) {
            getPageBase().showResult(result);
        }
    }

    private List<AssignmentEditorDto> getAssignmentEditorDtoList(List<AssignmentType> assignmentTypeList){
        List<AssignmentEditorDto> assignmentEditorDtoList = new ArrayList<>();
        for (AssignmentType assignmentType : assignmentTypeList){
            AssignmentEditorDto assignmentEditorDto = new AssignmentEditorDto(UserDtoStatus.MODIFY, assignmentType, getPageBase());
            assignmentEditorDtoList.add(assignmentEditorDto);
        }
        return assignmentEditorDtoList;
    }

    public ObjectDataProvider getAvailableAssignmentsDataProvider() {
        ObjectDataProvider<AssignmentEditorDto, F> provider = new ObjectDataProvider<AssignmentEditorDto, F>(this, type) {

            @Override
            public AssignmentEditorDto createDataObjectWrapper(PrismObject<F> obj) {
                return AssignmentEditorDto.createDtoFromObject(obj.asObjectable(), UserDtoStatus.MODIFY, getPageBase());
            }

            @Override
            public void setQuery(ObjectQuery query) {
                super.setQuery(query);
                searchQuery = query;
            }

            @Override
            public ObjectQuery getQuery() {
                if (searchQuery == null){
                    searchQuery = new ObjectQuery();
                }
                if (filterModel != null && filterModel.getObject() != null){
                    searchQuery.addFilter(filterModel.getObject());
                }
                return searchQuery;
            }
        };
        return provider;
    }
}
