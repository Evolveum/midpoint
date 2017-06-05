/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.form.multivalue.GenericMultiValueLabelEditPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel contains available list of focus type items and
 * the list of assigned items of the same type with the
 * possibility of editing the list of assignments.
 * 
 * @author Kate Honchar
 */
public class MultipleAssignmentSelectorPanel<F extends FocusType, H extends FocusType, G extends FocusType>
        extends BasePanel<List<AssignmentEditorDto>> {          //G - type of the object which is to be assigned (a.g. assign a role (RoleType))
                                                                //F - type of the focus which is opened for editing (e.g. edit user - UserType)
                                                                //H - a type of filter object (e.g. Filter by user - UserType)
	private static final long serialVersionUID = 1L;
	
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
    private Class<G> type;

    private List<OrgType> tenantEditorObject = new ArrayList<>();
    private List<OrgType> orgEditorObject = new ArrayList<>();
    private PrismObject<F> focus;
    private static final Trace LOGGER = TraceManager.getTrace(MultipleAssignmentSelectorPanel.class);
    private static final String OPERATION_LOAD_ASSIGNABLE_ROLES = DOT_CLASS + "loadAssignableRoles";

    public MultipleAssignmentSelectorPanel(String id, LoadableModel<List<AssignmentEditorDto>> assignmentsModel,
			PrismObject<F> focus, Class<H> targetFocusClass, Class<G> type, PageBase page) {
        super(id, assignmentsModel);
        this.assignmentsModel = assignmentsModel;
        this.type = type;
        this.focus = focus;
        tenantEditorObject.add(new OrgType());
        orgEditorObject.add(new OrgType());
        initLayout(targetFocusClass, page);

    }

    private void initLayout(Class<H> targetFocusClass, PageBase page) {

        IModel<List<AssignmentEditorDto>> availableAssignmentModel = createAvailableAssignmentModel();
        final MultipleAssignmentSelector availableAssignmentsPanel = new MultipleAssignmentSelector<F, H>(ID_AVAILABLE_ASSIGNMENTS,
                availableAssignmentModel, targetFocusClass, type, focus, getFilterModel(true), page);
        final MultipleAssignmentSelector currentAssignmentsPanel = new MultipleAssignmentSelector<F, H>(ID_CURRENT_ASSIGNMENTS,
                assignmentsModel, targetFocusClass, type, null, getFilterModel(true), page) {
        	private static final long serialVersionUID = 1L;
        	
            @Override
	        protected List<AssignmentEditorDto> getListProviderDataList(){
	                return assignmentsModel.getObject();
	            }
	        };
        currentAssignmentsPanel.setFilterButtonVisibility(false);

        AjaxButton add = new AjaxButton(ID_BUTTON_ADD) {
        	private static final long serialVersionUID = 1L;
        	
            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form form) {
                addToAssignmentsModel(target, availableAssignmentsPanel, currentAssignmentsPanel);
            }
        };

        AjaxButton remove = new AjaxButton(ID_BUTTON_REMOVE) {
        	private static final long serialVersionUID = 1L;
        	
            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form form) {
                deleteFromAssignmentsModel(target, currentAssignmentsPanel, availableAssignmentsPanel);
            }
        };
        remove.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isEnabled() {
            	// TODO: the modify authorization here is probably wrong.
        		// It is a model autz. UI autz should be here instead?
                return WebComponentUtil.isAuthorized(ModelAuthorizationAction.UNASSIGN.getUrl());
            }
        });

        AjaxLink<String> buttonReset = new AjaxLink<String>(ID_BUTTON_RESET) {
        	private static final long serialVersionUID = 1L;
        	
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
                    String assignmentOid = getAssignmentDtoOid(assignmentDto);
                    String dtoOid = getAssignmentDtoOid(dto);
                    if (assignmentOid != null &&
                            dtoOid != null && assignmentOid.equals(dtoOid)) {
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
                    String assignmentDtoOid = getAssignmentDtoOid(assignmentDto);
                    String dtoOid = getAssignmentDtoOid(dto);
                    if (assignmentDtoOid != null &&
                            dtoOid != null && assignmentDtoOid.equals(dtoOid) &&
                            areEqualReferenceObjects(assignmentDto.getTenantRef(), dto.getTenantRef()) &&
                            areEqualReferenceObjects(assignmentDto.getOrgRef(), dto.getOrgRef())) {
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

    private  IModel<ObjectFilter> getFilterModel(final boolean isRequestableFilter){
        return new IModel<ObjectFilter>() {
        	private static final long serialVersionUID = 1L;
        	
            @Override
            public ObjectFilter getObject() {
                ObjectFilter archivedRolesFilter = QueryBuilder.queryFor(RoleType.class, getPageBase().getPrismContext())
                        .item(RoleType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).eq(ActivationStatusType.ARCHIVED)
                        .buildFilter();
                ObjectFilter filter;
                if (isRequestableFilter) {
                    ObjectFilter assignableRolesFilter = getAssignableRolesFilter();
                    if (assignableRolesFilter instanceof NotFilter) {
                        return null;
                    } else if (assignableRolesFilter != null) {
                        filter = AndFilter.createAnd(assignableRolesFilter, new NotFilter(archivedRolesFilter));
                    } else {
                        filter = new NotFilter(archivedRolesFilter);
                    }
                    return filter;
                } else {
                    return new NotFilter(archivedRolesFilter);
                }
            }

            @Override
            public void setObject(ObjectFilter objectFilter) {

            }

            @Override
            public void detach() {

            }
        };
    }

    private ObjectFilter getAssignableRolesFilter() {
        LOGGER.debug("Loading roles which the current user has right to assign");
        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNABLE_ROLES);
        ObjectFilter filter = null;
        try {
            PageBase pb = getPageBase();
            ModelInteractionService mis = pb.getModelInteractionService();
            RoleSelectionSpecification roleSpec = mis.getAssignableRoleSpecification(focus, result);
            filter = roleSpec.getFilter();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load available roles", ex);
            result.recordFatalError("Couldn't load available roles", ex);
        } finally {
            result.recomputeStatus();
        }
        if (!result.isSuccess() && !result.isHandledError()) {
            getPageBase().showResult(result);
        }
        return filter;
    }

    private GenericMultiValueLabelEditPanel createTenantContainer(){
        final GenericMultiValueLabelEditPanel tenantEditor = new GenericMultiValueLabelEditPanel<OrgType>(ID_TENANT_EDITOR,
                createTenantModel(),
                createStringResource("MultipleAssignmentSelector.tenant"), LABEL_SIZE, INPUT_SIZE, false) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected boolean getLabelVisibility(){
                return false;
            }

            @Override
            protected IModel<String> createTextModel(final IModel<OrgType> model) {

                return new IModel<String>() {
                	private static final long serialVersionUID = 1L;
                	
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
                List<QName> supportedTypes = new ArrayList<>();
                supportedTypes.add(getPageBase().getPrismContext().getSchemaRegistry()
                        .findObjectDefinitionByCompileTimeClass(OrgType.class).getTypeName());

                    ObjectFilter filter = QueryBuilder.queryFor(OrgType.class, getPageBase().getPrismContext())
                            .item(OrgType.F_TENANT).eq(true)
                            .buildFilter();

                ObjectBrowserPanel<OrgType> tenantPanel = new ObjectBrowserPanel<OrgType>(getPageBase().getMainPopupBodyId(),
                        OrgType.class, supportedTypes, false, getPageBase(), filter) {
                	private static final long serialVersionUID = 1L;
                	
                    @Override
                    protected void onSelectPerformed(AjaxRequestTarget target, OrgType org) {
                        super.onSelectPerformed(target, org);
                        tenantEditorObject.clear();
                        tenantEditorObject.add(org);
                        target.add(getTenantEditorContainer());
                    }
                };

//                OrgTreeAssignablePanel tenantPanel = new OrgTreeAssignablePanel(
//                        getPageBase().getMainPopupBodyId(), false, getPageBase()) {
//
//                    @Override
//                    protected void onItemSelect(SelectableBean<OrgType> selected, AjaxRequestTarget target) {
//                        closeModalWindow(target);
//                        tenantEditorObject.clear();
//                        tenantEditorObject.add(selected.getValue());
//                        target.add(getTenantEditorContainer());                    }
//                };
                getPageBase().showMainPopup(tenantPanel, target);
            }

//            @Override
//            protected void setDialogSize() {
//                getPageBase().getMainPopup().setInitialWidth(900);
//                getPageBase().getMainPopup().setInitialHeight(700);
//            }


        };
        tenantEditor.setOutputMarkupId(true);
        return tenantEditor;
    }

    private GenericMultiValueLabelEditPanel createOrgContainer(){
        final GenericMultiValueLabelEditPanel orgUnitEditor = new GenericMultiValueLabelEditPanel<OrgType>(ID_ORG_EDITOR,
                createOrgUnitModel(),
                createStringResource("MultipleAssignmentSelector.orgUnit"), LABEL_SIZE, INPUT_SIZE, false) {
        	private static final long serialVersionUID = 1L;

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
                List<QName> supportedTypes = new ArrayList<>();
                supportedTypes.add(getPageBase().getPrismContext().getSchemaRegistry()
                        .findObjectDefinitionByCompileTimeClass(OrgType.class).getTypeName());
                ObjectBrowserPanel<OrgType> orgPanel = new ObjectBrowserPanel<OrgType>(getPageBase().getMainPopupBodyId(),
                        OrgType.class, supportedTypes, false, getPageBase()) {
                	private static final long serialVersionUID = 1L;
                	
                    @Override
                    protected void onSelectPerformed(AjaxRequestTarget target, OrgType org) {
                        super.onSelectPerformed(target, org);
                        orgEditorObject.clear();
                        orgEditorObject.add(org);
                        target.add(getOrgUnitEditorContainer());
                    }
                };
                getPageBase().showMainPopup(orgPanel, target);
            }

//            @Override
//            protected void setDialogSize() {
//                getPageBase().getMainPopup().setInitialWidth(900);
//                getPageBase().getMainPopup().setInitialHeight(700);
//            }
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

    private IModel<List<OrgType>> createTenantModel(){
        return new IModel<List<OrgType>>() {
        	private static final long serialVersionUID = 1L;
        	
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
        	private static final long serialVersionUID = 1L;
        	
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

    private String getAssignmentDtoOid(AssignmentEditorDto assignmentDto){
        ObjectReferenceType assignmentDtoTargetRef = assignmentDto.getTargetRef();
        String assignmentDtoOid = null;
        if (assignmentDtoTargetRef != null){
            assignmentDtoOid = assignmentDtoTargetRef.getOid();
        } else {
            AssignmentType oldValue = assignmentDto.getOldValue() != null ? assignmentDto.getOldValue().getValue() : null;
            assignmentDtoOid = oldValue != null ?
                    (oldValue.getTarget() == null ? null : oldValue.getTarget().getOid())
                    : null;
        }
        return assignmentDtoOid;
    }
}