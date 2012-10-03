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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    private boolean showEmpty = false;
    private boolean minimized = true;

    private AssignmentType newAssignment;
    private List<ACAttributeDto> attributes;

    public AssignmentEditorDto(String name, AssignmentEditorDtoType type, UserDtoStatus status, AssignmentType assignment) {
        Validate.notNull(status, "User dto status must not be null.");
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(assignment, "Assignment must not be null.");

        this.name = name;
        this.type = type;
        this.status = status;
        this.oldAssignment = assignment;

        PrismContainerValue value = oldAssignment.asPrismContainerValue();

        //improve assignment clone, this doesn't look good
        PrismContainerValue oldValue = value.clone();
        newAssignment = new AssignmentType();
        newAssignment.setupContainerValue(oldValue);
        if (AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION.equals(type)
                && oldAssignment.getAccountConstruction() != null) {
            AccountConstructionType construction = oldAssignment.getAccountConstruction();
            newAssignment.setAccountConstruction(construction.clone());
        }
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

    public boolean isModified() {
        //todo reimplement
        return false;
//        return oldAssignment != null && !oldAssignment.equals(newAssignment);
    }

    public PrismContainerValue getOldValue() {
        return oldAssignment != null ? oldAssignment.asPrismContainerValue() : null;
    }

    public PrismContainerValue getNewValue() {
        //todo reimplement
        return newAssignment != null ? newAssignment.asPrismContainerValue() : null;
//        PrismContainerValue value = newAssignment.asPrismContainerValue();
//        PrismContainerValue newValue = value.clone();
//        //remove empty/null values, which are placeholders in form
//        List<Item> items = newValue.getItems();
//        Iterator<Item> iterator = items.iterator();
//        while (iterator.hasNext()) {
//            Item item = iterator.next();
//            List<PrismValue> values = item.getValues();
//            if (values != null) {
//                Iterator<PrismValue> valueIterator = values.iterator();
//                while (valueIterator.hasNext()) {
//                    PrismValue prismValue = valueIterator.next();
//                    if (prismValue instanceof PrismPropertyValue) {
//                        PrismPropertyValue propertyValue = (PrismPropertyValue) prismValue;
//                        if (propertyValue.getValue() == null) {
//                            valueIterator.remove();
//                        }
//                    }
//                }
//            }
//
//            if (item.isEmpty()) {
//                iterator.remove();
//            }
//        }
//        return newValue;
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
