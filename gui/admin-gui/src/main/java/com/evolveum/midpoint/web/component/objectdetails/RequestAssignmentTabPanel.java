package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.springframework.expression.spel.ast.Assign;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Honchar.
 */
public class RequestAssignmentTabPanel<F extends FocusType> extends AbstractObjectTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_BUTTON_REMOVE = "remove";
    private static final String ID_BUTTON_ADD = "add";
    private static final String ID_FORM = "form";
    private static final String ID_AVAILABLE_ROLES = "availableRoles";
    private static final String ID_CURRENT_ROLES = "currentRoles";
    private static final String DOT_CLASS = RequestAssignmentTabPanel.class.getName();
    private static final String OPERATION_LOAD_ROLES = DOT_CLASS + "loadRoles";

    private List<SelectableBean<RoleType>> selectedAvailableRolesList = new ArrayList<>();
    private List<SelectableBean<RoleType>> selectedCurrentRolesList = new ArrayList<>();
    private List<SelectableBean<RoleType>> providerList;
    private static final Trace LOGGER = TraceManager.getTrace(RequestAssignmentTabPanel.class);

    private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;

    public RequestAssignmentTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusWrapperModel,
                                    LoadableModel<List<AssignmentEditorDto>> assignmentsModel, PageBase page) {
        super(id, mainForm, focusWrapperModel, page);
        this.assignmentsModel = assignmentsModel;
        providerList = getCurrentUsersRoles();
        initLayout();
    }

    private void initLayout() {

        IModel<List<SelectableBean<RoleType>>> availableRolesModel = createAvailableAssignmentModel();
        IModel<List<SelectableBean<RoleType>>> currentRolesModel = createCurrentAssignmentModel();
        final MultipleAssignmentSelector availableRolesPanel = new MultipleAssignmentSelector<RoleType>(ID_AVAILABLE_ROLES, availableRolesModel, getAvailableRolesDataProvider());
        availableRolesPanel.setResetButtonVisibility(false);
        final MultipleAssignmentSelector currentRolesPanel = new MultipleAssignmentSelector<RoleType>(ID_CURRENT_ROLES, currentRolesModel, getCurrentRolesDataProvider());

        AjaxButton add = new AjaxButton(ID_BUTTON_ADD) {
            @Override

            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form form) {
                addToProviderList(target, availableRolesPanel, currentRolesPanel);
            }
        };
//        add(add);

        Form<?> form = new Form<Void>(ID_FORM) {
            @Override
            protected void onSubmit() {
                updateAssignmentsModel();
            }
        };

        AjaxButton remove = new AjaxButton(ID_BUTTON_REMOVE) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form form) {
                deleteFromProviderList(target, currentRolesPanel, availableRolesPanel);
            }
        };
        form.add(remove);
        form.add(availableRolesPanel);
        form.add(currentRolesPanel);
        form.add(add);
        add(form);


    }

    private void addToProviderList(AjaxRequestTarget target, MultipleAssignmentSelector from, MultipleAssignmentSelector to) {
        List<SelectableBean<RoleType>> fromList = (List<SelectableBean<RoleType>>)from.getModel().getObject();
        for (SelectableBean<RoleType> role : fromList) {
            boolean toBeAdded = true;
            for (SelectableBean<RoleType> providerRole : providerList){
                if (providerRole.getValue().getOid().equals(role.getValue().getOid())){
                    toBeAdded = false;
                    break;
                }
            }
            if (toBeAdded){
                providerList.add(role);
            }
            role.setSelected(false);
        }
        fromList.clear();
        target.add(to);
        target.add(from);
    }

    private void deleteFromProviderList(AjaxRequestTarget target, MultipleAssignmentSelector from, MultipleAssignmentSelector to){
        List<SelectableBean<RoleType>> fromList = (List<SelectableBean<RoleType>>)from.getModel().getObject();
        for (SelectableBean<RoleType> role : fromList) {
            role.setSelected(false);
            if (providerList.contains(role)) {

                providerList.remove(role);
            }
        }

        target.add(to);
        target.add(from);

    }

    private List<PrismObject<RoleType>> getAvailableRoles(){
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ROLES);
        List<PrismObject<RoleType>> availableRoles;
        try {
            availableRoles = getPageBase().getModelService().searchObjects(RoleType.class, null, null, task, task.getResult());
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException e) {
            task.getResult().recordFatalError(e);
            LoggingUtils.logException(LOGGER, "Couldn't load roles", e);
            availableRoles = new ArrayList<>();
            // TODO: better errror reporting
        }
        return availableRoles;
    }

    private List<SelectableBean<RoleType>> getCurrentUsersRoles() {
        List<AssignmentEditorDto> modelObj = assignmentsModel.getObject();
        List<SelectableBean<RoleType>> currentUsersRoles = new ArrayList<>();
        for (AssignmentEditorDto dto : modelObj){
            if (dto.getType().equals(AssignmentEditorDtoType.ROLE)){
                currentUsersRoles.add(createSelectableBeanFromDto(dto));
            }
        }
        return currentUsersRoles;
    }

    private SelectableBean<RoleType> createSelectableBeanFromDto(AssignmentEditorDto dto){
        SelectableBean<RoleType> selectableBean = new SelectableBean<>();
        selectableBean.setValue(createRoleTypeFromDto(dto));
        return selectableBean;
    }

    private AssignmentEditorDto createAssignmentEditorDto(SelectableBean<RoleType>  role){
        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid(role.getValue().getOid());
        targetRef.setType(RoleType.COMPLEX_TYPE);
        targetRef.setTargetName(role.getValue().getName());

        AssignmentType assignment = new AssignmentType();
        assignment.setTargetRef(targetRef);

        AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, getPageBase());
        dto.setMinimized(false);
        dto.setShowEmpty(true);

        return dto;
    }

    private ObjectDataProvider getAvailableRolesDataProvider(){
        ObjectDataProvider provider = new ObjectDataProvider(RequestAssignmentTabPanel.this, RoleType.class);
        provider.setQuery(new ObjectQuery());
        return provider;
    }

    private ISortableDataProvider getCurrentRolesDataProvider(){
        ISortableDataProvider provider = new ListDataProvider(this, new IModel<List<SelectableBean<RoleType>>>() {
            @Override
            public List<SelectableBean<RoleType>> getObject() {
                return providerList;
            }

            @Override
            public void setObject(List<SelectableBean<RoleType>> list) {
                providerList = list;
            }

            @Override
            public void detach() {

            }
        });
        ISortState sort = provider.getSortState();
        if (sort != null){

        }
        return provider;
    }

    private IModel<List<SelectableBean<RoleType>>> createAvailableAssignmentModel(){
        return new IModel<List<SelectableBean<RoleType>>>() {
            @Override
            public List<SelectableBean<RoleType>> getObject() {
                return selectedAvailableRolesList;
            }

            @Override
            public void setObject(List<SelectableBean<RoleType>> assignmentList) {
                selectedAvailableRolesList = assignmentList;
            }

            @Override
            public void detach() {

            }
        };
    }
    private IModel<List<SelectableBean<RoleType>>> createCurrentAssignmentModel(){
        return new IModel<List<SelectableBean<RoleType>>>() {
            @Override
            public List<SelectableBean<RoleType>> getObject() {
                return selectedCurrentRolesList;
            }

            @Override
            public void setObject(List<SelectableBean<RoleType>> assignmentList) {
                selectedCurrentRolesList = assignmentList;
            }

            @Override
            public void detach() {

            }
        };
    }

    private RoleType createRoleTypeFromDto(AssignmentEditorDto dto){
        ObjectReferenceType targetRef = dto.getTargetRef();

        RoleType role = new RoleType();
        role.setOid(targetRef.getOid());
        role.setName(targetRef.getTargetName());
        return role;
    }

    private void updateAssignmentsModel(){
        for (AssignmentEditorDto dto : assignmentsModel.getObject()){
            if (dto.getType().equals(AssignmentEditorDtoType.ROLE)){
                boolean toBeDeleted = true;
                for (SelectableBean<RoleType> role : providerList){
                    if (dto.getTargetRef().getOid().equals(role.getValue().getOid())){
                        toBeDeleted = false;
                    }
                }
                if (toBeDeleted){
                    dto.setStatus(UserDtoStatus.DELETE);
                }
            }
        }
        List<AssignmentEditorDto> assignmentsToAdd = new ArrayList<>();
        for (SelectableBean<RoleType> role : providerList){
                boolean toBeAdded = true;
                for (AssignmentEditorDto dto : assignmentsModel.getObject()){
                    if (dto.getTargetRef().getOid().equals(role.getValue().getOid())){
                        toBeAdded = false;
                    }
                }
                if (toBeAdded){
                    AssignmentEditorDto newAssignmentDto = createAssignmentEditorDto(role);
                    newAssignmentDto.setStatus(UserDtoStatus.ADD);
                    assignmentsToAdd.add(newAssignmentDto);
                }
        }
        assignmentsModel.getObject().addAll(assignmentsToAdd);
    }
}
