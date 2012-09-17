/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import org.apache.commons.lang.Validate;

/**
 * //todo consolidate [lazyman]
 *
 * @author lazyman
 */
public class AssignmentEditorDto extends SelectableBean {

    public static final String F_TYPE = "type";
    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_ACTIVATION = "activation";

    private String name;
    private AssignmentEditorDtoType type;
    private UserDtoStatus status;
    private AssignmentType oldAssignment;
    private AssignmentType newAssignment;

    private boolean showEmpty = false;
    private boolean minimized = true;

    public AssignmentEditorDto(String name, AssignmentEditorDtoType type, UserDtoStatus status, AssignmentType assignment) {
        Validate.notNull(status, "User dto status must not be null.");
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(assignment, "Assignment must not be null.");

        this.name = name;
        this.type = type;
        this.status = status;
        this.newAssignment = assignment;
    }

    public boolean isMinimized() {
        return minimized;
    }

    public void setMinimized(boolean minimized) {
        this.minimized = minimized;
    }

    public boolean isShowEmpty() {
        return showEmpty;
    }

    public void setShowEmpty(boolean showEmpty) {
        this.showEmpty = showEmpty;
    }

    public UserDtoStatus getStatus() {
        return status;
    }

    public ActivationType getActivation() {
        ActivationType type = newAssignment.getActivation();
        if (type == null) {
            type = new ActivationType();
            newAssignment.setActivation(type);
        }

        return type;
    }

    public String getName() {
        return name;
    }

    public ObjectReferenceType getTargetRef() {
        return newAssignment.getTargetRef();
    }

    public AssignmentEditorDtoType getType() {
        return type;
    }

    public void setStatus(UserDtoStatus status) {
        this.status = status;
    }

    public void startEditing() {
        if (oldAssignment != null) {
            return;
        }

        PrismContainerValue value = newAssignment.asPrismContainerValue();

        PrismContainerValue oldValue = value.clone();
        oldAssignment = new AssignmentType();
        oldAssignment.setupContainerValue(oldValue);
    }

    public boolean isModified() {
        return oldAssignment != null && !oldAssignment.equals(newAssignment);
    }

    public PrismContainerValue getOldValue() {
        return oldAssignment != null ? oldAssignment.asPrismContainerValue() : null;
    }

    public PrismContainerValue getNewValue() {
        return newAssignment.asPrismContainerValue();
    }

    public String getDescription() {
        return newAssignment.getDescription();
    }

    public void setDescription(String description) {
        newAssignment.setDescription(description);
    }

    public AssignmentType getAssignment() {
        return newAssignment;
    }
}
