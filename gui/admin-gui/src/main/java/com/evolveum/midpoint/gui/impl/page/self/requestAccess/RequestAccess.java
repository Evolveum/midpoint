/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.SKIP;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.result.OpResult;

import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Page;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityDefinitionBuilder;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.RestartResponseException;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RequestAccess implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(RequestAccess.class);

    private static final String DOT_CLASS = RequestAccess.class.getName() + ".";
    private static final String OPERATION_COMPUTE_ALL_CONFLICTS = DOT_CLASS + "computeAllConflicts";
    private static final String OPERATION_COMPUTE_CONFLICT = DOT_CLASS + "computeConflicts";
    private static final String OPERATION_REQUEST_ASSIGNMENTS = DOT_CLASS + "requestAssignments";
    private static final String OPERATION_REQUEST_ASSIGNMENTS_SINGLE = DOT_CLASS + "requestAssignmentsSingle";
    private static final String OPERATION_LOAD_ASSIGNABLE_RELATIONS_LIST = DOT_CLASS + "loadAssignableRelationsList";
    private static final String OPERATION_LOAD_ASSIGNABLE_ROLES = DOT_CLASS + "loadAssignableRoles";

    public static final List<ValidityPredefinedValueType> DEFAULT_VALIDITY_PERIODS = Arrays.asList(
            new ValidityPredefinedValueType()
                    .duration(XmlTypeConverter.createDuration("P1D"))
                    .display(new DisplayType().label("RequestAccess.validity1Day")),
            new ValidityPredefinedValueType()
                    .duration(XmlTypeConverter.createDuration("P7D"))
                    .display(new DisplayType().label("RequestAccess.validity1Week")),
            new ValidityPredefinedValueType()
                    .duration(XmlTypeConverter.createDuration("P1M"))
                    .display(new DisplayType().label("RequestAccess.validity1Month")),
            new ValidityPredefinedValueType()
                    .duration(XmlTypeConverter.createDuration("P1Y"))
                    .display(new DisplayType().label("RequestAccess.validity1Year"))
    );

    public static final String VALIDITY_CUSTOM_LENGTH = "validityCustomLength";

    public static final String DEFAULT_MYSELF_IDENTIFIER = "myself";

    private Boolean poiMyself;

    private String poiGroupSelectionIdentifier;

    /**
     * This map contains real list of assignments that are requested for specific users.
     */
    private final Map<ObjectReferenceType, List<AssignmentType>> requestItems = new HashMap<>();

    /**
     * This map contains existing assignments from users that have to be removed to avoid conflicts when requesting
     */
    private final Map<ObjectReferenceType, List<AssignmentType>> requestItemsExistingToRemove = new HashMap<>();

    /**
     * Map that contains list of current role memberships (before requesting) for specific user.
     */
    private final Map<ObjectReferenceType, List<ObjectReferenceType>> existingPoiRoleMemberships = new HashMap<>();

    /**
     * Assignments that we're added to shopping cart from role catalog.
     * Not assigned to any user yet. Set is assigned as a whole to every new user for which we're requesting.
     *
     * These should only contain targetRef with proper oid and relation, nothing else.
     */
    private final Set<AssignmentType> templateAssignments = new HashSet<>();

    /**
     * Currently selected relation in request access wizard relation step.
     */
    private QName relation;

    private QName defaultRelation;

    /**
     * Used as backing field for combobox model.
     * It can contain different values - string keys (custom length label), predefined values
     */
    private Object selectedValidity;

    private Duration validityDuration;

    private String comment;

    private boolean conflictsDirty;

    private List<Conflict> conflicts = new ArrayList<>();

    public Map<ObjectReferenceType, List<ObjectReferenceType>> getExistingPoiRoleMemberships() {
        return Collections.unmodifiableMap(existingPoiRoleMemberships);
    }

    public Boolean isPoiMyself() {
        return poiMyself;
    }

    public void setPoiMyself(Boolean poiMyself) {
        this.poiMyself = poiMyself;

        if (poiMyself) {
            poiGroupSelectionIdentifier = null;
        }
    }

    public String getPoiGroupSelectionIdentifier() {
        return poiGroupSelectionIdentifier;
    }

    public void setPoiGroupSelectionIdentifier(String poiGroupSelectionIdentifier) {
        this.poiGroupSelectionIdentifier = poiGroupSelectionIdentifier;
        poiMyself = false;
    }

    public Object getSelectedValidity() {
        return selectedValidity;
    }

    public void setSelectedValidity(Object selectedValidity) {
        if (selectedValidity instanceof ValidityPredefinedValueType predefined) {
            setValidityDuration(predefined.getDuration());
        } else {
            setValidityDuration(null);
        }

        this.selectedValidity = selectedValidity;
    }

    public Set<AssignmentType> getTemplateAssignments() {
        return Collections.unmodifiableSet(templateAssignments);
    }

    public List<Conflict> getConflicts() {
        if (conflicts == null) {
            conflicts = new ArrayList<>();
        }
        return conflicts;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<ObjectReferenceType> getPersonOfInterest() {
        return List.copyOf(new ArrayList<>(requestItems.keySet()));
    }

    public void addPersonOfInterest(ObjectReferenceType ref, List<ObjectReferenceType> existingMemberships) {
        addPersonOfInterest(List.of(ref), Map.of(ref, existingMemberships));
    }

    public void addPersonOfInterest(List<ObjectReferenceType> refs, Map<ObjectReferenceType, List<ObjectReferenceType>> existingMemberships) {
        if (refs == null) {
            return;
        }

        Set<String> newOids = refs.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());

        Set<ObjectReferenceType> existing = new HashSet<>(requestItems.keySet());
        Set<String> existingOids = existing.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());

        boolean changed = false;

        // add new persons if they're not in set yet
        for (ObjectReferenceType ref : refs) {
            if (existingOids.contains(ref.getOid())) {
                continue;
            }

            List<AssignmentType> assignments = this.templateAssignments.stream().map(AssignmentType::clone).collect(Collectors.toList());
            requestItems.put(ref, assignments);

            List<ObjectReferenceType> memberships = existingMemberships.get(ref);
            if (memberships == null) {
                memberships = new ArrayList<>();
            }
            existingPoiRoleMemberships.put(ref, memberships);

            changed = true;
        }

        // remove persons that were not selected (or were unselected)
        for (ObjectReferenceType ref : existing) {
            if (newOids.contains(ref.getOid())) {
                continue;
            }

            requestItems.remove(ref);
            requestItemsExistingToRemove.remove(ref);
            existingPoiRoleMemberships.remove(ref);
            existingOids.remove(ref.getOid());

            changed = true;
        }

        if (changed) {
            markConflictsDirty();
        }
    }

    public boolean newPersonOfInterestExists(List<ObjectReferenceType> newPersonOfInterestRefs) {
        return CollectionUtils.isNotEmpty(getNewPersonOfInterestOids(newPersonOfInterestRefs));
    }

    /**
     * Returns the list of new person of interest oids that are not yet in the requestItems.
     * @param newPersonOfInterestRefs
     * @return
     */
    public List<String> getNewPersonOfInterestOids(List<ObjectReferenceType> newPersonOfInterestRefs) {
        Set<String> newOids = newPersonOfInterestRefs.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());
        Set<ObjectReferenceType> existing = new HashSet<>(requestItems.keySet());
        Set<String> existingOids = existing.stream().map(AbstractReferencable::getOid).collect(Collectors.toSet());
        return newOids
                .stream()
                .filter(newOid -> !existingOids.contains(newOid))
                .collect(Collectors.toList());
    }

    /**
     * Matching will be done only based on targetRef (oid, type and relation)
     */
    private boolean matchAssignments(AssignmentType one, AssignmentType two) {
        return matchAssignments(one, two, true);
    }

    private boolean matchAssignments(AssignmentType one, AssignmentType two, boolean checkRelation) {
        if (one == null && two == null) {
            return true;
        }

        if (one == null || two == null) {
            return false;
        }

        ObjectReferenceType oneTarget = one.getTargetRef();
        ObjectReferenceType twoTarget = two.getTargetRef();

        return referencesEqual(oneTarget, twoTarget, checkRelation);
    }

    /**
     * @param assignments list of assignments to search from
     * @param assignment assignment used to find matching one
     * @param checkRelation if true, relation will be ignored during matching
     * @return assignment from list that matches given assignment (based on targetRef)
     */
    private AssignmentType findMatchingAssignment(
            Collection<AssignmentType> assignments, AssignmentType assignment, boolean checkRelation) {

        if (assignments == null || assignment == null) {
            return null;
        }

        return assignments.stream()
                .filter(a -> referencesEqual(a.getTargetRef(), assignment.getTargetRef(), checkRelation))
                .findFirst()
                .orElse(null);
    }

    private AssignmentConstraintsType getDefaultAssignmentConstraints() {
        SystemConfigurationType config = MidPointApplication.get().getSystemConfigurationIfAvailable();
        if (config == null || config.getRoleManagement() == null) {
            return new AssignmentConstraintsType();
        }

        RoleManagementConfigurationType roleManagement = config.getRoleManagement();
        AssignmentConstraintsType constraints = roleManagement.getDefaultAssignmentConstraints();

        return constraints != null ? constraints : new AssignmentConstraintsType();
    }

    /**
     * @param assignments list of assignments containing only targetRef and nothing else (without any activation, extension, etc.)
     */
    public void addAssignments(List<AssignmentType> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return;
        }

        AssignmentConstraintsType constraints = getDefaultAssignmentConstraints();

        // we can't use selectedAssignments.contains(a) here because selectedAssignments can contain items other than targetRef
        // we'll also take assignment constraints into account
        List<AssignmentType> filterNotYetSelected = assignments.stream()
                .filter(a -> canAddTemplateAssignment(a.getTargetRef(), constraints))
                .toList();

        if (filterNotYetSelected.isEmpty()) {
            return;
        }

        // we'll add new assignments to templates (that would be added to all POIs added afterward)
        filterNotYetSelected.forEach(a -> templateAssignments.add(a.clone()));

        for (List<AssignmentType> list : requestItems.values()) {
            filterNotYetSelected.forEach(a -> list.add(a.clone()));
        }

        markConflictsDirty();
    }

    private boolean isAllowSameRelation(AssignmentConstraintsType constraints) {
        return constraints == null || BooleanUtils.isNotFalse(constraints.isAllowSameRelation());
    }

    private boolean isAllowSameTarget(AssignmentConstraintsType constraints) {
        return constraints == null || BooleanUtils.isNotFalse(constraints.isAllowSameTarget());
    }

    /**
     * @param assignments list of assignments that may contain items other than targetRef (activation, extension, etc.)
     */
    public void removeAssignments(List<AssignmentType> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            return;

        }

        for (AssignmentType a : assignments) {
            AssignmentType matching = findMatchingAssignment(templateAssignments, a, true);
            this.templateAssignments.remove(matching);

            for (ObjectReferenceType ref : requestItems.keySet()) {
                List<AssignmentType> assignmentList = requestItems.get(ref);
                assignmentList.removeIf(item -> matchAssignments(item, a));
            }
        }
        markConflictsDirty();
    }

    public List<AssignmentType> getShoppingCartAssignments() {
        Set<AssignmentType> assignments = new HashSet<>();

        requestItems.values().forEach(assignments::addAll);

        return List.copyOf(assignments);
    }

    public List<AssignmentType> getShoppingCartAssignments(ObjectReferenceType personOfInterestRef) {
        return getAssignments(personOfInterestRef, requestItems);
    }

    private List<AssignmentType> getAssignmentsToBeRemoved(ObjectReferenceType personOfInterestRef) {
        return getAssignments(personOfInterestRef, requestItemsExistingToRemove);
    }

    private List<AssignmentType> getAssignments(ObjectReferenceType personOfInterestRef, Map<ObjectReferenceType, List<AssignmentType>> map) {
        if (personOfInterestRef == null) {
            return new ArrayList<>();
        }

        List<AssignmentType> assignments = map.get(personOfInterestRef);
        return assignments != null ? assignments : new ArrayList<>();
    }

    public List<ShoppingCartItem> getShoppingCartItems() {
        Map<AssignmentType, Integer> counts = new HashMap<>();
        Map<AssignmentType, List<PolyStringType>> poiNames = new HashMap<>();

        templateAssignments.forEach(a -> counts.put(a.clone(), 0));

        for (Map.Entry<ObjectReferenceType, List<AssignmentType>> requestItem : requestItems.entrySet()) {
            ObjectReferenceType poi = requestItem.getKey();

            for (AssignmentType real : requestItem.getValue()) {
                AssignmentType counted = findAssignmentByTargetRef(counts.keySet(), real.getTargetRef());
                if (counted != null) {
                    int count = counts.get(counted);
                    counts.replace(counted, count + 1);
                }

                poiNames.computeIfAbsent(counted, k -> new ArrayList<>()).add(poi.getTargetName());
            }
        }

        return counts.entrySet().stream()
                .map(e -> new ShoppingCartItem(e.getKey(), e.getValue(), poiNames.get(e.getKey())))
                .sorted()
                .toList();
    }

    private AssignmentType findAssignmentByTargetRef(Set<AssignmentType> assignments, ObjectReferenceType targetRef) {
        if (assignments == null || targetRef == null) {
            return null;
        }

        return assignments.stream()
                .filter(a -> referencesEqual(a.getTargetRef(), targetRef, true))
                .findFirst()
                .orElse(null);
    }

    public QName getRelation() {
        return relation;
    }

    public void setRelation(QName relation) {
        if (relation == null) {
            relation = defaultRelation;
        }
        this.relation = relation;
    }

    public QName getDefaultRelation() {
        if (defaultRelation == null) {
            defaultRelation = findDefaultRelation();
        }
        return defaultRelation;
    }

    public long getWarningCount() {
        return getConflicts().stream().filter(c -> c.isWarning() && c.getState() != ConflictState.SOLVED).count();
    }

    public long getErrorCount() {
        return getConflicts().stream().filter(c -> !c.isWarning() && c.getState() != ConflictState.SOLVED).count();
    }

    public void clearCart() {
        poiMyself = null;
        poiGroupSelectionIdentifier = null;

        requestItems.clear();
        requestItemsExistingToRemove.clear();
        existingPoiRoleMemberships.clear();

        templateAssignments.clear();
        relation = null;

        selectedValidity = null;
        validityDuration = null;

        comment = null;

        conflictsDirty = false;
        conflicts.clear();
    }

    public boolean canSubmit() {
        return getErrorCount() == 0 && !requestItems.isEmpty() && requestItems.values().stream().anyMatch(c -> !c.isEmpty());
    }

    private void markConflictsDirty() {
        conflictsDirty = true;
    }

    public void computeConflicts(PageBase page) {
        if (!conflictsDirty) {
            return;
        }

        Task task = page.createSimpleTask(OPERATION_COMPUTE_ALL_CONFLICTS);
        OperationResult result = task.getResult();

        List<Conflict> allConflicts = new ArrayList<>();
        for (ObjectReferenceType ref : getPersonOfInterest()) {
            List<Conflict> conflicts = computeConflictsForOnePerson(ref, task, page);
            allConflicts.addAll(conflicts);
        }

        result.computeStatusIfUnknown();
        if (!WebComponentUtil.isSuccessOrHandledError(result) && allConflicts.isEmpty()) {
            conflictsDirty = false;
            page.error(OpResult.getOpResult(page, result));
            throw new RestartResponseException(page);
        }

        this.conflicts = allConflicts;
        conflictsDirty = false;
    }

    private ObjectReferenceType createRef(PrismObject<UserType> object) {
        if (object == null) {
            return null;
        }

        UserType user = object.asObjectable();

        return new ObjectReferenceType()
                .oid(object.getOid())
                .type(ObjectTypes.getObjectType(user.getClass()).getTypeQName())
                .targetName(user.getName());
    }

    public List<Conflict> computeConflictsForOnePerson(ObjectReferenceType ref, Task task, PageBase page) {
        OperationResult result = task.getResult().createSubresult(OPERATION_COMPUTE_CONFLICT);
        try {
            PrismObject<UserType> user = WebModelServiceUtils.loadObject(ref, page);
            ObjectReferenceType userRef = createRef(user);

            ObjectDelta<UserType> delta = createUserDelta(user);

            ModelExecuteOptions options = ModelExecuteOptions.create()
                    .partialProcessing(new PartialProcessingOptionsType()
                            .inbound(SKIP)
                            .projection(SKIP))
                    .ignoreAssignmentPruning();

            MidPointApplication mp = MidPointApplication.get();
            ModelContext<UserType> ctx = mp.getModelInteractionService()
                    .previewChanges(MiscUtil.createCollection(delta), options, task, result);

            DeltaSetTriple<? extends EvaluatedAssignment> evaluatedTriple = ctx.getEvaluatedAssignmentTriple();

            Map<String, Conflict> conflicts = new HashMap<>();
            if (evaluatedTriple != null) {
                processEvaluatedAssignments(userRef, conflicts, evaluatedTriple.getPlusSet());
                processEvaluatedAssignments(userRef, conflicts, evaluatedTriple.getZeroSet());
                processEvaluatedAssignments(userRef, conflicts, evaluatedTriple.getMinusSet());
            } else if (!result.isSuccess() && StringUtils.isNotEmpty(getSubresultWarningMessages(result))) {
                String msg = page.getString("PageAssignmentsList.conflictsWarning", getSubresultWarningMessages(result));
                page.warn(msg);
            }
            return new ArrayList<>(conflicts.values());
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get assignments conflicts. Reason: ", ex);
            if (ex instanceof CommonException cm && cm.getUserFriendlyMessage() != null) {
                result.setUserFriendlyMessage(cm.getUserFriendlyMessage());
            } else {
                result.recordFatalError("Couldn't get assignments conflicts", ex);
            }
        } finally {
            result.computeStatusIfUnknown();
        }

        return new ArrayList<>();
    }

    private void processEvaluatedAssignments(ObjectReferenceType userRef, Map<String, Conflict> conflicts,
            Collection<? extends EvaluatedAssignment> assignments) {
        if (assignments == null) {
            return;
        }

        for (EvaluatedAssignment evaluatedAssignment : assignments) {
            for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getAllTargetsPolicyRules()) {
                if (!policyRule.isTriggered() || !policyRule.containsEnabledAction()) {
                    continue;
                }

                // everything other than 'enforce' is a warning
                boolean warning = !policyRule.containsEnabledAction(EnforcementPolicyActionType.class);

                createConflicts(userRef, conflicts, evaluatedAssignment, policyRule.getAllTriggers(), warning);
            }
        }
    }

    private <F extends FocusType> void createConflicts(ObjectReferenceType userRef, Map<String, Conflict> conflicts, EvaluatedAssignment evaluatedAssignment,
            EvaluatedExclusionTrigger trigger, boolean warning) {

        EvaluatedAssignment conflictingAssignment = trigger.getConflictingAssignment();
        PrismObject<F> addedAssignmentTargetObj = (PrismObject<F>) evaluatedAssignment.getTarget();
        PrismObject<F> exclusionTargetObj = (PrismObject<F>) conflictingAssignment.getTarget();

        final String key = StringUtils.join(addedAssignmentTargetObj.getOid(), "/", exclusionTargetObj.getOid());
        final String alternateKey = StringUtils.join(exclusionTargetObj.getOid(), "/", addedAssignmentTargetObj.getOid());

        ConflictItem added = new ConflictItem(evaluatedAssignment.getAssignment(), WebComponentUtil.getDisplayNameOrName(addedAssignmentTargetObj),
                evaluatedAssignment.getAssignment(true) != null);
        ConflictItem exclusion = new ConflictItem(conflictingAssignment.getAssignment(), WebComponentUtil.getDisplayNameOrName(exclusionTargetObj),
                conflictingAssignment.getAssignment(true) != null);

        MidPointApplication mp = MidPointApplication.get();
        LocalizationService localizationService = mp.getLocalizationService();

        String message = null;
        if (trigger.getMessage() != null) {
            message = localizationService.translate(trigger.getMessage());
        }

        String shortMessage = null;
        if (trigger.getShortMessage() != null) {
            shortMessage = localizationService.translate(trigger.getShortMessage());
        }

        Conflict conflict = new Conflict(userRef, added, exclusion, shortMessage, message, warning);

        if (!conflicts.containsKey(key) && !conflicts.containsKey(alternateKey)) {
            conflicts.put(key, conflict);
        } else if (!warning) {
            if (conflicts.containsKey(key)) {
                conflicts.replace(key, conflict);
            }
            if (conflicts.containsKey(alternateKey)) {
                conflicts.replace(alternateKey, conflict);
            }
        }
    }

    private void createConflicts(ObjectReferenceType userRef, Map<String, Conflict> conflicts, EvaluatedAssignment evaluatedAssignment,
            Collection<EvaluatedPolicyRuleTrigger<?>> triggers, boolean warning) {

        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
            if (trigger instanceof EvaluatedExclusionTrigger evaluatedExclusionTrigger) {
                createConflicts(userRef, conflicts, evaluatedAssignment, evaluatedExclusionTrigger, warning);
            } else if (trigger instanceof EvaluatedCompositeTrigger compositeTrigger) {
                Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers = compositeTrigger.getInnerTriggers();
                createConflicts(userRef, conflicts, evaluatedAssignment, innerTriggers, warning);
            }
        }
    }

    private String getSubresultWarningMessages(OperationResult result) {
        if (result == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        result.getSubresults().forEach(subresult -> {
            if (subresult.isWarning()) {
                sb.append(subresult.getMessage());
                sb.append("\n");
            }
        });
        return sb.toString();
    }

    private ObjectDelta<UserType> createUserDelta(PrismObject<UserType> user) throws SchemaException {

        ObjectReferenceType userRef = createRef(user);
        List<AssignmentType> assignmentsToAdd = getShoppingCartAssignments(userRef);
        List<AssignmentType> assignmentsToRemove = getAssignmentsToBeRemoved(userRef);

        ObjectDelta<UserType> delta = user.createModifyDelta();

        PrismContainerDefinition<AssignmentType> def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);

        addAssignmentDeltas(delta, assignmentsToAdd, def, true);
        addAssignmentDeltas(delta, assignmentsToRemove, def, false);

        return delta;
    }

    private void addAssignmentDeltas(ObjectDelta<UserType> focusDelta, List<AssignmentType> assignments,
            PrismContainerDefinition<AssignmentType> def, boolean addAssignments) throws SchemaException {

        if (assignments.isEmpty()) {
            return;
        }

        PrismContext ctx = def.getPrismContext();
        ContainerDelta<AssignmentType> delta = ctx.deltaFactory().container().create(ItemPath.EMPTY_PATH, def.getItemName(), def);

        for (AssignmentType a : assignments) {
            PrismContainerValue<AssignmentType> newValue = a.asPrismContainerValue();

            newValue.applyDefinition(def, false);
            if (addAssignments) {
                delta.addValueToAdd(newValue.clone());
            } else {
                delta.addValueToDelete(newValue.clone());
            }
        }

        if (!delta.isEmpty()) {
            focusDelta.addModification(delta);
        }
    }

    private void addExistingToBeRemoved(ObjectReferenceType ref, AssignmentType assignment) {
        List<AssignmentType> list = requestItemsExistingToRemove.get(ref);
        if (list == null) {
            list = new ArrayList<>();
        }
        requestItemsExistingToRemove.put(ref, list);

        list.add(assignment);
    }

    public void solveConflict(Conflict conflict, ConflictItem toRemove) {
        if (toRemove.isExisting()) {
            addExistingToBeRemoved(conflict.getPersonOfInterest(), toRemove.getAssignment());
        }

        List<AssignmentType> assignments = requestItems.get(conflict.getPersonOfInterest());
        assignments.remove(toRemove.getAssignment());

        conflict.setState(ConflictState.SOLVED);
        conflict.setToBeRemoved(toRemove);

        // check if we didn't remove last instance of assignment for specific role from requestedItems
        // if so we have to remove it from selectedAssignments as well
        AssignmentType selected = findMatchingAssignment(templateAssignments, toRemove.getAssignment(), true);    // selected from role catalog
        boolean found = false;
        for (List<AssignmentType> list : requestItems.values()) {
            if (list.stream().anyMatch(a -> matchAssignments(a, selected))) {
                found = true;
                break;
            }
        }

        if (!found) {
            templateAssignments.remove(selected);
        }
    }

    public boolean isAllConflictsSolved() {
        return getConflicts().stream().noneMatch(c -> c.getState() == ConflictState.UNRESOLVED);
    }

    public OperationResult submitRequest(PageBase page) {
        int usersCount = requestItems.keySet().size();
        if (usersCount == 0) {
            return null;
        }

        if (usersCount == 1) {
            return submitSingleRequest(page);
        }

        return submitMultiRequest(page);
    }

    private OperationResult submitMultiRequest(PageBase page) {
        ExplicitChangeExecutionWorkDefinitionType explicitChangeExecution = new ExplicitChangeExecutionWorkDefinitionType();

        for (ObjectReferenceType poiRef : requestItems.keySet()) {
            try {
                ChangeExecutionRequestType request = new ChangeExecutionRequestType();
                request.setName(
                        page.getString(
                                "RequestAccess.changeExecutionRequestName",
                                WebComponentUtil.getOrigStringFromPoly(poiRef.getTargetName())));

                ModelExecuteOptions options = createSubmitModelOptions(page.getPrismContext());
                options.initialPartialProcessing(new PartialProcessingOptionsType().inbound(SKIP).projection(SKIP));
                Boolean executeAfterApprovals = isDefaultExecuteAfterAllApprovals(page);
                options.executeImmediatelyAfterApproval(executeAfterApprovals != null ? !executeAfterApprovals : null);
                request.setExecutionOptions(options.toModelExecutionOptionsType());

                PrismObject<UserType> user = WebModelServiceUtils.loadObject(poiRef, page);
                ObjectDelta<UserType> delta = createUserDelta(user);
                ObjectDeltaType deltaType = DeltaConvertor.toObjectDeltaType(delta);
                request.getDelta().add(deltaType);
                explicitChangeExecution.getRequest().add(request);
            } catch (SchemaException ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't prepare change execution request with object delta changes for poi "
                        + WebComponentUtil.getName(poiRef, false) + ". Reason: ", ex);
            }
        }

        Task task = page.createSimpleTask(OPERATION_REQUEST_ASSIGNMENTS);
        OperationResult result = task.getResult();

        TaskType taskTemplate = new TaskType()
                .name(page.getString(
                        "RequestAccess.changeExecutionRequestTaskName", explicitChangeExecution.getRequest().size()));

        try {
            page.getModelInteractionService().submit(
                    ActivityDefinitionBuilder.create(explicitChangeExecution).build(),
                    ActivitySubmissionOptions.create().withTaskTemplate(taskTemplate),
                    task, result);
        } catch (CommonException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't submit task wih explicit change execution data. Reason: ", ex);
            result.recordException("Couldn't prepare task wih explicit change execution data", ex);
        } finally {
            result.close();
        }

        return result;
    }

    private OperationResult submitSingleRequest(PageBase page) {
        Task task = page.createSimpleTask(OPERATION_REQUEST_ASSIGNMENTS_SINGLE);
        OperationResult result = task.getResult();

        ObjectReferenceType poiRef = requestItems.keySet().stream().findFirst().orElse(null);

        ObjectDelta<UserType> delta;
        try {
            PrismObject<UserType> user = WebModelServiceUtils.loadObject(poiRef, page);
            delta = createUserDelta(user);

            ModelExecuteOptions options = createSubmitModelOptions(page.getPrismContext());
            options.initialPartialProcessing(new PartialProcessingOptionsType().inbound(SKIP).projection(SKIP));
            Boolean executeAfterApprovals = isDefaultExecuteAfterAllApprovals(page);
            options.executeImmediatelyAfterApproval(executeAfterApprovals != null ? !executeAfterApprovals : null);
            page.getModelService().executeChanges(Collections.singletonList(delta), options, task, result);

            result.recordSuccess();
        } catch (Exception e) {
            result.recordFatalError(e);
            result.setMessage(page.createStringResource("PageAssignmentsList.requestError").getString());
            LoggingUtils.logUnexpectedException(LOGGER, "Could not save assignments ", e);
        } finally {
            result.computeStatusIfUnknown();
        }

        return result;
    }

    private ModelExecuteOptions createSubmitModelOptions(PrismContext ctx) {
        OperationBusinessContextType businessContextType;

        if (StringUtils.isNotEmpty(comment)) {
            businessContextType = new OperationBusinessContextType();
            businessContextType.setComment(comment);
        } else {
            businessContextType = null;
        }

        ModelExecuteOptions options = ExecuteChangeOptionsDto.createFromSystemConfiguration().createOptions(ctx);
        options.requestBusinessContext(businessContextType);

        return options;
    }

    public Duration getValidity() {
        return validityDuration;
    }

    public void setValidityDuration(Duration validity) {
        if (Objects.equals(this.validityDuration, validity)) {
            return;
        }

        this.validityDuration = validity;

        setRequestItemsValidity(validity);
    }

    private void setRequestItemsValidity(Duration validity) {
        XMLGregorianCalendar from = validity != null ? XmlTypeConverter.createXMLGregorianCalendar() : null;

        XMLGregorianCalendar to = null;
        if (validity != null) {
            to = XmlTypeConverter.createXMLGregorianCalendar(from);
            to.add(validity);
        }

        setRequestItemsValidity(from, to);
    }

    public void setRequestItemsValidity(XMLGregorianCalendar from, XMLGregorianCalendar to) {
        for (List<AssignmentType> list : requestItems.values()) {
            list.forEach(a -> {
                if (from == null && to == null) {
                    a.setActivation(null);
                } else {
                    ActivationType activation = a.getActivation();
                    if (activation == null) {
                        activation = new ActivationType();
                        a.setActivation(activation);
                    }

                    activation.validFrom(from).validTo(to);
                }
            });
        }

        markConflictsDirty();
    }

    private PrismObject<UserType> resolveFirstPoiReference(PageBase page) {
        List<ObjectReferenceType> personsOfInterest = getPersonOfInterest();
        ObjectReferenceType ref;
        if (personsOfInterest.isEmpty()) {
            return null;
        }

        ref = personsOfInterest.get(0);
        try {
            return WebModelServiceUtils.loadObject(ref, page);
        } catch (Exception ex) {
            page.error(page.getString("RequestAccess.resolveFirstPersonOfInterest", ref.getTargetName(), ref.getOid()));
        }

        return null;
    }

    private List<QName> getAssignableRelationList(PageBase page) {
        PrismObject<UserType> focus = resolveFirstPoiReference(page);
        if (focus == null) {
            return new ArrayList<>();
        }

        Task task = page.createSimpleTask(OPERATION_LOAD_ASSIGNABLE_RELATIONS_LIST);
        OperationResult result = task.getResult();

        return WebComponentUtil.getAssignableRelationsList(
                focus, RoleType.class, WebComponentUtil.AssignmentOrder.ASSIGNMENT, result, task, page);
    }

    public ObjectFilter getAssignableRolesFilter(PageBase page, Class<? extends AbstractRoleType> targetType) {
        PrismObject<UserType> focus = resolveFirstPoiReference(page);
        return getAssignableRolesFilter(focus, page, targetType);
    }

    public ObjectFilter getAssignableRolesFilter(PrismObject<UserType> focus, PageBase page,
            Class<? extends AbstractRoleType> targetType) {
        if (targetType == null) {
            return null;
        }

        if (focus == null) {
            return null;
        }

        QName relation = getRelation();

        Task task = page.createSimpleTask(OPERATION_LOAD_ASSIGNABLE_ROLES);
        OperationResult result = task.getResult();

        return WebComponentUtil.getAssignableRolesFilter(focus, targetType, relation,
                WebComponentUtil.AssignmentOrder.ASSIGNMENT, result, task, page);
    }

    public List<QName> getAvailableRelations(PageBase page) {
        List<QName> relations = getAssignableRelationList(page);

        if (CollectionUtils.isEmpty(relations)) {
            relations = RelationUtil.getCategoryRelationChoices(AreaCategoryType.SELF_SERVICE, page);
        }

        RelationSelectionType config = getRelationConfiguration(page);
        QName defaultRelation = getDefaultRelation();
        relations = relations.stream()
                // filter out non default relations if configuration doesn't allow other relations
                .filter(q -> BooleanUtils.isNotFalse(config.isAllowOtherRelations()) || defaultRelation == null || q.equals(defaultRelation))
                .collect(Collectors.toList());

        return relations;
    }

    private RelationSelectionType getRelationConfiguration(Page page) {
        AccessRequestType config = getAccessRequestConfiguration(page);
        RelationSelectionType relation = null;
        if (config != null) {
            relation = config.getRelationSelection();
        }

        return relation != null ? relation : new RelationSelectionType();
    }

    private Boolean isDefaultExecuteAfterAllApprovals(Page page) {
        SystemConfigurationType config = MidPointApplication.get().getSystemConfigurationIfAvailable();
        if (config == null || config.getRoleManagement() == null) {
            return null;
        }

        RoleManagementConfigurationType roleManagement = config.getRoleManagement();
        return roleManagement.isDefaultExecuteAfterAllApprovals();
    }

    public AccessRequestType getAccessRequestConfiguration(Page page) {
        CompiledGuiProfile profile = WebComponentUtil.getCompiledGuiProfile(page);
        if (profile == null) {
            return null;
        }

        return profile.getAccessRequest();
    }

    private QName findDefaultRelation() {
        AccessRequestType ar = getAccessRequestConfiguration(null);
        if (ar == null || ar.getRelationSelection() == null) {
            return SchemaConstants.ORG_DEFAULT;
        }

        RelationSelectionType config = ar.getRelationSelection();
        QName defaultRelation = null;
        if (config != null) {
            defaultRelation = config.getDefaultRelation();
        }

        return defaultRelation != null ? defaultRelation : SchemaConstants.ORG_DEFAULT;
    }

    public boolean isAssignedToAll(String oid) {
        return countNumberOfAssignees(oid) == requestItems.keySet().size();
    }

    public boolean isAssignedToNone(String oid) {
        return countNumberOfAssignees(oid) == 0;
    }

    private int countNumberOfAssignees(String oid) {
        if (oid == null) {
            return 0;
        }

        int count = 0;
        for (List<ObjectReferenceType> memberships : existingPoiRoleMemberships.values()) {
            boolean found = memberships.stream().anyMatch(m -> oid.equals(m.getOid()));
            if (found) {
                count++;
            }
        }

        return count;
    }

    public void updateSelectedAssignment(AssignmentType updated) {
        AssignmentType matching = findMatchingAssignment(templateAssignments, updated, true);
        if (matching == null) {
            return;
        }

        templateAssignments.remove(matching);
        templateAssignments.add(updated.clone());

        // we'll find existing assignments for this role and replace them with updated one
        for (List<AssignmentType> list : requestItems.values()) {
            AssignmentType real = list.stream().filter(a -> matchAssignments(a, matching)).findFirst().orElse(null);
            if (real == null) {
                continue;
            }

            list.remove(real);
            list.add(updated.clone());
        }
    }

    /**
     * This method checks if there is already assignment for specific role in shopping cart - matching is
     * done based on targetRef oid and relation.
     * Matching takes into account {@link AssignmentConstraintsType}.
     */
    public boolean canAddTemplateAssignment(ObjectReferenceType newTargetRef) {
        AssignmentConstraintsType constraints = getDefaultAssignmentConstraints();
        return canAddTemplateAssignment(newTargetRef, constraints);
    }

    private boolean hasSimilarMembership(List<ObjectReferenceType> memberships, ObjectReferenceType newTargetRef, boolean allowSameTarget, boolean allowSameRelation) {
        List<ObjectReferenceType> equalMemberships = memberships.stream()
                .filter(m -> referencesEqual(newTargetRef, m, false))
                .toList();

        if (equalMemberships.isEmpty()) {
            return false;
        }

        if (!allowSameTarget) {
            return true;
        }

        if (allowSameRelation) {
            return false;
        }

        return equalMemberships.stream().anyMatch(r -> Objects.equals(r.getRelation(), newTargetRef.getRelation()));
    }

    private boolean canAddTemplateAssignment(ObjectReferenceType newTargetRef, AssignmentConstraintsType constraints) {
        boolean allowSameTarget = isAllowSameTarget(constraints);
        boolean allowSameRelation = isAllowSameRelation(constraints);

        // if everyone in "shopping cart" has already this assignment, we can't add it again (this also depends on constraints)
        boolean allPoiHasMatching = true;
        for (List<ObjectReferenceType> memberships : existingPoiRoleMemberships.values()) {
            boolean found = hasSimilarMembership(memberships, newTargetRef, allowSameTarget, allowSameRelation);
            if (!found) {
                allPoiHasMatching = false;
            }
        }

        if (allPoiHasMatching) {
            return false;
        }

        // if there's already "template" assignment (picked one by user in role catalog) we can't add it again
        return getTemplateAssignments().stream()
                .noneMatch(a -> {
                    ObjectReferenceType aTargetRef = a.getTargetRef();

                    // we do care about relation in shopping cart, since we don't want to have multiple equals assignments for same role
                    // that would fully mess up shopping cart handling (removing, modification of assignments) - if assignments aren't unique
                    return referencesEqual(newTargetRef, aTargetRef, true);
                });
    }

    private boolean referencesEqual(ObjectReferenceType one, ObjectReferenceType two, boolean checkRelation) {
        if (one == null && two == null) {
            return true;
        }

        if (one == null || two == null) {
            return false;
        }

        if (!Objects.equals(one.getOid(), two.getOid()) || !Objects.equals(one.getType(), two.getType())) {
            return false;
        }

        // oid & type already equal here
        if (!checkRelation) {
            return true;
        }

        return Objects.equals(one.getRelation(), two.getRelation());
    }

    public int getPoiCount() {
        return getPersonOfInterest().size();
    }
}
