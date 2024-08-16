/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.enforcer.api.ItemSecurityConstraints;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AssignmentValueWrapperImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil.AssignmentTypeType;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class AssignmentWrapperFactoryImpl extends NoEmptyValueContainerWrapperFactoryImpl<AssignmentType> {

    @Override
    public PrismContainerValueWrapper<AssignmentType> createContainerValueWrapper(
            PrismContainerWrapper<AssignmentType> objectWrapper, PrismContainerValue<AssignmentType> objectValue,
            ValueStatus status, WrapperContext context) {
        return new AssignmentValueWrapperImpl(objectWrapper, objectValue, status);
    }

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismContainerDefinition && QNameUtil.match(def.getTypeName(), AssignmentType.COMPLEX_TYPE);
    }

    @Override
    protected List<? extends ItemDefinition> getItemDefinitions(
            PrismContainerWrapper<AssignmentType> parent, PrismContainerValue<AssignmentType> value) {
        AssignmentType assignmentType = value.getRealValue();
        AssignmentTypeType assignmentTypeType = AssignmentsUtil.getAssignmentType(assignmentType);
        List<? extends ItemDefinition> definitions = parent.getDefinitions();
        List<ItemDefinition<?>> filteredDefinitions = new ArrayList<>();
        for (ItemDefinition<?> definition : definitions) {
            if (isNotDefinedForAssignmentType(assignmentTypeType, definition, assignmentType)) {
                continue;
            }
            filteredDefinitions.add(definition);
        }
        return filteredDefinitions;
    }

    //CONSTRUCTION, ABSTRACT_ROLE, POLICY_RULE, FOCUS_MAPPING, PERSONA_CONSTRUCTION, ASSIGNMENT_RELATION;
    private boolean isNotDefinedForAssignmentType(
            AssignmentTypeType assignmentTypeType, ItemDefinition<?> def, AssignmentType assignmentType) {
        if (def instanceof PrismContainerDefinition) {
            if (QNameUtil.match(ConstructionType.COMPLEX_TYPE, def.getTypeName())
                    && AssignmentTypeType.CONSTRUCTION != assignmentTypeType) {
                return true;
            }
            if (QNameUtil.match(PolicyRuleType.COMPLEX_TYPE, def.getTypeName())
                    && AssignmentTypeType.POLICY_RULE != assignmentTypeType) {
                return true;
            }
            if (QNameUtil.match(MappingsType.COMPLEX_TYPE, def.getTypeName())
                    && AssignmentTypeType.FOCUS_MAPPING != assignmentTypeType) {
                return true;
            }
            if (QNameUtil.match(PersonaConstructionType.COMPLEX_TYPE, def.getTypeName())
                    && AssignmentTypeType.PERSONA_CONSTRUCTION != assignmentTypeType) {
                return true;
            }
            if (QNameUtil.match(AssignmentRelationType.COMPLEX_TYPE, def.getTypeName())
                    && AssignmentTypeType.ASSIGNMENT_RELATION != assignmentTypeType
                    && !isArchetype(assignmentType)) {
                return true;
            }
            return false;
        }
        if (AssignmentTypeType.ABSTRACT_ROLE != assignmentTypeType
                && AssignmentType.F_TARGET_REF.equivalent(def.getItemName())) {
            return true;
        }
        if ((AssignmentTypeType.ABSTRACT_ROLE != assignmentTypeType
                || isOrg(assignmentType)) && AssignmentType.F_TENANT_REF.equivalent(def.getItemName())) {
            return true;
        }
        if ((AssignmentTypeType.ABSTRACT_ROLE != assignmentTypeType
                || isOrg(assignmentType)) && AssignmentType.F_ORG_REF.equivalent(def.getItemName())) {
            return true;
        }
        return false;
    }

    private boolean isArchetype(AssignmentType assignmentType) {
        return isAssignmentWithTarget(assignmentType, ArchetypeType.COMPLEX_TYPE);
    }

    private boolean isOrg(AssignmentType assignmentType) {
        return isAssignmentWithTarget(assignmentType, OrgType.COMPLEX_TYPE);
    }

    private boolean isAssignmentWithTarget(AssignmentType assignmentType, QName targetType) {
        ObjectReferenceType ref = getRefFromAssignment(assignmentType);
        if (ref == null) {
            return false;
        }
        return QNameUtil.match(ref.getType(), targetType);
    }

    private ObjectReferenceType getRefFromAssignment(AssignmentType assignmentType) {
        if (assignmentType == null) {
            return null;
        }
        return assignmentType.getTargetRef();

    }

    @Override
    public int getOrder() {
        return 99;
    }

    @Override
    public boolean skipCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
        ItemSecurityConstraints securityConstraints = context.getSecurityConstraints();
        if (securityConstraints == null) {
            return super.skipCreateWrapper(def, status, context, isEmptyValue);
        }
        if (allowedItemExist(securityConstraints)) {
            return false;
        }
        return super.skipCreateWrapper(def, status, context, isEmptyValue);
    }

    private boolean allowedItemExist(ItemSecurityConstraints securityConstraints) {
        //todo go through all items and check if they are allowed
        //or hack : go only through those items which we need
        AuthorizationDecisionType descr = securityConstraints.findItemDecision(ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION));
        AuthorizationDecisionType tenantRef = securityConstraints.findItemDecision(ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_TENANT_REF));
        AuthorizationDecisionType orgRef = securityConstraints.findItemDecision(ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_ORG_REF));
        AuthorizationDecisionType focusType = securityConstraints.findItemDecision(ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_FOCUS_TYPE));
        return AuthorizationDecisionType.ALLOW.equals(descr) || AuthorizationDecisionType.ALLOW.equals(tenantRef)
                || AuthorizationDecisionType.ALLOW.equals(orgRef) || AuthorizationDecisionType.ALLOW.equals(focusType);

    }
}
