/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.bean;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author lazyman
 */
public class AssignmentBean extends SelectableBean implements Serializable {

    private static final long serialVersionUID = 1018852396117899734L;
    private int id;
    private boolean editing;
    private AssignmentBeanType type;
    private boolean enabled = true;
    private boolean useActivationDate = false;
    private AssignmentType assignment;
    @Deprecated
    private ModelService model;


    public int getId() {
        return id;
    }

    public AssignmentBean(int id, AssignmentType assignment, ModelService model) {
        Validate.notNull(assignment, "Assignment must not be null.");
        this.id = id;
        this.assignment = assignment;
        this.model = model;
        type = getType(assignment);
    }


    private AssignmentBeanType getType(AssignmentType assignment) {
        AssignmentBeanType type = AssignmentBeanType.TARGET_REF;
        if (assignment.getAccountConstruction() != null) {
            type = AssignmentBeanType.ACCOUNT_CONSTRUCTION;
        } else if (assignment.getTarget() != null) {
            type = AssignmentBeanType.TARGET;
        }

        return type;
    }

    public boolean isEditing() {
        return editing;
    }

    public void setEditing(boolean editing) {
        this.editing = editing;
    }

    public AssignmentBeanType getType() {
        if (type == null) {
            type = AssignmentBeanType.TARGET_REF;
        }
        return type;
    }

    public String getTypeLocalizedString() {
        return FacesUtils.translateKey(getType().getLocalizationKey());
    }

    public String getTypeString() {
        return getType().name();
    }

    public void setTypeString(String typeString) {
        if (StringUtils.isEmpty(typeString)) {
            type = null;
        }
        type = AssignmentBeanType.valueOf(typeString);
    }

    public boolean isUseActivationDate() {
        return useActivationDate;
    }

    public void setUseActivationDate(boolean useActivationDate) {
        this.useActivationDate = useActivationDate;
    }

    public Date getFromActivation() {
        if (assignment.getActivation() == null) {
            return new Date();
        }

        ActivationType activation = assignment.getActivation();
        if (activation.getValidFrom() == null) {
            return new Date();
        }

        return ControllerUtil.parseCalendarToDate(activation.getValidFrom());
    }

    public String getFromActivationString() {
        ActivationType activation = assignment.getActivation();
        if (activation != null && activation.getValidFrom() != null) {
            return formatDate(ControllerUtil.parseCalendarToDate(activation.getValidFrom()));
        }

        return null;
    }

    private String formatDate(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("MMM/dd/yyyy hh:mm a");
        return dateFormat.format(date);
    }

    public String getToActivationString() {
        ActivationType activation = assignment.getActivation();
        if (activation != null && activation.getValidTo() != null) {
            return formatDate(ControllerUtil.parseCalendarToDate(activation.getValidTo()));
        }

        return null;
    }

    public void setFromActivation(Date date) {
        if (!useActivationDate || date == null) {
            return;
        }

        ActivationType activation = getActivation();
        activation.setValidFrom(ControllerUtil.parseDateToCalendar(date));
    }

    public void setToActivation(Date date) {
        if (!useActivationDate || date == null) {
            return;
        }

        ActivationType activation = getActivation();
        activation.setValidTo(ControllerUtil.parseDateToCalendar(date));
    }

    private ActivationType getActivation() {
        ActivationType activation = assignment.getActivation();
        if (activation == null) {
            activation = new ActivationType();
            assignment.setActivation(activation);
        }
        return activation;
    }

    public String getObjectTypeString() {
        StringBuilder builder = new StringBuilder();
        switch (getType()) {
            case ACCOUNT_CONSTRUCTION:
                AccountConstructionType construction = assignment.getAccountConstruction();
                if (construction != null) {
                    builder.append("type: ");
                    builder.append(construction.getType());
                }
                break;
            case TARGET:
                ObjectType object = assignment.getTarget();
                if (object != null) {
                    builder.append(object.getClass().getSimpleName());
//					builder.append(", oid: ");
//					builder.append(object.getOid());
                }
                break;
            case TARGET_REF:
                ObjectReferenceType objectRef = assignment.getTargetRef();
                if (objectRef != null) {
                    builder.append(objectRef.getType().getLocalPart());
//					builder.append(", oid: ");
//					builder.append(objectRef.getOid());
                }
                break;
        }

        if (builder.length() == 0) {
            builder.append("Undefined");
        }

        return builder.toString();
    }

    public String getObjectString() throws ObjectNotFoundException, SchemaException {
        StringBuilder builder = new StringBuilder();
        switch (getType()) {
            case ACCOUNT_CONSTRUCTION:
                AccountConstructionType construction = assignment.getAccountConstruction();
                if (construction != null) {
                    builder.append("type: ");
                    builder.append(construction.getType());
                }
                break;
            case TARGET:
                ObjectType object = assignment.getTarget();
                if (object != null) {
                    builder.append(object.getName());
                    builder.append(", oid: ");
                    builder.append(object.getOid());
                }
                break;
            case TARGET_REF:
                ObjectReferenceType objectRef = assignment.getTargetRef();
                if (objectRef != null) {
                    ObjectType objType = model.getObject(ObjectTypes.getObjectTypeClass(objectRef.getType().getLocalPart()), objectRef.getOid(), new PropertyReferenceListType(), new OperationResult("Get object"));
                    builder.append(objType.getName());
                    builder.append(", oid: ");
                    builder.append(objectRef.getOid());
                }
                break;
        }

        if (builder.length() == 0) {
            builder.append("Undefined");
        }

        return builder.toString();
    }

    public String getDescription() {
        StringBuilder builder = new StringBuilder();
        switch (getType()) {
            case TARGET:
                ObjectType object = assignment.getTarget();
                if (object != null) {
                    builder.append(object.getDescription());
                }
                break;
            case TARGET_REF:
                ObjectReferenceType objectRef = assignment.getTargetRef();
                if (objectRef != null) {
                    builder.append(objectRef.getDescription());
                }
                break;
        }
        if (builder.length() == 0) {
            builder.append("Undefined");
        }

        return builder.toString();
    }

    public Date getToActivation() {
        if (assignment.getActivation() == null) {
            return new Date();
        }

        ActivationType activation = assignment.getActivation();
        if (activation.getValidTo() == null) {
            return new Date();
        }

        return ControllerUtil.parseCalendarToDate(activation.getValidTo());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AssignmentType getAssignment() {
        normalize();

        return assignment;
    }

    public void setTargetRef(ObjectReferenceType objectRef) {
        Validate.notNull(objectRef, "Object reference must not be null.");
        assignment.setTargetRef(objectRef);
    }

    public void setTarget(ObjectType object) {
        Validate.notNull(object, "Object must not be null.");
        assignment.setTarget(object);
    }

    public void setAccountConstruction(AccountConstructionType construction) {
        Validate.notNull(construction, "Account construction must not be null.");
        assignment.setAccountConstruction(construction);
    }

    public AccountConstructionType getAccountConstruction() {
        return assignment.getAccountConstruction();
    }

    private void normalize() {
        if (!useActivationDate && isEnabled()) {
            assignment.setActivation(null);
        } else {
            ActivationType activation = getActivation();
            if (!useActivationDate) {
                activation.setValidFrom(null);
                activation.setValidTo(null);
            }
            activation.setEnabled(isEnabled());
        }

        switch (getType()) {
            case ACCOUNT_CONSTRUCTION:
                assignment.setTarget(null);
                assignment.setTargetRef(null);
                break;
            case TARGET:
                assignment.setAccountConstruction(null);
                assignment.setTargetRef(null);
                break;
            case TARGET_REF:
                assignment.setAccountConstruction(null);
                assignment.setTarget(null);
        }
    }
}
