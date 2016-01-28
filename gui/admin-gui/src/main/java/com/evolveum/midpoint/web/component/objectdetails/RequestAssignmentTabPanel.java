package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.assignment.SimpleRoleSelector;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentPreviewDialog;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;

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

    private static final Trace LOGGER = TraceManager.getTrace(RequestAssignmentTabPanel.class);

    private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;

    public RequestAssignmentTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusWrapperModel,
                                    LoadableModel<List<AssignmentEditorDto>> assignmentsModel, PageBase page) {
        super(id, mainForm, focusWrapperModel, page);
        this.assignmentsModel = assignmentsModel;
        initLayout();
    }

    private void initLayout() {
        SimpleRoleSelector availableRolesPanel = new SimpleRoleSelector<UserType,RoleType>(ID_AVAILABLE_ROLES, assignmentsModel, getAvailableRolesDataProvider());
        availableRolesPanel.setResetButtonVisibility(false);
        SimpleRoleSelector currentRolesPanel = new SimpleRoleSelector<UserType,RoleType>(ID_CURRENT_ROLES, assignmentsModel, getCurrentRolesDataProvider());

        AjaxButton add = new AjaxButton(ID_BUTTON_ADD) {
            @Override

            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form form) {
//                update(target, selectedOriginals, existingValuesPanel, choicePanel);
            }
        };
//        add(add);

        Form<?> form = new Form<Void>(ID_FORM) {
            @Override
            protected void onSubmit() {

//                info("Selected Number : " + selectedNumber);

            }
        };

        AjaxButton remove = new AjaxButton(ID_BUTTON_REMOVE) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form form) {
//                update(target, selectedDestinations, choicePanel, existingValuesPanel);
            }
        };
        form.add(remove);
        form.add(availableRolesPanel);
        form.add(currentRolesPanel);
        form.add(add);
        add(form);


    }

    private void update(AjaxRequestTarget target, List<String> selections, ListMultipleChoice from, ListMultipleChoice to) {
        for (String destination : selections) {
            List choices =from.getChoices();
            List toChoices = to.getChoices();
            if (choices != null && toChoices != null ){

            }
            if (!to.getChoices().contains(destination)) {
                to.getChoices().add(destination);
                choices.remove(destination);
                from.setChoices(choices);
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

    private List<SelectableBean<RoleType>> geCurrentUsersRoles() {
        List<AssignmentEditorDto> modelObj = assignmentsModel.getObject();
        List<SelectableBean<RoleType>> currentUsersRoles = new ArrayList<>();
        for (AssignmentEditorDto dto : modelObj){
            if (dto.getType().equals(AssignmentEditorDtoType.ROLE)){
                ObjectReferenceType targetRef = dto.getTargetRef();
                RoleType role = new RoleType();
                role.setOid(targetRef.getOid());
                role.setName(targetRef.getTargetName());
//                role.setDisplayName(targetRef.getTargetName());
                SelectableBean<RoleType> selectableBean = new SelectableBean<>();
                selectableBean.setValue(role);
                currentUsersRoles.add(selectableBean);
            }
        }
        return currentUsersRoles;
    }


    private ObjectDataProvider getAvailableRolesDataProvider(){
        ObjectDataProvider provider = new ObjectDataProvider(RequestAssignmentTabPanel.this, RoleType.class);
        provider.setQuery(new ObjectQuery());
        return provider;
    }

    private ISortableDataProvider getCurrentRolesDataProvider(){
        ISortableDataProvider provider = new ListDataProvider(this, new IModel<List<SelectableBean<RoleType>>>() {
            @Override
            public List getObject() {
                return geCurrentUsersRoles();
            }

            @Override
            public void setObject(List list) {

            }

            @Override
            public void detach() {

            }
        });
        return provider;
    }

}
