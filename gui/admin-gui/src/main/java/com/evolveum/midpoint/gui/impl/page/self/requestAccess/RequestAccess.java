/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.web.page.self.PageAssignmentsList;
import com.evolveum.midpoint.web.page.self.dto.ConflictDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Session;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.SKIP;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RequestAccess implements Serializable {

    private List<ObjectReferenceType> personOfInterest;

    private List<AssignmentType> shoppingCartAssignments;

    public List<ObjectReferenceType> getPersonOfInterest() {
        if (personOfInterest == null) {
            personOfInterest = new ArrayList<>();
        }
        return personOfInterest;
    }

    public void setPersonOfInterest(List<ObjectReferenceType> personOfInterest) {
        this.personOfInterest = personOfInterest;
    }

    public List<AssignmentType> getShoppingCartAssignments() {
        if (shoppingCartAssignments == null) {
            shoppingCartAssignments = new ArrayList<>();
        }
        return shoppingCartAssignments;
    }

    public void setShoppingCartAssignments(List<AssignmentType> shoppingCartAssignments) {
        this.shoppingCartAssignments = shoppingCartAssignments;
    }

    public void computeConflicts() {
//        MidPointApplication mp = MidPointApplication.get();
//
//        SessionStorage storage = MidPointAuthWebSession.get().getSessionStorage();
//
//        ObjectDelta<UserType> delta;
//        OperationResult result = new OperationResult(OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS);
//        Task task = createSimpleTask(OPERATION_PREVIEW_ASSIGNMENT_CONFLICTS);
//        Map<String, ConflictDto> conflictsMap = new HashMap<>();
//        try {
//            PrismObject<UserType> user = getTargetUser();
//            delta = user.createModifyDelta();
//
//            PrismContainerDefinition def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
//            handleAssignmentDeltas(delta, storage.getRoleCatalog().getAssignmentShoppingCart(), def);
//
//            PartialProcessingOptionsType partialProcessing = new PartialProcessingOptionsType();
//            partialProcessing.setInbound(SKIP);
//            partialProcessing.setProjection(SKIP);
//            ModelExecuteOptions recomputeOptions = ModelExecuteOptions
//                    .create(mp.getPrismContext())
//                    .partialProcessing(partialProcessing);
//            ModelContext<UserType> modelContext = mp.getModelInteractionService()
//                    .previewChanges(MiscUtil.createCollection(delta), recomputeOptions, task, result);
//            DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple =
//                    modelContext.getEvaluatedAssignmentTriple();
//            if (evaluatedAssignmentTriple != null) {
//                Collection<? extends EvaluatedAssignment> addedAssignments = evaluatedAssignmentTriple.getPlusSet();
//                for (EvaluatedAssignment<UserType> evaluatedAssignment : addedAssignments) {
//                    for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getAllTargetsPolicyRules()) {
//                        if (!policyRule.containsEnabledAction()) {
//                            continue;
//                        }
//                        // everything other than 'enforce' is a warning
//                        boolean isWarning = !policyRule.containsEnabledAction(EnforcementPolicyActionType.class);
//                        fillInConflictedObjects(evaluatedAssignment, policyRule.getAllTriggers(), isWarning, conflictsMap);
//                    }
//                }
//            } else if (!result.isSuccess() && StringUtils.isNotEmpty(getSubresultWarningMessages(result))) {
//                getFeedbackMessages().warn(PageAssignmentsList.this,
//                        createStringResource("PageAssignmentsList.conflictsWarning").getString() + " " + getSubresultWarningMessages(result));
//                conflictProblemExists = true;
//            }
//        } catch (Exception e) {
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get assignments conflicts. Reason: ", e);
//            error("Couldn't get assignments conflicts. Reason: " + e);
//        }
//        return new ArrayList<>(conflictsMap.values());
    }
}
