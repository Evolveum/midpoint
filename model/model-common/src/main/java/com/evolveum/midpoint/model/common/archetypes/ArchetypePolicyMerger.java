/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.archetypes;

import com.evolveum.midpoint.model.api.AdminGuiConfigurationMergeManager;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Merges {@link ArchetypePolicyType} objects together; and also with {@link ObjectPolicyConfigurationType} ones.
 */
@Component
class ArchetypePolicyMerger {

    @Autowired private AdminGuiConfigurationMergeManager adminGuiConfigurationMergeManager;

    ArchetypePolicyType mergeArchetypePolicies(ArchetypePolicyType superPolicy, ArchetypePolicyType currentPolicy) {
        if (currentPolicy == null) {
            if (superPolicy == null) {
                return null;
            }
            // FIXME: probably no need to clone, caller pass this to function, which also invokes clone
            return superPolicy.clone();
        }

        if (superPolicy == null) {
            // FIXME: probably no need to clone, caller pass this to function, which also invokes clone
            return currentPolicy.clone();
        }

        ArchetypePolicyType mergedPolicy = currentPolicy.clone();

        ArchetypeAdminGuiConfigurationType mergedAdminGuiConfig = mergeAdminGuiConfig(currentPolicy, superPolicy);
        mergedPolicy.setAdminGuiConfiguration(mergedAdminGuiConfig);

        ApplicablePoliciesType mergedApplicablePolicies = mergeApplicablePolicies(currentPolicy, superPolicy);
        mergedPolicy.setApplicablePolicies(mergedApplicablePolicies);

        AssignmentRelationApproachType mergedRelationApproach = mergeRelationApproach(currentPolicy, superPolicy);
        mergedPolicy.setAssignmentHolderRelationApproach(mergedRelationApproach);

        ConflictResolutionType mergedConflictResolutionType = mergeConflictResolution(currentPolicy, superPolicy);
        mergedPolicy.setConflictResolution(mergedConflictResolutionType);

        DisplayType mergedDisplayType = adminGuiConfigurationMergeManager.mergeDisplayType(currentPolicy.getDisplay(), superPolicy.getDisplay());
        mergedPolicy.setDisplay(mergedDisplayType);


        if (currentPolicy.getExpressionProfile() == null) {
            mergedPolicy.setExpressionProfile(superPolicy.getExpressionProfile());
        }

        List<ItemConstraintType> itemConstraints = mergeItemConstraints(currentPolicy.getItemConstraint(), superPolicy.getItemConstraint());
        mergedPolicy.getItemConstraint().clear();
        mergedPolicy.getItemConstraint().addAll(itemConstraints);

        LifecycleStateModelType mergedLifecycleStateModel = mergeLifecycleStateModel(currentPolicy.getLifecycleStateModel(), superPolicy.getLifecycleStateModel());
        mergedPolicy.setLifecycleStateModel(mergedLifecycleStateModel);


        //Experimental
        if (currentPolicy.getLinks() == null) {
            mergedPolicy.setLinks(superPolicy.getLinks());
        }

        if (currentPolicy.getObjectTemplateRef() == null) {
            mergedPolicy.setObjectTemplateRef(superPolicy.getObjectTemplateRef());
        }

        return mergedPolicy;
    }

    private ArchetypeAdminGuiConfigurationType mergeAdminGuiConfig(ArchetypePolicyType currentPolicy, ArchetypePolicyType superPolicy) {
        ArchetypeAdminGuiConfigurationType currentAdminGuiConfig = currentPolicy.getAdminGuiConfiguration();
        ArchetypeAdminGuiConfigurationType superAdminGuiConfig = superPolicy.getAdminGuiConfiguration();
        if (currentAdminGuiConfig == null) {
            return superAdminGuiConfig;
        }

        if (superAdminGuiConfig == null) {
            return currentAdminGuiConfig;
        }

        ArchetypeAdminGuiConfigurationType mergedAdminGuiConfig = new ArchetypeAdminGuiConfigurationType();
        GuiObjectDetailsPageType mergedObjectDetails = mergeObjectDetails(currentAdminGuiConfig, superAdminGuiConfig);
        mergedAdminGuiConfig.setObjectDetails(mergedObjectDetails);

        return mergedAdminGuiConfig;
    }

    private GuiObjectDetailsPageType mergeObjectDetails(ArchetypeAdminGuiConfigurationType currentAdminGuiConfig, ArchetypeAdminGuiConfigurationType superAdminGuiConfig) {
        GuiObjectDetailsPageType currentObjectDetails = currentAdminGuiConfig.getObjectDetails();
        GuiObjectDetailsPageType superObjectDetails = superAdminGuiConfig.getObjectDetails();
        if (currentObjectDetails == null) {
            if (superObjectDetails == null) {
                return null;
            }
            return superObjectDetails.clone();
        }

        if (superObjectDetails == null) {
            return currentObjectDetails.clone();
        }

        return adminGuiConfigurationMergeManager.mergeObjectDetailsPageConfiguration(superObjectDetails, currentObjectDetails);
        //TODO save method, objectForm, relations
    }

    private ApplicablePoliciesType mergeApplicablePolicies(ArchetypePolicyType currentPolicy, ArchetypePolicyType superPolicy) {
        ApplicablePoliciesType currentApplicablePolicies = currentPolicy.getApplicablePolicies();
        ApplicablePoliciesType superApplicablePolicies = superPolicy.getApplicablePolicies();
        if (currentApplicablePolicies == null) {
            return superApplicablePolicies;
        }

        ApplicablePoliciesType mergedPolicies = currentApplicablePolicies.clone();
        if (superApplicablePolicies == null) {
            return mergedPolicies;
        }
        for (ObjectReferenceType policyGroupRef : superApplicablePolicies.getPolicyGroupRef()) {
            mergedPolicies.getPolicyGroupRef().add(policyGroupRef.clone());
        }

        return mergedPolicies;
    }

    private AssignmentRelationApproachType mergeRelationApproach(ArchetypePolicyType currentPolicy, ArchetypePolicyType superPolicy) {
        if (currentPolicy.getAssignmentHolderRelationApproach() != null) {
            return currentPolicy.getAssignmentHolderRelationApproach();
        }

        return superPolicy.getAssignmentHolderRelationApproach();

    }

    private ConflictResolutionType mergeConflictResolution(ArchetypePolicyType currentPolicy, ArchetypePolicyType superPolicy) {
        ConflictResolutionType currentConflictResolution = currentPolicy.getConflictResolution();
        ConflictResolutionType superConflictResolution = superPolicy.getConflictResolution();
        if (currentConflictResolution == null) {
            if (superConflictResolution == null) {
                return null;
            }
            return superConflictResolution.clone();
        }

        if (superConflictResolution == null) {
            return currentConflictResolution.clone();
        }

        ConflictResolutionType mergedConflictResolution = currentConflictResolution.clone();
        if (currentConflictResolution.getAction() == null) {
            mergedConflictResolution.setAction(superConflictResolution.getAction());
        }
        if (currentConflictResolution.getDelayUnit() == null) {
            mergedConflictResolution.setDelayUnit(superConflictResolution.getDelayUnit());
        }

        if (currentConflictResolution.getMaxAttempts() == null) {
            mergedConflictResolution.setMaxAttempts(superConflictResolution.getMaxAttempts());
        }

        return mergedConflictResolution;
    }

    private List<ItemConstraintType> mergeItemConstraints(List<ItemConstraintType> currentConstraints, List<ItemConstraintType> superConstraints) {
        return adminGuiConfigurationMergeManager.mergeContainers(currentConstraints, superConstraints,
                this::createItemConstraintPredicate,
                this::mergeItemConstraint);
    }

    private Predicate<ItemConstraintType> createItemConstraintPredicate(ItemConstraintType constraint) {
        return c -> pathsMatch(constraint.getPath(), c.getPath());
    }

    // we want to merge according to path, but there might exist more than 1 def without path, so rather do nothing.
    private boolean pathsMatch(ItemPathType supperPath, ItemPathType currentPath) {
        return supperPath != null && currentPath != null && supperPath.equivalent(currentPath);
    }

    private ItemConstraintType mergeItemConstraint(ItemConstraintType matchedConstraint, ItemConstraintType superConstraint) {
        ItemConstraintType mergedConstraint = cloneComplex(matchedConstraint);
        if (matchedConstraint.getVisibility() == null) {
            mergedConstraint.setVisibility(superConstraint.getVisibility());
        }
        return mergedConstraint;
    }

    private LifecycleStateModelType mergeLifecycleStateModel(LifecycleStateModelType currentLifecycleStateModel, LifecycleStateModelType superLifecycleStateModel) {
        if (currentLifecycleStateModel == null) {
            if (superLifecycleStateModel == null) {
                return null;
            }
            return superLifecycleStateModel.clone();
        }

        if (superLifecycleStateModel == null) {
            return currentLifecycleStateModel.clone();
        }

        LifecycleStateModelType mergedLifecycleModel = new LifecycleStateModelType();
        List<LifecycleStateType> mergedLifecycleState = mergeLifecycleState(currentLifecycleStateModel.getState(), superLifecycleStateModel.getState());
        mergedLifecycleModel.getState().addAll(mergedLifecycleState);

        return mergedLifecycleModel;
    }

    private List<LifecycleStateType> mergeLifecycleState(List<LifecycleStateType> currentState, List<LifecycleStateType> superLifecycleStates) {
        return adminGuiConfigurationMergeManager.mergeContainers(currentState, superLifecycleStates,
                this::createLifecycleStatePredicate,
                this::mergeLifecycleState);
    }

    private Predicate<LifecycleStateType> createLifecycleStatePredicate(LifecycleStateType currentState) {
        return s -> s.getName() != null && currentState.getName() != null && s.getName().equals(currentState.getName());
    }

    private LifecycleStateType mergeLifecycleState(LifecycleStateType currentLifecycleState, LifecycleStateType superLifecycleState) {
        LifecycleStateType mergedLifecycleState = new LifecycleStateType();
        if (currentLifecycleState.getName() == null) {
            mergedLifecycleState.setName(superLifecycleState.getName());
        }

        if (currentLifecycleState.getDescription() == null) {
            mergedLifecycleState.setDescription(superLifecycleState.getDescription());
        }

        if (currentLifecycleState.getDisplayName() == null) {
            mergedLifecycleState.setDisplayName(superLifecycleState.getDisplayName());
        }

        List<LifecycleStateActionType> mergedEntryActions = mergeEntryAction(currentLifecycleState.getEntryAction(), superLifecycleState.getEntryAction());
        if (mergedEntryActions != null) {
            mergedLifecycleState.getEntryAction().clear();
            mergedLifecycleState.getEntryAction().addAll(mergedEntryActions);
        }

        List<LifecycleStateActionType> mergedExitActions = mergeEntryAction(currentLifecycleState.getExitAction(), superLifecycleState.getExitAction());
        if (mergedExitActions != null) {
            mergedLifecycleState.getExitAction().clear();
            mergedLifecycleState.getExitAction().addAll(mergedExitActions);
        }

        if (currentLifecycleState.getForcedActivationStatus() == null) {
            mergedLifecycleState.setForcedActivationStatus(superLifecycleState.getForcedActivationStatus());
        }

        if (currentLifecycleState.isActiveAssignments() == null) {
            mergedLifecycleState.setActiveAssignments(superLifecycleState.isActiveAssignments());
        }

        VirtualAssignmentSpecificationType mergedAssignment = mergeForcedAssignment(currentLifecycleState.getForcedAssignment(), superLifecycleState.getForcedAssignment());
        mergedLifecycleState.setForcedAssignment(mergedAssignment);

        return mergedLifecycleState;
    }

    private VirtualAssignmentSpecificationType mergeForcedAssignment(VirtualAssignmentSpecificationType currentForcedAssignment, VirtualAssignmentSpecificationType superForcedAssignment) {
        if (currentForcedAssignment == null) {
            if (superForcedAssignment == null) {
                return null;
            }
            return superForcedAssignment.clone();
        }

        if (superForcedAssignment == null) {
            return currentForcedAssignment.clone();
        }

        VirtualAssignmentSpecificationType mergedAssignment = currentForcedAssignment.clone();
        if (currentForcedAssignment.getFilter() == null) {
            mergedAssignment.setFilter(superForcedAssignment.getFilter().clone());
        }

        if (currentForcedAssignment.getTargetType() == null) {
            mergedAssignment.setTargetType(superForcedAssignment.getTargetType());
        }

        return mergedAssignment;
    }

    private List<LifecycleStateActionType> mergeEntryAction(List<LifecycleStateActionType> currentActions, List<LifecycleStateActionType> superActions) {
        return adminGuiConfigurationMergeManager.mergeContainers(currentActions, superActions,
                this::createLifecycleStateActionPredicate,
                this::mergeAction);
    }

    private Predicate<LifecycleStateActionType> createLifecycleStateActionPredicate(LifecycleStateActionType action) {
        return a -> a.getName() != null && a.getName().equals(action.getName());
    }

    private LifecycleStateActionType mergeAction(LifecycleStateActionType currentAction, LifecycleStateActionType superAction) {
        LifecycleStateActionType mergedAction = currentAction.clone();

        LifecycleStateActionDataReductionType currentDataReduction = currentAction.getDataReduction();
        LifecycleStateActionDataReductionType superDataReduction = superAction.getDataReduction();
        if (currentDataReduction == null) {
            if (superDataReduction == null) {
                return mergedAction;
            }
            mergedAction.setDataReduction(superDataReduction.clone());
            return mergedAction;
        }

        if (superDataReduction == null) {
            return mergedAction;
        }

        LifecycleStateActionDataReductionType mergedDataReduction = mergeDataReduction(currentDataReduction, superDataReduction);
        if (mergedDataReduction != null) {
            mergedAction.setDataReduction(mergedDataReduction);
        }
        return mergedAction;
    }

    private LifecycleStateActionDataReductionType mergeDataReduction(LifecycleStateActionDataReductionType currentDataReduction, LifecycleStateActionDataReductionType superDataReduction) {
        List<ItemPathType> currentItems = currentDataReduction.getPurgeItem();
        List<ItemPathType> superItems = superDataReduction.getPurgeItem();

        LifecycleStateActionDataReductionType mergedDataReduction = currentDataReduction.clone();
        if (currentItems.isEmpty()) {
            if (superItems.isEmpty()) {
                return null;
            }
            superItems.forEach(i -> mergedDataReduction.getPurgeItem().add(i.clone()));
            return mergedDataReduction;
        }

        for (ItemPathType superItem : superItems) {
            if (!currentItems.contains(superItem)) {
                mergedDataReduction.getPurgeItem().add(superItem.clone());
            }
        }

        return mergedDataReduction;
    }

    ArchetypePolicyType merge(ArchetypePolicyType archetypePolicy, ObjectPolicyConfigurationType objectPolicy) {
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
        return resultPolicy;
    }

    private <C extends Containerable> C cloneComplex(C containerable) {
        return containerable.cloneWithoutId();
    }
}
