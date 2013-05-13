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
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class AssignmentEditorDto extends SelectableBean implements Comparable<AssignmentEditorDto> {

    public static final String F_TYPE = "type";
    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_ACTIVATION = "activation";
    public static final String F_RELATION = "relation";

    private String name;
    private AssignmentEditorDtoType type;
    private UserDtoStatus status;
    private AssignmentType oldAssignment;

    private boolean showEmpty = false;
    private boolean minimized = true;

    private AssignmentType newAssignment;
    private List<ACAttributeDto> attributes;

    public AssignmentEditorDto(ObjectType targetObject, AssignmentEditorDtoType type, UserDtoStatus status, AssignmentType assignment) {
        Validate.notNull(status, "User dto status must not be null.");
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(assignment, "Assignment must not be null.");

        this.type = type;
        this.status = status;
        this.oldAssignment = assignment;

        PrismContainerValue value = oldAssignment.asPrismContainerValue();

        //todo improve assignment clone, this doesn't look good
        PrismContainerValue newValue = value.clone();
        newAssignment = new AssignmentType();
        newAssignment.setupContainerValue(newValue);
        if (AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION.equals(type)
                && oldAssignment.getAccountConstruction() != null) {
            ConstructionType construction = oldAssignment.getAccountConstruction();
            newAssignment.setAccountConstruction(construction.clone());
        }

        this.name = getNameForTargetObject(targetObject);
    }

    private String getNameForTargetObject(ObjectType object) {
        if (object == null) {
            return null;
        }

        String name = WebMiscUtil.getName(object);

        PolyStringType display = object instanceof OrgType ? ((OrgType)object).getDisplayName() : null;
        String displayName = WebMiscUtil.getOrigStringFromPoly(display);

        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(name)) {
            builder.append(name);
        }

        if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(displayName)) {
            builder.append(", ");
        }

        if (StringUtils.isNotEmpty(displayName)) {
            builder.append(displayName);
        }

        if (StringUtils.isNotEmpty(getRelation())) {
            builder.append(" (").append(getRelation()).append(')');
        }

        return builder.toString();
    }

    public List<ACAttributeDto> getAttributes() {
        if (attributes == null) {
            attributes = new ArrayList<ACAttributeDto>();
        }
        return attributes;
    }

    public void setAttributes(List<ACAttributeDto> attributes) {
        this.attributes = attributes;
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
            type.setAdministrativeStatus(ActivationStatusType.ENABLED);
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

    public boolean isModified() throws SchemaException {
        return !getOldValue().equivalent(getNewValue());
    }

    public PrismContainerValue getOldValue() {
        return oldAssignment.asPrismContainerValue();
    }

    public PrismContainerValue getNewValue() throws SchemaException {
        //this removes activation element if it's empty
        ActivationType activation = newAssignment.getActivation();
        if (activation == null || activation.asPrismContainerValue().isEmpty()) {
            newAssignment.setActivation(null);
        }

        ConstructionType construction = newAssignment.getAccountConstruction();
        if (construction == null) {
            return newAssignment.asPrismContainerValue();
        }

        construction.getAttribute().clear();

        for (ACAttributeDto attribute : getAttributes()) {
            if (attribute.isEmpty()) {
                continue;
            }

            construction.getAttribute().add(attribute.getConstruction());
        }

        return newAssignment.asPrismContainerValue();
    }

    public String getDescription() {
        return newAssignment.getDescription();
    }

    public String getRelation() {
        ObjectReferenceType ref = newAssignment.getTargetRef();
        if (ref == null || ref.getRelation() == null) {
            return null;
        }

        return ref.getRelation().getLocalPart();
    }

    public void setDescription(String description) {
        newAssignment.setDescription(description);
    }

    @Override
    public int compareTo(AssignmentEditorDto other) {
        Validate.notNull(other, "Can't compare assignment editor dto with null.");

        int value = getIndexOfType(getType()) - getIndexOfType(other.getType());
        if (value != 0) {
            return value;
        }

        String name1 = getName() != null ? getName() : "";
        String name2 = other.getName() != null ? other.getName() : "";

        return String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
    }

    private int getIndexOfType(AssignmentEditorDtoType type) {
        if (type == null) {
            return 0;
        }

        AssignmentEditorDtoType[] values = AssignmentEditorDtoType.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(type)) {
                return i;
            }
        }

        return 0;
    }
}
