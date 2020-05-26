/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common;

import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Component that can efficiently determine archetypes for objects.
 * It is backed by caches, therefore this is supposed to be a low-overhead service that can be
 * used in many places.
 *
 * @author Radovan Semancik
 */
@Component
public class ArchetypeManager {

    private static final Trace LOGGER = TraceManager.getTrace(ArchetypeManager.class);

    @Autowired private SystemObjectCache systemObjectCache;

    public PrismObject<ArchetypeType> getArchetype(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        // TODO: make this efficient (use cache)
        return systemObjectCache.getArchetype(oid, result);
    }

    public <O extends AssignmentHolderType> ObjectReferenceType determineArchetypeRef(PrismObject<O> assignmentHolder, OperationResult result) throws SchemaException, ConfigurationException {
        if (assignmentHolder == null) {
            return null;
        }
        if (!assignmentHolder.canRepresent(AssignmentHolderType.class)) {
            return null;
        }

        List<ObjectReferenceType> archetypeAssignmentsRef = determineArchetypesFromAssignments(assignmentHolder.asObjectable());

        if (CollectionUtils.isNotEmpty(archetypeAssignmentsRef)) {
            if (archetypeAssignmentsRef.size() > 1) {
                throw new SchemaException("Only a single archetype for an object is supported: "+assignmentHolder);
            }
        }

        List<ObjectReferenceType> archetypeRefs = assignmentHolder.asObjectable().getArchetypeRef();
        if (CollectionUtils.isEmpty(archetypeRefs)) {
            if (CollectionUtils.isEmpty(archetypeAssignmentsRef)) {
                return null;
            }
            return archetypeAssignmentsRef.get(0);
        }
        if (archetypeRefs.size() > 1) {
            throw new SchemaException("Only a single archetype for an object is supported: "+assignmentHolder);
        }

        //check also assignments

        return archetypeRefs.get(0);
    }

    private <O extends AssignmentHolderType> List<ObjectReferenceType> determineArchetypesFromAssignments(O assignmentHolder) {
        List<AssignmentType> assignments = assignmentHolder.getAssignment();
        return assignments.stream()
                .filter(a -> {
                    ObjectReferenceType target = a.getTargetRef();
                    if (target == null) {
                        return false;
                    }
                    return QNameUtil.match(ArchetypeType.COMPLEX_TYPE, target.getType());
                })
                .map(archetypeAssignment -> archetypeAssignment.getTargetRef())
                .collect(Collectors.toList());
    }

    public <O extends AssignmentHolderType> PrismObject<ArchetypeType> determineArchetype(PrismObject<O> assignmentHolder, OperationResult result) throws SchemaException, ConfigurationException {
        return determineArchetype(assignmentHolder, null, result);
    }

    public <O extends AssignmentHolderType> PrismObject<ArchetypeType> determineArchetype(PrismObject<O> assignmentHolder, String explicitArchetypeOid, OperationResult result) throws SchemaException, ConfigurationException {
        String archetypeOid;
        if (explicitArchetypeOid != null) {
            archetypeOid = explicitArchetypeOid;
        } else {
            ObjectReferenceType archetypeRef = determineArchetypeRef(assignmentHolder, result);
            if (archetypeRef == null) {
                return null;
            }
            archetypeOid = archetypeRef.getOid();
        }

        PrismObject<ArchetypeType> archetype;
        try {
            archetype = systemObjectCache.getArchetype(archetypeOid, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.warn("Archetype {} for object {} cannot be found", archetypeOid, assignmentHolder);
            return null;
        }
        return archetype;
    }

    public <O extends ObjectType> ArchetypePolicyType determineArchetypePolicy(PrismObject<O> object, OperationResult result) throws SchemaException, ConfigurationException {
        return determineArchetypePolicy(object, null, result);
    }

    public <O extends ObjectType> ArchetypePolicyType determineArchetypePolicy(PrismObject<O> object, String explicitArchetypeOid, OperationResult result) throws SchemaException, ConfigurationException {
        if (object == null) {
            return null;
        }
        ArchetypePolicyType archetypePolicy = null;
        if (object.canRepresent(AssignmentHolderType.class)) {
            PrismObject<ArchetypeType> archetype = determineArchetype((PrismObject<? extends AssignmentHolderType>) object, explicitArchetypeOid, result);
            if (archetype != null) {
                archetypePolicy = archetype.asObjectable().getArchetypePolicy();
            }
        }
        // No archetype for this object. Try to find appropriate system configuration section for this object.
        ObjectPolicyConfigurationType objectPolicy = determineObjectPolicyConfiguration(object, result);
        // TODO: cache the result of the merge
        return merge(archetypePolicy, objectPolicy);
    }

    private ArchetypePolicyType merge(ArchetypePolicyType archetypePolicy, ObjectPolicyConfigurationType objectPolicy) {
        if (archetypePolicy == null && objectPolicy == null) {
            return null;
        }
        if (archetypePolicy == null) {
            return objectPolicy.clone();
        }
        if (objectPolicy == null) {
            return archetypePolicy.clone();
        }
        ArchetypePolicyType resultPolicy = archetypePolicy.clone();
        if (archetypePolicy.getApplicablePolicies() == null && objectPolicy.getApplicablePolicies() != null) {
            resultPolicy.setApplicablePolicies(objectPolicy.getApplicablePolicies().clone());
        }
        if (archetypePolicy.getConflictResolution() == null && objectPolicy.getConflictResolution() != null) {
            resultPolicy.setConflictResolution(objectPolicy.getConflictResolution().clone());
        }
        if (archetypePolicy.getDisplay() == null && objectPolicy.getDisplay() != null) {
            resultPolicy.setDisplay(objectPolicy.getDisplay().clone());
        }
        if (archetypePolicy.getExpressionProfile() == null && objectPolicy.getExpressionProfile() != null) {
            resultPolicy.setExpressionProfile(objectPolicy.getExpressionProfile());
        }
        if (archetypePolicy.getLifecycleStateModel() == null && objectPolicy.getLifecycleStateModel() != null) {
            resultPolicy.setLifecycleStateModel(objectPolicy.getLifecycleStateModel().clone());
        }
        if (archetypePolicy.getObjectTemplateRef() == null && objectPolicy.getObjectTemplateRef() != null) {
            resultPolicy.setObjectTemplateRef(objectPolicy.getObjectTemplateRef().clone());
        }
        if (archetypePolicy.getItemConstraint().isEmpty()) {
            for (ItemConstraintType objItemConstraint : objectPolicy.getItemConstraint()) {
                resultPolicy.getItemConstraint().add(objItemConstraint.clone());
            }
        }
        // Deprecated
        if (archetypePolicy.getPropertyConstraint().isEmpty()) {
            for (ItemConstraintType objPropertyConstraint : objectPolicy.getPropertyConstraint()) {
                resultPolicy.getPropertyConstraint().add(objPropertyConstraint.clone());
            }
        }
        return resultPolicy;
    }

    public <O extends ObjectType> ObjectPolicyConfigurationType determineObjectPolicyConfiguration(PrismObject<O> object, OperationResult result) throws SchemaException, ConfigurationException {
        if (object == null) {
            return null;
        }
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        if (systemConfiguration == null) {
            return null;
        }
        return determineObjectPolicyConfiguration(object, systemConfiguration.asObjectable());
    }

    public <O extends ObjectType> ExpressionProfile determineExpressionProfile(PrismObject<O> object, OperationResult result) throws SchemaException, ConfigurationException {
        ArchetypePolicyType archetypePolicy = determineArchetypePolicy(object, result);
        if (archetypePolicy == null) {
            return null;
        }
        String expressionProfileId = archetypePolicy.getExpressionProfile();
        return systemObjectCache.getExpressionProfile(expressionProfileId, result);
    }

    /**
     * This has to remain static due to use from LensContext. Hopefully it will get refactored later.
     */
    public static <O extends ObjectType> ObjectPolicyConfigurationType determineObjectPolicyConfiguration(PrismObject<O> object, SystemConfigurationType systemConfigurationType) throws ConfigurationException {
        List<String> subTypes = FocusTypeUtil.determineSubTypes(object);
        return determineObjectPolicyConfiguration(object.getCompileTimeClass(), subTypes, systemConfigurationType);
    }

    public static <O extends ObjectType> ObjectPolicyConfigurationType determineObjectPolicyConfiguration(Class<O> objectClass, List<String> objectSubtypes, SystemConfigurationType systemConfigurationType) throws ConfigurationException {
        ObjectPolicyConfigurationType applicablePolicyConfigurationType = null;
        for (ObjectPolicyConfigurationType aPolicyConfigurationType: systemConfigurationType.getDefaultObjectPolicyConfiguration()) {
            QName typeQName = aPolicyConfigurationType.getType();
            if (typeQName == null) {
                continue;       // TODO implement correctly (using 'applicable policies' perhaps)
            }
            ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(typeQName);
            if (objectType == null) {
                throw new ConfigurationException("Unknown type "+typeQName+" in default object policy definition in system configuration");
            }
            if (objectType.getClassDefinition() == objectClass) {
                String aSubType = aPolicyConfigurationType.getSubtype();
                if (aSubType == null) {
                    if (applicablePolicyConfigurationType == null) {
                        applicablePolicyConfigurationType = aPolicyConfigurationType;
                    }
                } else if (objectSubtypes != null && objectSubtypes.contains(aSubType)) {
                    applicablePolicyConfigurationType = aPolicyConfigurationType;
                }
            }
        }
        if (applicablePolicyConfigurationType != null) {
            return applicablePolicyConfigurationType;
        }

        return null;
    }

    // TODO take object's archetype into account
    public static <O extends ObjectType> LifecycleStateModelType determineLifecycleModel(PrismObject<O> object, PrismObject<SystemConfigurationType> systemConfiguration) throws ConfigurationException {
        if (systemConfiguration == null) {
            return null;
        }
        return determineLifecycleModel(object, systemConfiguration.asObjectable());
    }

    public static <O extends ObjectType> LifecycleStateModelType determineLifecycleModel(PrismObject<O> object, SystemConfigurationType systemConfigurationType) throws ConfigurationException {
        ObjectPolicyConfigurationType objectPolicyConfiguration = determineObjectPolicyConfiguration(object, systemConfigurationType);
        if (objectPolicyConfiguration == null) {
            return null;
        }
        return objectPolicyConfiguration.getLifecycleStateModel();
    }
}
