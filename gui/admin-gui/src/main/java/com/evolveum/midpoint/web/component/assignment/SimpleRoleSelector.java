/*
 * Copyright (C) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author semancik
 */
public class SimpleRoleSelector<R extends AbstractRoleType> extends BasePanel<PrismContainerWrapper<AssignmentType>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SimpleRoleSelector.class);

    private static final String ID_LIST = "list";
    private static final String ID_ITEM = "item";
    private static final String ID_BUTTON_RESET = "buttonReset";

    private List<PrismObject<R>> availableRoles;

    public SimpleRoleSelector(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentModel, List<PrismObject<R>> availableRoles) {
        super(id, assignmentModel);

        this.availableRoles = availableRoles;
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);
        ListView<PrismObject<R>> list = new ListView<>(ID_LIST, availableRoles) {
            @Override
            protected void populateItem(ListItem<PrismObject<R>> item) {
                item.add(createRoleLink(ID_ITEM, item.getModel()));
            }
        };
        add(list);

        AjaxLink<String> buttonReset = new AjaxLink<>(ID_BUTTON_RESET) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                resetPerformed(target);
            }
        };
        buttonReset.setBody(createStringResource("SimpleRoleSelector.reset"));
        add(buttonReset);
    }

    private Component createRoleLink(String id, IModel<PrismObject<R>> model) {
        AjaxLink<PrismObject<R>> button = new AjaxLink<>(id, model) {

            @Override
            public IModel<?> getBody() {
                return () -> getModelObject().asObjectable().getName().getOrig();
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                toggleRolePerformed(target, model.getObject());
            }
        };

        button.add(AttributeAppender.append("class", () -> isSelected(model.getObject()) ? "active" : null));
        button.add(AttributeAppender.append("title", () -> model.getObject().asObjectable().getDescription()));

        return button;
    }

    private boolean isSelected(PrismObject<R> role) {
        for (PrismContainerValueWrapper<? extends AssignmentType> assignmentContainer : getModel().getObject().getValues()) {
            AssignmentType assignment = assignmentContainer.getRealValue();
            if (!willProcessAssignment(assignment)) {
                continue;
            }

            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null && role.getOid().equals(targetRef.getOid())) {
                if (assignmentContainer.getStatus() != ValueStatus.DELETED) {
                    return true;
                }
            }
        }

        return false;
    }

    private void toggleRolePerformed(AjaxRequestTarget target, PrismObject<R> role) {
        LOGGER.trace("{} CLICK: {}", this, role);

        Iterator<? extends PrismContainerValueWrapper<? extends AssignmentType>> iterator =
                getModel().getObject().getValues().iterator();

        boolean found = false;
        while (iterator.hasNext()) {
            PrismContainerValueWrapper<? extends AssignmentType> assignmentContainer = iterator.next();
            AssignmentType assignment = assignmentContainer.getRealValue();
            if (!willProcessAssignment(assignment)) {
                continue;
            }

            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef == null || !role.getOid().equals(targetRef.getOid())) {
                continue;
            }

            found = true;

            switch (assignmentContainer.getStatus()) {
                case ADDED:
                    iterator.remove();
                    break;
                case DELETED:
                    assignmentContainer.setStatus(ValueStatus.NOT_CHANGED);
                    break;
                case NOT_CHANGED:
                    assignmentContainer.setStatus(ValueStatus.DELETED);
            }
        }

        if (!found) {
            AssignmentType newAssignment = ObjectTypeUtil.createAssignmentTo(role, SchemaConstants.ORG_DEFAULT);
            WebPrismUtil.createNewValueWrapper(getModelObject(), newAssignment.asPrismContainerValue(), getPageBase(), target);
        }

        target.add(this);
    }

    private void resetPerformed(AjaxRequestTarget target) {
        Iterator<? extends PrismContainerValueWrapper<? extends AssignmentType>> iterator =
                getModel().getObject().getValues().iterator();

        while (iterator.hasNext()) {
            PrismContainerValueWrapper<? extends AssignmentType> assignmentContainer = iterator.next();
            AssignmentType assignment = assignmentContainer.getRealValue();

            if (!isManagedRole(assignment) || !willProcessAssignment(assignment)) {
                continue;
            }

            if (assignmentContainer.getStatus() == ValueStatus.ADDED) {
                iterator.remove();
            } else if (assignmentContainer.getStatus() == ValueStatus.DELETED) {
                assignmentContainer.setStatus(ValueStatus.NOT_CHANGED);
            }
        }

        target.add(this);
    }

    protected boolean willProcessAssignment(AssignmentType dto) {
        return true;
    }

    protected boolean isManagedRole(AssignmentType assignment) {
        ObjectReferenceType targetRef = assignment.getTargetRef();
        if (targetRef == null || targetRef.getOid() == null) {
            return false;
        }

        for (PrismObject<R> availableRole : availableRoles) {
            if (availableRole.getOid().equals(targetRef.getOid())) {
                return true;
            }
        }
        return false;
    }
}
