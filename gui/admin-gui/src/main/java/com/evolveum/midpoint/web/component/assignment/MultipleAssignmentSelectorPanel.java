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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.form.multivalue.GenericMultiValueLabelEditPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.users.component.*;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTableDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Honchar.
 * Panel contains available list of focus type items and
 * the list of assigned items of the same type with the
 * possibility of editing the list of assignments.
 */
public class MultipleAssignmentSelectorPanel<F extends FocusType, H extends FocusType> extends BasePanel<List<AssignmentEditorDto>> {
    private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;
    private static final String ID_BUTTON_REMOVE = "remove";
    private static final String ID_BUTTON_ADD = "add";
    private static final String ID_FORM = "form";
    private static final String ID_AVAILABLE_ASSIGNMENTS = "availableAssignments";
    private static final String ID_CURRENT_ASSIGNMENTS = "currentAssignments";
    private static final String ID_TENANT_EDITOR = "tenantEditor";
    private static final String ID_ORG_EDITOR = "orgEditor";
    private static final String ID_BUTTON_RESET = "buttonReset";

    private static final String LABEL_SIZE = "col-md-4";
    private static final String INPUT_SIZE = "col-md-10";


    private static final String DOT_CLASS = MultipleAssignmentSelectorPanel.class.getName();
    private static final String OPERATION_LOAD_AVAILABLE_ROLES = DOT_CLASS + "loadAvailableRoles";
    private Class<F> type;

    private BaseSortableDataProvider dataProvider;
    private BaseSortableDataProvider currentAssignmentsProvider;
    private List<OrgType> tenantEditorObject = new ArrayList<>();
    private List<OrgType> orgEditorObject = new ArrayList<>();
    private PrismObject<UserType> user;
    private ObjectQuery dataProviderQuery;
    private ObjectFilter authorizedRolesFilter = null;
    private IModel<ObjectFilter> filterModel = null;
    private static final Trace LOGGER = TraceManager.getTrace(MultipleAssignmentSelectorPanel.class);

    public MultipleAssignmentSelectorPanel(String id, LoadableModel<List<AssignmentEditorDto>> assignmentsModel,
                                           PrismObject<UserType> user, Class<H> targetFocusClass, Class<F> type) {
        super(id, assignmentsModel);
        this.assignmentsModel = assignmentsModel;
        this.type = type;
        this.user = user;
        filterModel = getFilterModel();
        tenantEditorObject.add(new OrgType());
        orgEditorObject.add(new OrgType());
        initLayout(targetFocusClass);

    }

    private void initLayout(Class<H> targetFocusClass) {

        IModel<List<AssignmentEditorDto>> availableAssignmentModel = createAvailableAssignmentModel();
        dataProvider = getAvailableAssignmentsDataProvider();
        final MultipleAssignmentSelector availableAssignmentsPanel = new MultipleAssignmentSelector<H>(ID_AVAILABLE_ASSIGNMENTS,
                availableAssignmentModel, dataProvider, targetFocusClass, type);
        currentAssignmentsProvider = getListDataProvider(null);
        final MultipleAssignmentSelector currentAssignmentsPanel = new MultipleAssignmentSelector<H>(ID_CURRENT_ASSIGNMENTS,
                assignmentsModel, currentAssignmentsProvider, targetFocusClass, type);
        currentAssignmentsPanel.setFilterButtonVisibility(false);

        AjaxButton add = new AjaxButton(ID_BUTTON_ADD) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form form) {
                addToAssignmentsModel(target, availableAssignmentsPanel, currentAssignmentsPanel);
            }
        };

        AjaxButton remove = new AjaxButton(ID_BUTTON_REMOVE) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form form) {
                deleteFromAssignmentsModel(target, currentAssignmentsPanel, availableAssignmentsPanel);
            }
        };

        AjaxLink<String> buttonReset = new AjaxLink<String>(ID_BUTTON_RESET) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                reset(currentAssignmentsPanel);
                target.add(currentAssignmentsPanel);
            }
        };
        buttonReset.setBody(createStringResource("MultipleAssignmentSelector.reset"));

        Form<?> form = new Form<Void>(ID_FORM);
        form.add(createTenantContainer());
        form.add(createOrgContainer());
        form.add(availableAssignmentsPanel);
        form.add(currentAssignmentsPanel);
        form.add(buttonReset);
        form.add(add);
        form.add(remove);
        add(form);

    }

    private IModel<List<AssignmentEditorDto>> createAvailableAssignmentModel() {
        return new IModel<List<AssignmentEditorDto>>() {
            @Override
            public List<AssignmentEditorDto> getObject() {
                return new ArrayList<>();
            }

            @Override
            public void setObject(List<AssignmentEditorDto> assignmentList) {
            }

            @Override
            public void detach() {
            }
        };
    }

    private void addToAssignmentsModel(AjaxRequestTarget target, MultipleAssignmentSelector from, MultipleAssignmentSelector to) {
        List<AssignmentEditorDto> fromProviderList = from.getProvider().getAvailableData();
        List<AssignmentEditorDto> listToBeAdded = new ArrayList<>();
        List<AssignmentEditorDto> assignmentsList = assignmentsModel.getObject();
        if (tenantEditorObject != null  && StringUtils.isNotEmpty(tenantEditorObject.get(0).getOid()) ||
                orgEditorObject != null && StringUtils.isNotEmpty(orgEditorObject.get(0).getOid())) {
            setTenantAndOrgToAssignmentsList(fromProviderList);
        }
        for (AssignmentEditorDto dto : fromProviderList) {
            if (dto.isSelected()) {
                boolean toBeAdded = true;
                for (AssignmentEditorDto assignmentDto : assignmentsList) {
                    if (assignmentDto.getTargetRef().getOid().equals(dto.getTargetRef().getOid())) {
                        if (areEqualReferenceObjects(assignmentDto.getTenantRef(), dto.getTenantRef()) &&
                                areEqualReferenceObjects(assignmentDto.getOrgRef(), dto.getOrgRef())){
                            if (assignmentDto.getStatus().equals(UserDtoStatus.DELETE)) {
                                assignmentDto.setStatus(UserDtoStatus.MODIFY);
                            }
                            assignmentDto.setTenantRef(dto.getTenantRef());
                            assignmentDto.setOrgRef(dto.getOrgRef());
                            toBeAdded = false;
                        }
                    }
                }
                if (toBeAdded) {
                    dto.setStatus(UserDtoStatus.ADD);
                    listToBeAdded.add(dto);
                }
                dto.setSelected(false);
            }
        }
        assignmentsList.addAll(listToBeAdded);
        target.add(to);
        target.add(from);
    }

    private void deleteFromAssignmentsModel(AjaxRequestTarget target, MultipleAssignmentSelector from, MultipleAssignmentSelector to) {
        List<AssignmentEditorDto> fromProviderList = ((BaseSortableDataProvider) from.getProvider()).getAvailableData();
        List<AssignmentEditorDto> listToBeRemoved = new ArrayList<>();
        List<AssignmentEditorDto> assignmentsList = assignmentsModel.getObject();
        for (AssignmentEditorDto dto : fromProviderList) {
            if (dto.isSelected()) {
                for (AssignmentEditorDto assignmentDto : assignmentsList) {
                    if (assignmentDto.getTargetRef().getOid().equals(dto.getTargetRef().getOid())) {
                        if (assignmentDto.getStatus().equals(UserDtoStatus.ADD)) {
                            listToBeRemoved.add(assignmentDto);
                        } else {
                            assignmentDto.setStatus(UserDtoStatus.DELETE);
                        }
                    }
                }
                dto.setSelected(false);
            }
        }
        assignmentsList.removeAll(listToBeRemoved);
        target.add(to);
        target.add(from);
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
                dataProviderQuery = query;
            }

            @Override
            public ObjectQuery getQuery() {
                if (dataProviderQuery == null){
                    dataProviderQuery = new ObjectQuery();
                }
                if (filterModel != null && filterModel.getObject() != null){
                    dataProviderQuery.addFilter(filterModel.getObject());
                }
                return dataProviderQuery;
            }
        };
        return provider;
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
            RoleSelectionSpecification roleSpec = mis.getAssignableRoleSpecification(user, result);
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

    public <T extends FocusType> ListDataProvider<AssignmentEditorDto> getListDataProvider(final T user) {
        final ListDataProvider<AssignmentEditorDto> provider = new ListDataProvider<AssignmentEditorDto>(this, new IModel<List<AssignmentEditorDto>>() {
            @Override
            public List<AssignmentEditorDto> getObject() {
                return getAvailableAssignmentsDataList(user);
            }

            @Override
            public void setObject(List<AssignmentEditorDto> list) {
            }

            @Override
            public void detach() {

            }
        });
        return provider;
    }

    private GenericMultiValueLabelEditPanel createTenantContainer(){
        final GenericMultiValueLabelEditPanel tenantEditor = new GenericMultiValueLabelEditPanel<OrgType>(ID_TENANT_EDITOR,
                createTenantModel(),
                createStringResource("MultipleAssignmentSelector.tenant"), LABEL_SIZE, INPUT_SIZE, false){

            @Override
            protected void initDialog() {
                ModalWindow dialog = new OrgUnitBrowser(ID_MODAL_EDITOR){
                    @Override
                    protected void rowSelected(AjaxRequestTarget target, IModel<OrgTableDto> row, Operation operation) {
                        closeModalWindow(target);
                        tenantEditorObject.clear();
                        tenantEditorObject.add((OrgType)row.getObject().getObject());
                        target.add(getTenantEditorContainer());
                    }

                    @Override
                    protected ObjectQuery createSearchQuery() {
                        ObjectQuery query = new ObjectQuery();
                        ObjectFilter filter = EqualFilter.createEqual(OrgType.F_TENANT, OrgType.class,
                                getPageBase().getPrismContext(), null, true);
                        query.setFilter(filter);

                        return query;
                    }
                };
                add(dialog);
            }

            @Override
            protected boolean getLabelVisibility(){
                return false;
            }

            @Override
            protected IModel<String> createTextModel(final IModel<OrgType> model) {

                return new IModel<String>() {
                    @Override
                    public String getObject() {
                        return WebComponentUtil.getName(model.getObject().asPrismObject());
                    }

                    @Override
                    public void setObject(String s) {
                    }

                    @Override
                    public void detach() {
                    }
                };
            }

            @Override
            protected void removeValuePerformed(AjaxRequestTarget target, ListItem<OrgType> item) {
                tenantEditorObject.clear();
                tenantEditorObject.add(new OrgType());
                target.add(getTenantEditorContainer());
            }

            @Override
            protected void editValuePerformed(AjaxRequestTarget target, IModel<OrgType> rowModel) {
                OrgUnitBrowser window = (OrgUnitBrowser) get(ID_MODAL_EDITOR);
                window.show(target);
            }


        };
        tenantEditor.setOutputMarkupId(true);
        return tenantEditor;
    }

    private GenericMultiValueLabelEditPanel createOrgContainer(){
        final GenericMultiValueLabelEditPanel orgUnitEditor = new GenericMultiValueLabelEditPanel<OrgType>(ID_ORG_EDITOR,
                createOrgUnitModel(),
                createStringResource("MultipleAssignmentSelector.orgUnit"), LABEL_SIZE, INPUT_SIZE, false){

            @Override
            protected void initDialog() {
                ModalWindow dialog = new OrgUnitBrowser(ID_MODAL_EDITOR){
                    @Override
                    protected void rowSelected(AjaxRequestTarget target, IModel<OrgTableDto> row, Operation operation) {
                        closeModalWindow(target);
                        orgEditorObject.clear();
                        orgEditorObject.add((OrgType)row.getObject().getObject());
                        target.add(getOrgUnitEditorContainer());
                    }

                    @Override
                    protected ObjectQuery createSearchQuery() {
                        return new ObjectQuery();
                    }
                };
                add(dialog);
            }

            @Override
            protected boolean getLabelVisibility(){
                return false;
            }

            @Override
            protected IModel<String> createTextModel(final IModel<OrgType> model) {

                return new IModel<String>() {
                    @Override
                    public String getObject() {
                        return WebComponentUtil.getName(model.getObject().asPrismObject());
                    }

                    @Override
                    public void setObject(String s) {
                    }

                    @Override
                    public void detach() {
                    }
                };
            }

            @Override
            protected void removeValuePerformed(AjaxRequestTarget target, ListItem<OrgType> item) {
                orgEditorObject.clear();
                orgEditorObject.add(new OrgType());
                target.add(getOrgUnitEditorContainer());
            }

            @Override
            protected void editValuePerformed(AjaxRequestTarget target, IModel<OrgType> rowModel) {
                OrgUnitBrowser window = (OrgUnitBrowser) get(ID_MODAL_EDITOR);
                window.show(target);
            }
        };
        orgUnitEditor.setOutputMarkupId(true);
        return orgUnitEditor;
    }

    private AssignmentEditorDto createTenantOrgDto(){
        AssignmentEditorDtoType aType = AssignmentEditorDtoType.ROLE; //doesn't matter the type
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid("");
        targetRef.setType(aType.getQname());
        targetRef.setTargetName(new PolyStringType(""));

        AssignmentType assignment = new AssignmentType();
        assignment.setTargetRef(targetRef);

        AssignmentEditorDto tenantOrgDto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, getPageBase());
        if (tenantEditorObject != null && tenantEditorObject.size() > 0) {
            OrgType org = tenantEditorObject.get(0);
            ObjectViewDto<OrgType> dto = new ObjectViewDto(org.getOid(),
                    tenantEditorObject.get(0).getName() == null ? "" : tenantEditorObject.get(0).getName().toString());
            dto.setType(OrgType.class);
            tenantOrgDto.setTenantRef(dto);
        }
        if (orgEditorObject != null && orgEditorObject.size() > 0) {
            OrgType org = orgEditorObject.get(0);
            ObjectViewDto<OrgType> dto = new ObjectViewDto(org.getOid(),
                    orgEditorObject.get(0).getName() == null ? "" : orgEditorObject.get(0).getName().toString());
            dto.setType(OrgType.class);
            tenantOrgDto.setOrgRef(dto);
        }
        return tenantOrgDto;
    }

    private void setTenantAndOrgToAssignmentsList(List<AssignmentEditorDto> selectedItems){
        AssignmentEditorDto tenantOrgDto = createTenantOrgDto();
        for (AssignmentEditorDto dto : selectedItems){
            if (dto.isSelected()) {
                    dto.setTenantRef(tenantOrgDto.getTenantRef());
                    dto.setOrgRef(tenantOrgDto.getOrgRef());
            }
        }
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
        ObjectDataProvider temporaryProvider = new ObjectDataProvider(MultipleAssignmentSelectorPanel.this, type);
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

    private <T extends FocusType> List<AssignmentEditorDto> getAvailableAssignmentsDataList(T user){
        ObjectQuery query = null;
        List<AssignmentEditorDto> currentAssignments;
        if (user == null) {
            currentAssignments = getAssignmentsByType(assignmentsModel.getObject());
            if (currentAssignmentsProvider != null) {
                query = currentAssignmentsProvider.getQuery() == null ? new ObjectQuery() :
                        currentAssignmentsProvider.getQuery();
            }
        } else {
            List<AssignmentEditorDto> assignmentsList = getAssignmentEditorDtoList(user.getAssignment());
            currentAssignments = getAssignmentsByType(assignmentsList);
            if (type.equals(RoleType.class)){
                for (AssignmentEditorDto dto : currentAssignments){
                    dto.setTenantRef(null);
                    dto.setOrgRef(null);
                }
            }
            query = dataProvider.getQuery();
        }
        if (query != null){
            if (filterModel != null && filterModel.getObject() != null){
                query.addFilter(filterModel.getObject());
            }
            return applyQueryToListProvider(query, currentAssignments);
        }
        return currentAssignments;
    }


    private List<AssignmentEditorDto> getAssignmentEditorDtoList(List<AssignmentType> assignmentTypeList){
        List<AssignmentEditorDto> assignmentEditorDtoList = new ArrayList<>();
        for (AssignmentType assignmentType : assignmentTypeList){
            AssignmentEditorDto assignmentEditorDto = new AssignmentEditorDto(UserDtoStatus.MODIFY, assignmentType, getPageBase());
            assignmentEditorDtoList.add(assignmentEditorDto);
        }
        return assignmentEditorDtoList;
    }

    private IModel<List<OrgType>> createTenantModel(){
        return new IModel<List<OrgType>>() {
            @Override
            public List<OrgType> getObject() {
                return tenantEditorObject;
            }

            @Override
            public void setObject(List<OrgType> orgTypes) {
                tenantEditorObject.clear();
                tenantEditorObject.addAll(orgTypes);
            }

            @Override
            public void detach() {

            }
        };
    }

    private IModel<List<OrgType>> createOrgUnitModel(){
        return new IModel<List<OrgType>>() {
            @Override
            public List<OrgType> getObject() {
                return orgEditorObject;
            }

            @Override
            public void setObject(List<OrgType> orgTypes) {
                orgEditorObject.clear();
                orgEditorObject.addAll(orgTypes);
            }

            @Override
            public void detach() {

            }
        };
    }

    private WebMarkupContainer getTenantEditorContainer (){
        return (WebMarkupContainer) get(ID_FORM).get(ID_TENANT_EDITOR);
    }

    private WebMarkupContainer getOrgUnitEditorContainer (){
        return (WebMarkupContainer) get(ID_FORM).get(ID_ORG_EDITOR);
    }

    private boolean areEqualReferenceObjects(ObjectViewDto<OrgType> objRef1, ObjectViewDto<OrgType> objRef2){
        return (objRef1 == null && objRef2 == null) ||
                (objRef1 != null && objRef2 != null && objRef1.getOid() == null && objRef2.getOid() == null) ||
                (objRef1 != null && objRef2 != null && objRef1.getOid() != null &&
                        objRef2.getOid() != null && objRef1.getOid().equals(objRef2.getOid()));
    }

    private void reset(MultipleAssignmentSelector panel) {
        List<AssignmentEditorDto> assignmentsList = (List<AssignmentEditorDto>)panel.getModel().getObject();
        List<AssignmentEditorDto> listToBeRemoved = new ArrayList<>();
        for (AssignmentEditorDto dto : assignmentsList){
            if (dto.getStatus().equals(UserDtoStatus.ADD)) {
                listToBeRemoved.add(dto);
            } else if (dto.getStatus() == UserDtoStatus.DELETE) {
                dto.setStatus(UserDtoStatus.MODIFY);
            }
        }
        assignmentsList.removeAll(listToBeRemoved);
    }

}