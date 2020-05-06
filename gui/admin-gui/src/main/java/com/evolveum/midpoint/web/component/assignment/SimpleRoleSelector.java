/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author semancik
 */
public class SimpleRoleSelector<F extends FocusType, R extends AbstractRoleType> extends BasePanel<PrismContainerWrapper<AssignmentType>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SimpleRoleSelector.class);

    private static final String ID_LIST = "list";
    private static final String ID_ITEM = "item";
    private static final String ID_BUTTON_RESET = "buttonReset";

    List<PrismObject<R>> availableRoles;

    public SimpleRoleSelector(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentModel, List<PrismObject<R>> availableRoles) {
        super(id, assignmentModel);
        this.availableRoles = availableRoles;
        initLayout();
    }

    public List<AssignmentType> getAssignmentTypeList() {
        return null;
    }

    public String getExcludeOid() {
        return null;
    }

    private void initLayout() {
        setOutputMarkupId(true);
        ListView<PrismObject<R>> list = new ListView<PrismObject<R>>(ID_LIST, availableRoles) {
            @Override
            protected void populateItem(ListItem<PrismObject<R>> item) {
                item.add(createRoleLink(ID_ITEM, item.getModel()));
            }
        };
        list.setOutputMarkupId(true);
        add(list);

        AjaxLink<String> buttonReset = new AjaxLink<String>(ID_BUTTON_RESET) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                reset();
                target.add(SimpleRoleSelector.this);
            }
        };
        buttonReset.setBody(createStringResource("SimpleRoleSelector.reset"));
        add(buttonReset);
    }


    private Component createRoleLink(String id, IModel<PrismObject<R>> model) {
        AjaxLink<PrismObject<R>> button = new AjaxLink<PrismObject<R>>(id, model) {

            @Override
            public IModel<?> getBody() {
                return new Model<>(getModel().getObject().asObjectable().getName().getOrig());
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                LOGGER.trace("{} CLICK: {}", this, getModel().getObject());
                toggleRole(getModel().getObject(), target);
                target.add(this);
            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                PrismObject<R> role = getModel().getObject();
                if (isSelected(role)) {
                    tag.put("class", "list-group-item active");
                } else {
                    tag.put("class", "list-group-item");
                }
                String description = role.asObjectable().getDescription();
                if (description != null) {
                    tag.put("title", description);
                }
            }
        };
        button.setOutputMarkupId(true);
        return button;
    }


    private boolean isSelected(PrismObject<R> role) {
        for (PrismContainerValueWrapper<AssignmentType> assignmentContainer: getModel().getObject().getValues()) {
            AssignmentType assignment = assignmentContainer.getRealValue();
            if (willProcessAssignment(assignment)) {
                ObjectReferenceType targetRef = assignment.getTargetRef();
                if (targetRef != null && role.getOid().equals(targetRef.getOid())) {
                    if (assignmentContainer.getStatus() != ValueStatus.DELETED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void toggleRole(PrismObject<R> role, AjaxRequestTarget target) {
        Iterator<PrismContainerValueWrapper<AssignmentType>> iterator = getModel().getObject().getValues().iterator();
        while (iterator.hasNext()) {
            PrismContainerValueWrapper<AssignmentType> assignmentContainer = iterator.next();
            AssignmentType assignment = assignmentContainer.getRealValue();
            if (willProcessAssignment(assignment)) {
                ObjectReferenceType targetRef = assignment.getTargetRef();
                if (targetRef != null && role.getOid().equals(targetRef.getOid())) {
                    if (assignmentContainer.getStatus() == ValueStatus.ADDED) {
                        iterator.remove();
                    } else {
                        assignmentContainer.setStatus(ValueStatus.DELETED);
                    }
                    return;
                }
            }
        }

        AssignmentType newAssignment = ObjectTypeUtil.createAssignmentTo(role, SchemaConstants.ORG_DEFAULT);
        WebPrismUtil.createNewValueWrapper(getModelObject(), newAssignment.asPrismContainerValue(), getPageBase(), target);
//        AssignmentType newAssignment = ObjectTypeUtil.createAssignmentTo(role, prismContext);
        //TODO
        //create ContainerValueWrapper for new assignment
//        getAssignmentModel().getObject().add(newAssignment);
    }

    private void reset() {
        Iterator<PrismContainerValueWrapper<AssignmentType>> iterator = getModel().getObject().getValues().iterator();
        while (iterator.hasNext()) {
            PrismContainerValueWrapper<AssignmentType> assignmentContainer = iterator.next();
            AssignmentType assignment = assignmentContainer.getRealValue();
            if (isManagedRole(assignment) && willProcessAssignment(assignment)) {
                if (assignmentContainer.getStatus() == ValueStatus.ADDED) {
                    iterator.remove();
                } else if (assignmentContainer.getStatus() == ValueStatus.DELETED) {
                    //what status to use for container?
//                    assignmentContainer.setStatus(UserDtoStatus.MODIFY);
                }
            }
        }
    }

    protected boolean willProcessAssignment(AssignmentType dto) {
        return true;
    }

    protected boolean isManagedRole(AssignmentType assignment) {
        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (targetRef == null || targetRef.getOid() == null) {
            return false;
        }
        for (PrismObject<R> availableRole: availableRoles) {
            if (availableRole.getOid().equals(targetRef.getOid())) {
                return true;
            }
        }
        return false;
    }

}
