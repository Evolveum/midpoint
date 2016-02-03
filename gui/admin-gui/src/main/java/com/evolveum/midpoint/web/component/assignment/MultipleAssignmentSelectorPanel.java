package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Honchar.
 */
public class MultipleAssignmentSelectorPanel<F extends FocusType> extends BasePanel<List<AssignmentEditorDto>> {
    private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;
    private static final String ID_BUTTON_REMOVE = "remove";
    private static final String ID_BUTTON_ADD = "add";
    private static final String ID_FORM = "form";
    private static final String ID_AVAILABLE_ASSIGNMENTS = "availableAssignments";
    private static final String ID_CURRENT_ASSIGNMENTS = "currentAssignments";
    private static final String DOT_CLASS = MultipleAssignmentSelectorPanel.class.getName();
    private Class<F> type;

    ObjectDataProvider dataProvider;
    private static final Trace LOGGER = TraceManager.getTrace(MultipleAssignmentSelectorPanel.class);

    public MultipleAssignmentSelectorPanel(String id, LoadableModel<List<AssignmentEditorDto>> assignmentsModel, Class<F> type) {
        super(id, assignmentsModel);
        this.assignmentsModel = assignmentsModel;
        this.type = type;
        initLayout();

    }

    private void initLayout() {
        IModel<List<AssignmentEditorDto>> availableAssignmentModel = createAvailableAssignmentModel();
        dataProvider = getAvailableAssignmentsDataProvider();
        final MultipleAssignmentSelector availableAssignmentsPanel = new MultipleAssignmentSelector<F>(ID_AVAILABLE_ASSIGNMENTS, availableAssignmentModel, dataProvider);
        availableAssignmentsPanel.setResetButtonVisibility(false);
        final MultipleAssignmentSelector currentAssignmentsPanel = new MultipleAssignmentSelector<F>(ID_CURRENT_ASSIGNMENTS, assignmentsModel, getCurrentAssignmentsDataProvider());

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
        Form<?> form = new Form<Void>(ID_FORM);
        form.add(remove);
        form.add(availableAssignmentsPanel);
        form.add(currentAssignmentsPanel);
        form.add(add);
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
        List<AssignmentEditorDto> fromProviderList = ((BaseSortableDataProvider) from.getProvider()).getAvailableData();
        List<AssignmentEditorDto> listToBeAdded = new ArrayList<>();
        List<AssignmentEditorDto> assignmentsList = assignmentsModel.getObject();
        for (AssignmentEditorDto dto : fromProviderList) {
            if (dto.isSelected()) {
                boolean toBeAdded = true;
                for (AssignmentEditorDto assignmentDto : assignmentsList) {
                    if (assignmentDto.getTargetRef().getOid().equals(dto.getTargetRef().getOid())) {
                        if (assignmentDto.getStatus().equals(UserDtoStatus.DELETE)) {
                            assignmentDto.setStatus(UserDtoStatus.MODIFY);
                        }
                        toBeAdded = false;
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

    private List<AssignmentEditorDto> getCurrentUsersAssignmentsByType() {
        List<AssignmentEditorDto> modelObj = assignmentsModel.getObject();
        List<AssignmentEditorDto> currentUsersAssignments = new ArrayList<>();
        for (AssignmentEditorDto dto : modelObj) {
            if (dto.getType().equals(AssignmentEditorDtoType.getType(type)) && !dto.getStatus().equals(UserDtoStatus.DELETE)) {
                currentUsersAssignments.add(dto);
            }
        }
        return currentUsersAssignments;
    }

    private ObjectDataProvider getAvailableAssignmentsDataProvider() {
        return new ObjectDataProvider<AssignmentEditorDto, F>(this, type) {

            @Override
            public AssignmentEditorDto createDataObjectWrapper(PrismObject<F> obj) {
                return AssignmentEditorDto.createDtoFromObject(obj.asObjectable(), UserDtoStatus.MODIFY, getPageBase());
            }

            @Override
            public ObjectQuery getQuery() {
                return new ObjectQuery();
            }
        };
    }

    private ISortableDataProvider getCurrentAssignmentsDataProvider() {
        ISortableDataProvider provider = new ListDataProvider(this, new IModel<List<AssignmentEditorDto>>() {
            @Override
            public List<AssignmentEditorDto> getObject() {
                return getCurrentUsersAssignmentsByType();
            }

            @Override
            public void setObject(List<AssignmentEditorDto> list) {
            }

            @Override
            public void detach() {

            }
        });
        ISortState sort = provider.getSortState();
        if (sort != null) {

        }
        return provider;
    }

}