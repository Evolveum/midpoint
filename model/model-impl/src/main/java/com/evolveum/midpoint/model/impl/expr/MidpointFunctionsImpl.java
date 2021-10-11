/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.api.expr.OptimizingTriggerCreator;
import com.evolveum.midpoint.model.common.ArchetypeManager;
import com.evolveum.midpoint.model.common.ConstantsManager;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.expr.triggerSetter.OptimizingTriggerCreatorImpl;
import com.evolveum.midpoint.model.impl.expr.triggerSetter.TriggerCreatorGlobalState;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.api.context.SynchronizationIntent;
import com.evolveum.midpoint.model.impl.messaging.MessageWrapper;
import com.evolveum.midpoint.model.impl.sync.SynchronizationExpressionsEvaluator;
import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.model.impl.sync.SynchronizationServiceUtils;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_CREDENTIALS_PASSWORD;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_CREDENTIALS_PASSWORD_VALUE;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType.RUNNABLE;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

/**
 * @author semancik
 *
 */
@Component
public class MidpointFunctionsImpl implements MidpointFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointFunctionsImpl.class);

    public static final String CLASS_DOT = MidpointFunctions.class.getName() + ".";

    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private ModelService modelService;
    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired private ModelObjectResolver modelObjectResolver;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private transient Protector protector;
    @Autowired private OrgStructFunctionsImpl orgStructFunctions;
    @Autowired private WorkflowService workflowService;
    @Autowired private ConstantsManager constantsManager;
    @Autowired private LocalizationService localizationService;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private SynchronizationExpressionsEvaluator correlationConfirmationEvaluator;
    @Autowired private ArchetypeManager archetypeManager;
    @Autowired private TriggerCreatorGlobalState triggerCreatorGlobalState;
    @Autowired private TaskManager taskManager;
    @Autowired private SchemaHelper schemaHelper;
    @Context HttpServletRequest httpServletRequest;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    public String hello(String name) {
        return "Hello " + name;
    }

    @Override
    public PrismContext getPrismContext() {
        return prismContext;
    }

    @Override
    public RelationRegistry getRelationRegistry() {
        return relationRegistry;
    }

    @Override
    public List<String> toList(String... s) {
        return Arrays.asList(s);
    }

    @Override
    public UserType getUserByOid(String oid) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(UserType.class, oid, null, new OperationResult("getUserByOid")).asObjectable();
    }

    @Override
    public boolean isMemberOf(UserType user, String orgOid) {
        for (ObjectReferenceType objectReferenceType : user.getParentOrgRef()) {
            if (orgOid.equals(objectReferenceType.getOid())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getPlaintextUserPassword(FocusType user) throws EncryptionException {
        if (user == null || user.getCredentials() == null || user.getCredentials().getPassword() == null) {
            return null;        // todo log a warning here?
        }
        ProtectedStringType protectedStringType = user.getCredentials().getPassword().getValue();
        if (protectedStringType != null) {
            return protector.decryptString(protectedStringType);
        } else {
            return null;
        }
    }

    @Override
    public String getPlaintextAccountPassword(ShadowType account) throws EncryptionException {
        if (account == null || account.getCredentials() == null || account.getCredentials().getPassword() == null) {
            return null;        // todo log a warning here?
        }
        ProtectedStringType protectedStringType = account.getCredentials().getPassword().getValue();
        if (protectedStringType != null) {
            return protector.decryptString(protectedStringType);
        } else {
            return null;
        }
    }

    @Override
    public String getPlaintextAccountPasswordFromDelta(ObjectDelta<? extends ShadowType> delta) throws EncryptionException {

        if (delta.isAdd()) {
            ShadowType newShadow = delta.getObjectToAdd().asObjectable();
            return getPlaintextAccountPassword(newShadow);
        }
        if (!delta.isModify()) {
            return null;
        }

        List<ProtectedStringType> passwords = new ArrayList<>();
        for (ItemDelta itemDelta : delta.getModifications()) {
            takePasswordsFromItemDelta(passwords, itemDelta);
        }
        LOGGER.trace("Found " + passwords.size() + " password change value(s)");
        if (!passwords.isEmpty()) {
            return protector.decryptString(passwords.get(passwords.size() - 1));
        } else {
            return null;
        }
    }

    private void takePasswordsFromItemDelta(List<ProtectedStringType> passwords, ItemDelta itemDelta) {
        if (itemDelta.isDelete()) {
            return;
        }

        if (itemDelta.getPath().equivalent(PATH_CREDENTIALS_PASSWORD_VALUE)) {
            LOGGER.trace("Found password value add/modify delta");
            //noinspection unchecked
            Collection<PrismPropertyValue<ProtectedStringType>> values = itemDelta.isAdd() ?
                    itemDelta.getValuesToAdd() :
                    itemDelta.getValuesToReplace();
            for (PrismPropertyValue<ProtectedStringType> value : values) {
                passwords.add(value.getValue());
            }
        } else if (itemDelta.getPath().equivalent(PATH_CREDENTIALS_PASSWORD)) {
            LOGGER.trace("Found password add/modify delta");
            //noinspection unchecked
            Collection<PrismContainerValue<PasswordType>> values = itemDelta.isAdd() ?
                    itemDelta.getValuesToAdd() :
                    itemDelta.getValuesToReplace();
            for (PrismContainerValue<PasswordType> value : values) {
                if (value.asContainerable().getValue() != null) {
                    passwords.add(value.asContainerable().getValue());
                }
            }
        } else if (itemDelta.getPath().equivalent(ShadowType.F_CREDENTIALS)) {
            LOGGER.trace("Found credentials add/modify delta");
            //noinspection unchecked
            Collection<PrismContainerValue<CredentialsType>> values = itemDelta.isAdd() ?
                    itemDelta.getValuesToAdd() :
                    itemDelta.getValuesToReplace();
            for (PrismContainerValue<CredentialsType> value : values) {
                if (value.asContainerable().getPassword() != null && value.asContainerable().getPassword().getValue() != null) {
                    passwords.add(value.asContainerable().getPassword().getValue());
                }
            }
        }
    }

    @Override
    public String getPlaintextUserPasswordFromDeltas(List<ObjectDelta<? extends FocusType>> objectDeltas) throws EncryptionException {

        List<ProtectedStringType> passwords = new ArrayList<>();

        for (ObjectDelta<? extends FocusType> delta : objectDeltas) {

            if (delta.isAdd()) {
                FocusType newObject = delta.getObjectToAdd().asObjectable();
                return getPlaintextUserPassword(newObject); // for simplicity we do not look for other values
            }

            if (!delta.isModify()) {
                continue;
            }

            for (ItemDelta itemDelta : delta.getModifications()) {
                takePasswordsFromItemDelta(passwords, itemDelta);
            }
        }
        LOGGER.trace("Found " + passwords.size() + " password change value(s)");
        if (!passwords.isEmpty()) {
            return protector.decryptString(passwords.get(passwords.size() - 1));
        } else {
            return null;
        }
    }

    @Override
    public <F extends ObjectType> boolean hasLinkedAccount(String resourceOid) {
        ModelContext<F> ctx = ModelExpressionThreadLocalHolder.getLensContext();
        if (ctx == null) {
            throw new IllegalStateException("No lens context");
        }
        ModelElementContext<F> focusContext = ctx.getFocusContext();
        if (focusContext == null) {
            throw new IllegalStateException("No focus in lens context");
        }

        ScriptExpressionEvaluationContext scriptContext = ScriptExpressionEvaluationContext.getThreadLocal();

        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceOid, ShadowKindType.ACCOUNT, null, null, false);
        ModelProjectionContext projectionContext = ctx.findProjectionContext(rat);
        if (projectionContext == null) {
            // but check if it is not among list of deleted contexts
            if (scriptContext == null || scriptContext.isEvaluateNew()) {
                return false;
            }
            // evaluating old state
            for (ResourceShadowDiscriminator deletedOne : ctx.getHistoricResourceObjects()) {
                if (resourceOid.equals(deletedOne.getResourceOid()) && deletedOne.getKind() == ShadowKindType.ACCOUNT
                        && deletedOne.getIntent() == null || "default"
                        .equals(deletedOne.getIntent())) {        // TODO implement this seriously
                    LOGGER.trace("Found deleted one: {}", deletedOne);  // TODO remove
                    return true;
                }
            }
            return false;
        }

        if (projectionContext.isTombstone()) {
            return false;
        }

        SynchronizationPolicyDecision synchronizationPolicyDecision = projectionContext.getSynchronizationPolicyDecision();
        SynchronizationIntent synchronizationIntent = projectionContext.getSynchronizationIntent();
        if (scriptContext == null) {
            if (synchronizationPolicyDecision == null) {
                return synchronizationIntent != SynchronizationIntent.DELETE && synchronizationIntent != SynchronizationIntent.UNLINK;
            } else {
                return synchronizationPolicyDecision != SynchronizationPolicyDecision.DELETE && synchronizationPolicyDecision != SynchronizationPolicyDecision.UNLINK;
            }
        } else if (scriptContext.isEvaluateNew()) {
            // Evaluating new state
            if (focusContext.isDelete()) {
                return false;
            }
            if (synchronizationPolicyDecision == null) {
                return synchronizationIntent != SynchronizationIntent.DELETE && synchronizationIntent != SynchronizationIntent.UNLINK;
            } else {
                return synchronizationPolicyDecision != SynchronizationPolicyDecision.DELETE && synchronizationPolicyDecision != SynchronizationPolicyDecision.UNLINK;
            }
        } else {
            // Evaluating old state
            if (focusContext.isAdd()) {
                return false;
            }
            if (synchronizationPolicyDecision == null) {
                return synchronizationIntent != SynchronizationIntent.ADD;
            } else {
                return synchronizationPolicyDecision != SynchronizationPolicyDecision.ADD;
            }
        }
    }

    @Override
    public <F extends FocusType> boolean isDirectlyAssigned(F focusType, String targetOid) {
        for (AssignmentType assignment : focusType.getAssignment()) {
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null && targetRef.getOid().equals(targetOid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <F extends FocusType> boolean isDirectlyAssigned(F focusType, ObjectType target) {
        return isDirectlyAssigned(focusType, target.getOid());
    }

    @Override
    public boolean isDirectlyAssigned(String targetOid) {
        ModelContext<? extends FocusType> ctx = ModelExpressionThreadLocalHolder.getLensContext();
        if (ctx == null) {
            throw new IllegalStateException("No lens context");
        }
        ModelElementContext<? extends FocusType> focusContext = ctx.getFocusContext();
        if (focusContext == null) {
            throw new IllegalStateException("No focus in lens context");
        }

        PrismObject<? extends FocusType> focus;
        ScriptExpressionEvaluationContext scriptContext = ScriptExpressionEvaluationContext.getThreadLocal();
        if (scriptContext == null) {
            focus = focusContext.getObjectAny();
        } else if (scriptContext.isEvaluateNew()) {
            // Evaluating new state
            if (focusContext.isDelete()) {
                return false;
            }
            focus = focusContext.getObjectNew();
        } else {
            // Evaluating old state
            if (focusContext.isAdd()) {
                return false;
            }
            focus = focusContext.getObjectOld();
        }
        if (focus == null) {
            return false;
        }
        return isDirectlyAssigned(focus.asObjectable(), targetOid);
    }

    @Override
    public boolean isDirectlyAssigned(ObjectType target) {
        return isDirectlyAssigned(target.getOid());
    }

    // EXPERIMENTAL!!
    @SuppressWarnings("unused")
    @Experimental
    public boolean hasActiveAssignmentTargetSubtype(String roleSubtype) {
        ModelContext<ObjectType> lensContext = ModelExpressionThreadLocalHolder.getLensContext();
        if (lensContext == null) {
            throw new UnsupportedOperationException("hasActiveAssignmentRoleSubtype works only with model context");
        }
        DeltaSetTriple<? extends EvaluatedAssignment<?>> evaluatedAssignmentTriple = lensContext.getEvaluatedAssignmentTriple();
        if (evaluatedAssignmentTriple == null) {
            throw new UnsupportedOperationException("hasActiveAssignmentRoleSubtype works only with evaluatedAssignmentTriple");
        }
        Collection<? extends EvaluatedAssignment<?>> nonNegativeEvaluatedAssignments = evaluatedAssignmentTriple.getNonNegativeValues();
        for (EvaluatedAssignment<?> nonNegativeEvaluatedAssignment : nonNegativeEvaluatedAssignments) {
            PrismObject<?> target = nonNegativeEvaluatedAssignment.getTarget();
            if (target == null) {
                continue;
            }
            //noinspection unchecked
            Collection<String> targetSubtypes = FocusTypeUtil.determineSubTypes((PrismObject) target);
            if (targetSubtypes.contains(roleSubtype)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ShadowType getLinkedShadow(FocusType focus, ResourceType resource) throws SchemaException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return getLinkedShadow(focus, resource.getOid());
    }

    @Override
    public ShadowType getLinkedShadow(FocusType focus, ResourceType resource, boolean repositoryObjectOnly)
            throws SchemaException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return getLinkedShadow(focus, resource.getOid(), repositoryObjectOnly);
    }

    @Override
    public ShadowType getLinkedShadow(FocusType focus, String resourceOid)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return getLinkedShadow(focus, resourceOid, false);
    }

    @Override
    public ShadowType getLinkedShadow(FocusType focus, String resourceOid, boolean repositoryObjectOnly)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        if (focus == null) {
            return null;
        }
        List<ObjectReferenceType> linkRefs = focus.getLinkRef();
        for (ObjectReferenceType linkRef : linkRefs) {
            ShadowType shadowType;
            try {
                shadowType = getObject(ShadowType.class, linkRef.getOid(),
                        SelectorOptions.createCollection(GetOperationOptions.createNoFetch()));
            } catch (ObjectNotFoundException e) {
                // Shadow is gone in the meantime. MidPoint will resolve that by itself.
                // It is safe to ignore this error in this method.
                LOGGER.trace("Ignoring shadow " + linkRef.getOid() + " linked in " + focus
                        + " because it no longer exists in repository");
                continue;
            }
            if (shadowType.getResourceRef().getOid().equals(resourceOid)) {
                // We have repo shadow here. Re-read resource shadow if necessary.
                if (!repositoryObjectOnly) {
                    try {
                        shadowType = getObject(ShadowType.class, shadowType.getOid());
                    } catch (ObjectNotFoundException e) {
                        // Shadow is gone in the meantime. MidPoint will resolve that by itself.
                        // It is safe to ignore this error in this method.
                        LOGGER.trace("Ignoring shadow " + linkRef.getOid() + " linked in " + focus
                                + " because it no longer exists on resource");
                        continue;
                    }
                }
                return shadowType;
            }
        }
        return null;
    }

    @Override
    public ShadowType getLinkedShadow(FocusType focus, String resourceOid, ShadowKindType kind, String intent)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return getLinkedShadow(focus, resourceOid, kind, intent, false);
    }

    @Override
    public ShadowType getLinkedShadow(FocusType focus, String resourceOid, ShadowKindType kind, String intent,
            boolean repositoryObjectOnly)
            throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        List<ObjectReferenceType> linkRefs = focus.getLinkRef();
        for (ObjectReferenceType linkRef : linkRefs) {
            ShadowType shadowType;
            try {
                shadowType = getObject(ShadowType.class, linkRef.getOid(),
                        SelectorOptions.createCollection(GetOperationOptions.createNoFetch()));
            } catch (ObjectNotFoundException e) {
                // Shadow is gone in the meantime. MidPoint will resolve that by itself.
                // It is safe to ignore this error in this method.
                LOGGER.trace("Ignoring shadow " + linkRef.getOid() + " linked in " + focus
                        + " because it no longer exists in repository");
                continue;
            }
            if (ShadowUtil.matches(shadowType, resourceOid, kind, intent)) {
                // We have repo shadow here. Re-read resource shadow if necessary.
                if (!repositoryObjectOnly) {
                    try {
                        shadowType = getObject(ShadowType.class, shadowType.getOid());
                    } catch (ObjectNotFoundException e) {
                        // Shadow is gone in the meantime. MidPoint will resolve that by itself.
                        // It is safe to ignore this error in this method.
                        LOGGER.trace("Ignoring shadow " + linkRef.getOid() + " linked in " + focus
                                + " because it no longer exists on resource");
                        continue;
                    }
                }
                return shadowType;
            }
        }
        return null;
    }

    @Override
    public boolean isFullShadow() {
        ModelProjectionContext projectionContext = getProjectionContext();
        if (projectionContext == null) {
            LOGGER.debug("Call to isFullShadow while there is no projection context");
            return false;
        }
        return projectionContext.isFullShadow();
    }

    @Override
    public boolean isProjectionExists() {
        ModelProjectionContext projectionContext = getProjectionContext();
        if (projectionContext == null) {
            return false;
        }
        return projectionContext.isExists();
    }

    @Override
    public <T> Integer countAccounts(String resourceOid, QName attributeName, T attributeValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = getCurrentResult(CLASS_DOT + "countAccounts");
        ResourceType resourceType = modelObjectResolver.getObjectSimple(ResourceType.class, resourceOid, null, null, result);
        return countAccounts(resourceType, attributeName, attributeValue, getCurrentTask(), result);
    }

    @Override
    public <T> Integer countAccounts(ResourceType resourceType, QName attributeName, T attributeValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName() + "countAccounts");
        return countAccounts(resourceType, attributeName, attributeValue, getCurrentTask(), result);
    }

    @Override
    public <T> Integer countAccounts(ResourceType resourceType, String attributeName, T attributeValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName() + "countAccounts");
        QName attributeQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), attributeName);
        return countAccounts(resourceType, attributeQName, attributeValue, getCurrentTask(), result);
    }

    private <T> Integer countAccounts(ResourceType resourceType, QName attributeName, T attributeValue, Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        ObjectQuery query = createAttributeQuery(resourceType, attributeName, attributeValue);
        return modelObjectResolver.countObjects(ShadowType.class, query, null, task, result);
    }

    @Override
    public <T> boolean isUniquePropertyValue(ObjectType objectType, String propertyPathString, T propertyValue)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Validate.notEmpty(propertyPathString, "Empty property path");
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName() + "isUniquePropertyValue");
        ItemPath propertyPath = prismContext.itemPathParser().asItemPath(propertyPathString);
        return isUniquePropertyValue(objectType, propertyPath, propertyValue, getCurrentTask(), result);
    }

    private <T> boolean isUniquePropertyValue(final ObjectType objectType, ItemPath propertyPath, T propertyValue, Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        List<? extends ObjectType> conflictingObjects = getObjectsInConflictOnPropertyValue(objectType, propertyPath,
                propertyValue, null, false, task, result);
        return conflictingObjects.isEmpty();
    }

    @Override
    public <O extends ObjectType, T> List<O> getObjectsInConflictOnPropertyValue(O objectType, String propertyPathString,
            T propertyValue, boolean getAllConflicting)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return getObjectsInConflictOnPropertyValue(objectType, propertyPathString, propertyValue,
                PrismConstants.DEFAULT_MATCHING_RULE_NAME.getLocalPart(), getAllConflicting);
    }

    public <O extends ObjectType, T> List<O> getObjectsInConflictOnPropertyValue(O objectType, String propertyPathString,
            T propertyValue, String matchingRuleName, boolean getAllConflicting)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Validate.notEmpty(propertyPathString, "Empty property path");
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName() + "getObjectsInConflictOnPropertyValue");
        ItemPath propertyPath = prismContext.itemPathParser().asItemPath(propertyPathString);
        QName matchingRuleQName = new QName(matchingRuleName);      // no namespace for now
        return getObjectsInConflictOnPropertyValue(objectType, propertyPath, propertyValue, matchingRuleQName, getAllConflicting,
                getCurrentTask(), result);
    }

    private <O extends ObjectType, T> List<O> getObjectsInConflictOnPropertyValue(final O objectType, ItemPath propertyPath,
            T propertyValue, QName matchingRule, final boolean getAllConflicting, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Validate.notNull(objectType, "Null object");
        Validate.notNull(propertyPath, "Null property path");
        Validate.notNull(propertyValue, "Null property value");
        PrismPropertyDefinition<T> propertyDefinition = objectType.asPrismObject().getDefinition()
                .findPropertyDefinition(propertyPath);
        if (matchingRule == null) {
            if (propertyDefinition != null && PolyStringType.COMPLEX_TYPE.equals(propertyDefinition.getTypeName())) {
                matchingRule = PrismConstants.POLY_STRING_ORIG_MATCHING_RULE_NAME;
            } else {
                matchingRule = PrismConstants.DEFAULT_MATCHING_RULE_NAME;
            }
        }
        ObjectQuery query = prismContext.queryFor(objectType.getClass())
                .item(propertyPath, propertyDefinition).eq(propertyValue).matching(matchingRule)
                .build();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Determining uniqueness of property {} using query:\n{}", propertyPath, query.debugDump());
        }

        final List<O> conflictingObjects = new ArrayList<>();
        ResultHandler<O> handler = (object, parentResult) -> {
            if (objectType.getOid() == null) {
                // We have found a conflicting object
                conflictingObjects.add(object.asObjectable());
                return getAllConflicting;
            } else {
                if (objectType.getOid().equals(object.getOid())) {
                    // We have found ourselves. No conflict (yet). Just go on.
                    return true;
                } else {
                    // We have found someone else. Conflict.
                    conflictingObjects.add(object.asObjectable());
                    return getAllConflicting;
                }
            }
        };

        //noinspection unchecked
        modelObjectResolver.searchIterative((Class) objectType.getClass(), query, null, handler, task, result);

        return conflictingObjects;
    }

    @Override
    public <T> boolean isUniqueAccountValue(ResourceType resourceType, ShadowType shadowType, String attributeName,
            T attributeValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Validate.notEmpty(attributeName, "Empty attribute name");
        OperationResult result = getCurrentResult(MidpointFunctions.class.getName() + "isUniqueAccountValue");
        QName attributeQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), attributeName);
        return isUniqueAccountValue(resourceType, shadowType, attributeQName, attributeValue, getCurrentTask(), result);
    }

    private <T> boolean isUniqueAccountValue(ResourceType resourceType, final ShadowType shadowType,
            QName attributeName, T attributeValue, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Validate.notNull(resourceType, "Null resource");
        Validate.notNull(shadowType, "Null shadow");
        Validate.notNull(attributeName, "Null attribute name");
        Validate.notNull(attributeValue, "Null attribute value");

        ObjectQuery query = createAttributeQuery(resourceType, attributeName, attributeValue);
        LOGGER.trace("Determining uniqueness of attribute {} using query:\n{}", attributeName, query.debugDumpLazily());

        final Holder<Boolean> isUniqueHolder = new Holder<>(true);
        ResultHandler<ShadowType> handler = (object, parentResult) -> {
            if (shadowType.getOid() == null) {
                // We have found a conflicting object
                isUniqueHolder.setValue(false);
                return false;
            } else {
                if (shadowType.getOid().equals(object.getOid())) {
                    // We have found ourselves. No conflict (yet). Just go on.
                    return true;
                } else {
                    // We have found someone else. Conflict.
                    isUniqueHolder.setValue(false);
                    return false;
                }
            }
        };

        modelObjectResolver.searchIterative(ShadowType.class, query, null, handler, task, result);

        return isUniqueHolder.getValue();
    }

    private <T> ObjectQuery createAttributeQuery(ResourceType resourceType, QName attributeName, T attributeValue) throws SchemaException {
        RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
        RefinedObjectClassDefinition rAccountDef = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        RefinedAttributeDefinition attrDef = rAccountDef.findAttributeDefinition(attributeName);
        if (attrDef == null) {
            throw new SchemaException("No attribute '" + attributeName + "' in " + rAccountDef);
        }
        return prismContext.queryFor(ShadowType.class)
                .itemWithDef(attrDef, ShadowType.F_ATTRIBUTES, attrDef.getItemName()).eq(attributeValue)
                .and().item(ShadowType.F_OBJECT_CLASS).eq(rAccountDef.getObjectClassDefinition().getTypeName())
                .and().item(ShadowType.F_RESOURCE_REF).ref(resourceType.getOid())
                .build();
    }

    @Override
    public <F extends ObjectType> ModelContext<F> getModelContext() {
        return ModelExpressionThreadLocalHolder.getLensContext();
    }

    @Override
    public <F extends ObjectType> ModelElementContext<F> getFocusContext() {
        ModelContext<ObjectType> lensContext = ModelExpressionThreadLocalHolder.getLensContext();
        if (lensContext == null) {
            return null;
        }
        //noinspection unchecked
        return (ModelElementContext<F>) lensContext.getFocusContext();
    }

    @Override
    public ModelProjectionContext getProjectionContext() {
        return ModelExpressionThreadLocalHolder.getProjectionContext();
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition> Mapping<V,D> getMapping() {
        return ModelExpressionThreadLocalHolder.getMapping();
    }

    @Override
    public Task getCurrentTask() {
        Task rv = ModelExpressionThreadLocalHolder.getCurrentTask();
        if (rv == null) {
            // fallback (MID-4130): but maybe we should instead make sure ModelExpressionThreadLocalHolder is set up correctly
            ScriptExpressionEvaluationContext ctx = ScriptExpressionEvaluationContext.getThreadLocal();
            if (ctx != null) {
                rv = ctx.getTask();
            }
        }
        return rv;
    }

    @Override
    public OperationResult getCurrentResult() {
        // This is the most current operation result, reflecting e.g. the fact that mapping evaluation was started.
        ScriptExpressionEvaluationContext ctx = ScriptExpressionEvaluationContext.getThreadLocal();
        if (ctx != null) {
            return ctx.getResult();
        } else {
            // This is a bit older. But better than nothing.
            return ModelExpressionThreadLocalHolder.getCurrentResult();
        }
    }

    @Override
    public OperationResult getCurrentResult(String operationName) {
        OperationResult currentResult = getCurrentResult();
        if (currentResult != null) {
            return currentResult;
        } else {
            LOGGER.warn("No operation result for {}, creating a new one", operationName);
            return new OperationResult(operationName);
        }
    }

    // functions working with ModelContext

    @Override
    public ModelContext unwrapModelContext(LensContextType lensContextType)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return LensContext.fromLensContextType(lensContextType, prismContext, provisioningService, getCurrentTask(),
                getCurrentResult(MidpointFunctions.class.getName() + "getObject"));
    }

    @Override
    public LensContextType wrapModelContext(ModelContext<?> lensContext) throws SchemaException {
        return ((LensContext<?>) lensContext).toLensContextType();
    }

    // Convenience functions

    @Override
    public <T extends ObjectType> T createEmptyObject(Class<T> type) throws SchemaException {
        PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
        PrismObject<T> object = objectDefinition.instantiate();
        return object.asObjectable();
    }

    @Override
    public <T extends ObjectType> T createEmptyObjectWithName(Class<T> type, String name) throws SchemaException {
        T objectType = createEmptyObject(type);
        objectType.setName(new PolyStringType(name));
        return objectType;
    }

    @Override
    public <T extends ObjectType> T createEmptyObjectWithName(Class<T> type, PolyString name) throws SchemaException {
        T objectType = createEmptyObject(type);
        objectType.setName(new PolyStringType(name));
        return objectType;
    }

    @Override
    public <T extends ObjectType> T createEmptyObjectWithName(Class<T> type, PolyStringType name) throws SchemaException {
        T objectType = createEmptyObject(type);
        objectType.setName(name);
        return objectType;
    }

    // Functions accessing modelService

    @Override
    public <T extends ObjectType> T resolveReference(ObjectReferenceType reference)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return resolveReferenceInternal(reference, false);
    }

    private <T extends ObjectType> T resolveReferenceInternal(ObjectReferenceType reference, boolean allowNotFound)
            throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (reference == null) {
            return null;
        }
        QName type = reference.getType();           // TODO what about implicitly specified types, like in resourceRef?
        PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry()
                .findObjectDefinitionByType(reference.getType());
        if (objectDefinition == null) {
            throw new SchemaException("No definition for type " + type);
        }
        Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                .executionPhase()
                .allowNotFound(allowNotFound)
                .build();
        return modelService.getObject(objectDefinition.getCompileTimeClass(), reference.getOid(), options,
                getCurrentTask(), getCurrentResult())
                .asObjectable();
    }

    @Override
    public <T extends ObjectType> T resolveReferenceIfExists(ObjectReferenceType reference)
            throws SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        try {
            return resolveReferenceInternal(reference, true);
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    @Override
    public <T extends ObjectType> T getObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return modelService.getObject(type, oid, options, getCurrentTask(), getCurrentResult()).asObjectable();
    }

    @Override
    public <T extends ObjectType> T getObject(Class<T> type, String oid) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        PrismObject<T> prismObject = modelService.getObject(type, oid,
                getDefaultGetOptionCollection(),
                getCurrentTask(), getCurrentResult());
        return prismObject.asObjectable();
    }

    @Override
    public void executeChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas,
            ModelExecuteOptions options) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        modelService.executeChanges(deltas, options, getCurrentTask(), getCurrentResult());
    }

    @Override
    public void executeChanges(
            Collection<ObjectDelta<? extends ObjectType>> deltas)
            throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        modelService.executeChanges(deltas, null, getCurrentTask(), getCurrentResult());
    }

    @SafeVarargs
    @Override
    public final void executeChanges(ObjectDelta<? extends ObjectType>... deltas)
            throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(deltas);
        modelService.executeChanges(deltaCollection, null, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> String addObject(PrismObject<T> newObject,
            ModelExecuteOptions options) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        ObjectDelta<T> delta = DeltaFactory.Object.createAddDelta(newObject);
        Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(delta);
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedChanges = modelService.executeChanges(deltaCollection, options, getCurrentTask(), getCurrentResult());
        String oid = ObjectDeltaOperation.findAddDeltaOid(executedChanges, newObject);
        newObject.setOid(oid);
        return oid;
    }

    @Override
    public <T extends ObjectType> String addObject(PrismObject<T> newObject)
            throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        return addObject(newObject, null);
    }

    @Override
    public <T extends ObjectType> String addObject(T newObject,
            ModelExecuteOptions options) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        return addObject(newObject.asPrismObject(), options);
    }

    @Override
    public <T extends ObjectType> String addObject(T newObject)
            throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        return addObject(newObject.asPrismObject(), null);
    }

    @Override
    public <T extends ObjectType> void modifyObject(ObjectDelta<T> modifyDelta,
            ModelExecuteOptions options) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(modifyDelta);
        modelService.executeChanges(deltaCollection, options, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> void modifyObject(ObjectDelta<T> modifyDelta)
            throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(modifyDelta);
        modelService.executeChanges(deltaCollection, null, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> void deleteObject(Class<T> type, String oid,
            ModelExecuteOptions options) throws ObjectAlreadyExistsException,
            ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException,
            SecurityViolationException {
        ObjectDelta<T> deleteDelta = prismContext.deltaFactory().object().createDeleteDelta(type, oid);
        Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(deleteDelta);
        modelService.executeChanges(deltaCollection, options, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> void deleteObject(Class<T> type, String oid)
            throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException,
            PolicyViolationException, SecurityViolationException {
        ObjectDelta<T> deleteDelta = prismContext.deltaFactory().object().createDeleteDelta(type, oid);
        Collection<ObjectDelta<? extends ObjectType>> deltaCollection = MiscSchemaUtil.createCollection(deleteDelta);
        modelService.executeChanges(deltaCollection, null, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <F extends FocusType> void recompute(Class<F> type, String oid) throws SchemaException, PolicyViolationException,
            ExpressionEvaluationException, ObjectNotFoundException,
            ObjectAlreadyExistsException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        modelService.recompute(type, oid, null, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <F extends FocusType> PrismObject<F> searchShadowOwner(String accountOid)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ConfigurationException,
            ExpressionEvaluationException, CommunicationException {
        //noinspection unchecked
        return (PrismObject<F>) modelService.searchShadowOwner(accountOid, null, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> List<T> searchObjects(
            Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        return MiscSchemaUtil.toObjectableList(
                modelService.searchObjects(type, query, options, getCurrentTask(), getCurrentResult()));
    }

    @Override
    public <T extends ObjectType> List<T> searchObjects(
            Class<T> type, ObjectQuery query) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return MiscSchemaUtil.toObjectableList(
                modelService.searchObjects(type, query,
                        getDefaultGetOptionCollection(), getCurrentTask(), getCurrentResult()));
    }

    @Override
    public <T extends ObjectType> void searchObjectsIterative(Class<T> type,
            ObjectQuery query, ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        modelService.searchObjectsIterative(type, query, handler, options, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> void searchObjectsIterative(Class<T> type,
            ObjectQuery query, ResultHandler<T> handler)
            throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        modelService.searchObjectsIterative(type, query, handler,
                getDefaultGetOptionCollection(), getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> T searchObjectByName(Class<T> type, String name)
            throws SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SchemaException, ExpressionEvaluationException {
        ObjectQuery nameQuery = ObjectQueryUtil.createNameQuery(name, prismContext);
        List<PrismObject<T>> foundObjects = modelService
                .searchObjects(type, nameQuery,
                        getDefaultGetOptionCollection(), getCurrentTask(), getCurrentResult());
        if (foundObjects.isEmpty()) {
            return null;
        }
        if (foundObjects.size() > 1) {
            throw new IllegalStateException("More than one object found for type " + type + " and name '" + name + "'");
        }
        return foundObjects.iterator().next().asObjectable();
    }

    @Override
    public <T extends ObjectType> T searchObjectByName(Class<T> type, PolyString name)
            throws SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SchemaException, ExpressionEvaluationException {
        ObjectQuery nameQuery = ObjectQueryUtil.createNameQuery(name, prismContext);
        List<PrismObject<T>> foundObjects = modelService
                .searchObjects(type, nameQuery,
                        getDefaultGetOptionCollection(), getCurrentTask(), getCurrentResult());
        if (foundObjects.isEmpty()) {
            return null;
        }
        if (foundObjects.size() > 1) {
            throw new IllegalStateException("More than one object found for type " + type + " and name '" + name + "'");
        }
        return foundObjects.iterator().next().asObjectable();
    }

    @Override
    public <T extends ObjectType> T searchObjectByName(Class<T> type, PolyStringType name)
            throws SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SchemaException, ExpressionEvaluationException {
        ObjectQuery nameQuery = ObjectQueryUtil.createNameQuery(name, prismContext);
        List<PrismObject<T>> foundObjects = modelService
                .searchObjects(type, nameQuery,
                        getDefaultGetOptionCollection(), getCurrentTask(), getCurrentResult());
        if (foundObjects.isEmpty()) {
            return null;
        }
        if (foundObjects.size() > 1) {
            throw new IllegalStateException("More than one object found for type " + type + " and name '" + name + "'");
        }
        return foundObjects.iterator().next().asObjectable();
    }

    @Override
    public <T extends ObjectType> int countObjects(Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, ConfigurationException,
            CommunicationException, ExpressionEvaluationException {
        return modelService.countObjects(type, query, options, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <T extends ObjectType> int countObjects(Class<T> type,
            ObjectQuery query) throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, ConfigurationException,
            CommunicationException, ExpressionEvaluationException {
        return modelService.countObjects(type, query,
                getDefaultGetOptionCollection(), getCurrentTask(), getCurrentResult());
    }

    @Override
    public OperationResult testResource(String resourceOid)
            throws ObjectNotFoundException {
        return modelService.testResource(resourceOid, getCurrentTask());
    }

    @Override
    public ObjectDeltaType getResourceDelta(ModelContext context, String resourceOid) throws SchemaException {
        List<ObjectDelta<ShadowType>> deltas = new ArrayList<>();
        for (Object modelProjectionContextObject : context.getProjectionContexts()) {
            LensProjectionContext lensProjectionContext = (LensProjectionContext) modelProjectionContextObject;
            if (lensProjectionContext.getResourceShadowDiscriminator() != null &&
                    resourceOid.equals(lensProjectionContext.getResourceShadowDiscriminator().getResourceOid())) {
                deltas.add(lensProjectionContext.getDelta());   // union of primary and secondary deltas
            }
        }
        ObjectDelta<ShadowType> sum = ObjectDeltaCollectionsUtil.summarize(deltas);
        return DeltaConvertor.toObjectDeltaType(sum);
    }

    @Override
    public long getSequenceCounter(String sequenceOid) throws ObjectNotFoundException, SchemaException {
        return SequentialValueExpressionEvaluator.getSequenceCounter(sequenceOid, repositoryService, getCurrentResult());
    }

    // orgstruct related methods

    @Override
    public Collection<String> getManagersOids(UserType user)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagersOids(user, false);
    }

    @Override
    public Collection<String> getOrgUnits(UserType user, QName relation) {
        return orgStructFunctions.getOrgUnits(user, relation, false);
    }

    @Override
    public OrgType getParentOrgByOrgType(ObjectType object, String orgType) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgByOrgType(object, orgType, false);
    }

    @Override
    public OrgType getParentOrgByArchetype(ObjectType object, String archetypeOid) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgByArchetype(object, archetypeOid, false);
    }

    @Override
    public OrgType getOrgByOid(String oid) throws SchemaException {
        return orgStructFunctions.getOrgByOid(oid, false);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgs(object, false);
    }

    @Override
    public Collection<String> getOrgUnits(UserType user) {
        return orgStructFunctions.getOrgUnits(user, false);
    }

    @Override
    public Collection<UserType> getManagersOfOrg(String orgOid) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagersOfOrg(orgOid, false);
    }

    @Override
    public boolean isManagerOfOrgType(UserType user, String orgType) throws SchemaException {
        return orgStructFunctions.isManagerOfOrgType(user, orgType, false);
    }

    @Override
    public Collection<UserType> getManagers(UserType user) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagers(user, false);
    }

    @Override
    public Collection<UserType> getManagersByOrgType(UserType user, String orgType)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagersByOrgType(user, orgType, false);
    }

    @Override
    public boolean isManagerOf(UserType user, String orgOid) {
        return orgStructFunctions.isManagerOf(user, orgOid, false);
    }

    @Override
    public Collection<OrgType> getParentOrgsByRelation(ObjectType object, String relation)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgsByRelation(object, relation, false);
    }

    @Override
    public Collection<UserType> getManagers(UserType user, String orgType, boolean allowSelf)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagers(user, orgType, allowSelf, false);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, String relation, String orgType)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgs(object, relation, orgType, false);
    }

    @Override
    public Collection<String> getManagersOidsExceptUser(UserType user)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getManagersOidsExceptUser(user, false);
    }

    @Override
    public Collection<String> getManagersOidsExceptUser(@NotNull Collection<ObjectReferenceType> userRefList)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        return orgStructFunctions.getManagersOidsExceptUser(userRefList, false);
    }

    @Override
    public OrgType getOrgByName(String name) throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getOrgByName(name, false);
    }

    @Override
    public Collection<OrgType> getParentOrgsByRelation(ObjectType object, QName relation)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgsByRelation(object, relation, false);
    }

    @Override
    public Collection<OrgType> getParentOrgs(ObjectType object, QName relation, String orgType)
            throws SchemaException, SecurityViolationException {
        return orgStructFunctions.getParentOrgs(object, relation, orgType, false);
    }

    @Override
    public boolean isManager(UserType user) {
        return orgStructFunctions.isManager(user);
    }

    @Override
    public Protector getProtector() {
        return protector;
    }

    @Override
    public String getPlaintext(ProtectedStringType protectedStringType) throws EncryptionException {
        if (protectedStringType != null) {
            return protector.decryptString(protectedStringType);
        } else {
            return null;
        }
    }

    @Override
    public Map<String, String> parseXmlToMap(String xml) {
        Map<String, String> resultingMap = new HashMap<>();
        if (xml != null && !xml.isEmpty()) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            String value = "";
            String startName = "";
            InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            boolean isRootElement = true;
            try {
                XMLEventReader eventReader = factory.createXMLEventReader(stream);
                while (eventReader.hasNext()) {

                    XMLEvent event = eventReader.nextEvent();
                    int code = event.getEventType();
                    if (code == XMLStreamConstants.START_ELEMENT) {

                        StartElement startElement = event.asStartElement();
                        startName = startElement.getName().getLocalPart();
                        if (!isRootElement) {
                            resultingMap.put(startName, null);
                        } else {
                            isRootElement = false;
                        }
                    } else if (code == XMLStreamConstants.CHARACTERS) {
                        Characters characters = event.asCharacters();
                        if (!characters.isWhiteSpace()) {

                            StringBuilder valueBuilder;
                            if (value != null) {
                                valueBuilder = new StringBuilder(value).append(" ").append(characters.getData());
                            } else {
                                valueBuilder = new StringBuilder(characters.getData());
                            }
                            value = valueBuilder.toString();
                        }
                    } else if (code == XMLStreamConstants.END_ELEMENT) {

                        EndElement endElement = event.asEndElement();
                        String endName = endElement.getName().getLocalPart();

                        if (endName.equals(startName)) {
                            if (value != null) {
                                resultingMap.put(endName, value);
                                value = null;
                            }
                        } else {
                            LOGGER.trace("No value between xml tags, tag name : {}", endName);
                        }

                    } else if (code == XMLStreamConstants.END_DOCUMENT) {
                        isRootElement = true;
                    }
                }
            } catch (XMLStreamException e) {

                StringBuilder error = new StringBuilder("Xml stream exception wile parsing xml string")
                        .append(e.getLocalizedMessage());
                throw new SystemException(error.toString());
            }
        } else {
            LOGGER.trace("Input xml string null or empty.");
        }
        return resultingMap;
    }

    @Override
    public List<ObjectReferenceType> getMembersAsReferences(String orgOid) throws SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException {
        return getMembers(orgOid).stream()
                .map(obj -> createObjectRef(obj, prismContext))
                .collect(Collectors.toList());
    }

    @Override
    public List<UserType> getMembers(String orgOid) throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .isDirectChildOf(orgOid)
                .build();
        return searchObjects(UserType.class, query, null);
    }

    @Override
    public <F extends FocusType> String computeProjectionLifecycle(F focus, ShadowType shadow, ResourceType resource) {
        if (focus == null || shadow == null) {
            return null;
        }
        if (!(focus instanceof UserType)) {
            return null;
        }
        if (shadow.getKind() != null && shadow.getKind() != ShadowKindType.ACCOUNT) {
            return null;
        }
        ProtectedStringType focusPasswordPs = FocusTypeUtil.getPasswordValue((UserType) focus);
        if (focusPasswordPs != null && focusPasswordPs.canGetCleartext()) {
            return null;
        }
        CredentialsCapabilityType credentialsCapabilityType = ResourceTypeUtil
                .getEffectiveCapability(resource, CredentialsCapabilityType.class);
        if (credentialsCapabilityType == null) {
            return null;
        }
        PasswordCapabilityType passwordCapabilityType = credentialsCapabilityType.getPassword();
        if (passwordCapabilityType == null) {
            return null;
        }
        if (passwordCapabilityType.isEnabled() == Boolean.FALSE) {
            return null;
        }
        return SchemaConstants.LIFECYCLE_PROPOSED;
    }

    public MidPointPrincipal getPrincipal() throws SecurityViolationException {
        return securityContextManager.getPrincipal();
    }

    @Override
    public String getPrincipalOid() {
        return securityContextManager.getPrincipalOid();
    }

    @Override
    public String getChannel() {
        Task task = getCurrentTask();
        return task != null ? task.getChannel() : null;
    }

    @Override
    public WorkflowService getWorkflowService() {
        return workflowService;
    }

    @Override
    public List<ShadowType> getShadowsToActivate(Collection<? extends ModelElementContext> projectionContexts) {
        List<ShadowType> shadows = new ArrayList<>();

        //noinspection unchecked
        for (ModelElementContext<ShadowType> projectionCtx : projectionContexts) {

            List<? extends ObjectDeltaOperation> executedShadowDeltas = projectionCtx.getExecutedDeltas();
            //noinspection unchecked
            for (ObjectDeltaOperation<ShadowType> shadowDelta : executedShadowDeltas) {
                if (shadowDelta.getExecutionResult().getStatus() == OperationResultStatus.SUCCESS
                        && shadowDelta.getObjectDelta().getChangeType() == ChangeType.ADD) {
                    PrismObject<ShadowType> shadow = shadowDelta.getObjectDelta().getObjectToAdd();
                    PrismProperty<String> pLifecycleState = shadow.findProperty(ShadowType.F_LIFECYCLE_STATE);
                    if (pLifecycleState != null && !pLifecycleState.isEmpty() && SchemaConstants.LIFECYCLE_PROPOSED
                            .equals(pLifecycleState.getRealValue())) {
                        shadows.add(shadow.asObjectable());
                    }

                }
            }
        }
        return shadows;
    }

    @Override
    public String createRegistrationConfirmationLink(UserType userType) {
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(userType.asPrismObject());
        if (securityPolicy != null && securityPolicy.getAuthentication() != null
                && securityPolicy.getAuthentication().getSequence() != null && !securityPolicy.getAuthentication().getSequence().isEmpty()) {
            if (securityPolicy.getRegistration() != null && securityPolicy.getRegistration().getSelfRegistration() != null
            && securityPolicy.getRegistration().getSelfRegistration().getAdditionalAuthenticationName() != null) {
                String resetPasswordSequenceName = securityPolicy.getRegistration().getSelfRegistration().getAdditionalAuthenticationName();
                String prefix = createPrefixLinkByAuthSequence(SchemaConstants.CHANNEL_GUI_SELF_REGISTRATION_URI, resetPasswordSequenceName, securityPolicy.getAuthentication().getSequence());
                if (prefix != null) {
                    return createTokenConfirmationLink(prefix, userType);
                }
            }
        }
        return createTokenConfirmationLink(SchemaConstants.REGISTRATION_CONFIRAMTION_PREFIX, userType);
    }

    @Override
    public String createPasswordResetLink(UserType userType) {
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(userType.asPrismObject());
        if (securityPolicy != null && securityPolicy.getAuthentication() != null
                && securityPolicy.getAuthentication().getSequence() != null && !securityPolicy.getAuthentication().getSequence().isEmpty()) {
            if (securityPolicy.getCredentialsReset() != null && securityPolicy.getCredentialsReset().getAuthenticationSequenceName() != null) {
                String resetPasswordSequenceName = securityPolicy.getCredentialsReset().getAuthenticationSequenceName();
                String prefix = createPrefixLinkByAuthSequence(SchemaConstants.CHANNEL_GUI_RESET_PASSWORD_URI, resetPasswordSequenceName, securityPolicy.getAuthentication().getSequence());
                if (prefix != null) {
                    return createTokenConfirmationLink(prefix, userType);
                }
            }
        }
        return createTokenConfirmationLink(SchemaConstants.PASSWORD_RESET_CONFIRMATION_PREFIX, userType);
    }

    @Override
    public String createAccountActivationLink(UserType userType) {
        return createBaseConfirmationLink(SchemaConstants.ACCOUNT_ACTIVATION_PREFIX, userType.getOid());
    }

    private String createBaseConfirmationLink(String prefix, UserType userType) {
        return getPublicHttpUrlPattern() + prefix + "?" + SchemaConstants.USER_ID + "=" + userType.getName().getOrig();
    }

    private String createBaseConfirmationLink(String prefix, String oid) {
        return getPublicHttpUrlPattern() + prefix + "?" + SchemaConstants.USER_ID + "=" + oid;
    }

    private String createTokenConfirmationLink(String prefix, UserType userType) {
        return createBaseConfirmationLink(prefix, userType) + "&" + SchemaConstants.TOKEN + "=" + getNonce(userType);
    }

    private String createPrefixLinkByAuthSequence(String channel, String nameOfSequence, Collection<AuthenticationSequenceType> sequences){
                AuthenticationSequenceType sequenceByName = null;
                AuthenticationSequenceType defaultSequence = null;
                for (AuthenticationSequenceType sequenceType : sequences) {
                    if (sequenceType.getName().equals(nameOfSequence)) {
                        sequenceByName = sequenceType;
                        break;
                    } else if (sequenceType.getChannel().getChannelId().equals(channel)
                            && Boolean.TRUE.equals(sequenceType.getChannel().isDefault())) {
                        defaultSequence = sequenceType;
                    }
                }
                AuthenticationSequenceType usedSequence = sequenceByName != null ? sequenceByName : defaultSequence;
                if (usedSequence != null) {
                    String sequecnceSuffix = usedSequence.getChannel().getUrlSuffix();
                    String prefix = (sequecnceSuffix.startsWith("/")) ? sequecnceSuffix : ("/" + sequecnceSuffix);
                    return SchemaConstants.AUTH_MODULE_PREFIX + prefix;
                }
        return null;
    }

    private SecurityPolicyType resolveSecurityPolicy(PrismObject<UserType> user) {
        return securityContextManager.runPrivileged(new Producer<SecurityPolicyType>() {
            private static final long serialVersionUID = 1L;

            @Override
            public SecurityPolicyType run() {

                Task task = taskManager.createTaskInstance("load security policy");

                Task currentTask = getCurrentTask();
                task.setChannel(currentTask != null ? currentTask.getChannel() : null);

                OperationResult result = new OperationResult("load security policy");

                try {
                    return modelInteractionService.getSecurityPolicy(user, task, result);
                } catch (CommonException e) {
                    LOGGER.error("Could not retrieve security policy: {}", e.getMessage(), e);
                    return null;
                }

            }

        });
    }

    private String getPublicHttpUrlPattern() {
        SystemConfigurationType systemConfiguration;
        try {
            systemConfiguration = modelInteractionService.getSystemConfiguration(getCurrentResult());
        } catch (ObjectNotFoundException | SchemaException e) {
            LOGGER.error("Error while getting system configuration. ", e);
            return null;
        }
        if (systemConfiguration == null) {
            LOGGER.trace("No system configuration defined. Skipping link generation.");
            return null;
        }
        String host = null;
        HttpConnectionInformation connectionInf = SecurityUtil.getCurrentConnectionInformation();
        if (connectionInf != null) {
            host = connectionInf.getServerName();
        }
        String publicHttpUrlPattern = SystemConfigurationTypeUtil.getPublicHttpUrlPattern(systemConfiguration, host);
        if (StringUtils.isBlank(publicHttpUrlPattern)) {
            LOGGER.error("No patern defined. It can break link generation.");
        }

        return publicHttpUrlPattern;

    }

    private String getNonce(UserType user) {
        if (user.getCredentials() == null) {
            return null;
        }

        if (user.getCredentials().getNonce() == null) {
            return null;
        }

        if (user.getCredentials().getNonce().getValue() == null) {
            return null;
        }

        try {
            return getPlaintext(user.getCredentials().getNonce().getValue());
        } catch (EncryptionException e) {
            return null;
        }
    }

    @Override
    public String getConst(String name) {
        return constantsManager.getConstantValue(name);
    }

    @Override
    public ShadowType resolveEntitlement(ShadowAssociationType shadowAssociationType) {
        ObjectReferenceType shadowRef = shadowAssociationType.getShadowRef();
        if (shadowRef == null) {
            LOGGER.trace("No shadowRef in association {}", shadowAssociationType);
            return null;
        }
        if (shadowRef.asReferenceValue().getObject() != null){
            return (ShadowType) shadowAssociationType.getShadowRef().asReferenceValue().getObject().asObjectable();
        }

        LensProjectionContext projectionCtx = (LensProjectionContext) getProjectionContext();
        if (projectionCtx == null) {
            LOGGER.trace("No projection found. Skipping resolving entitlement");
            return null;
        }

        Map<String, PrismObject<ShadowType>> entitlementMap = projectionCtx.getEntitlementMap();
        if (entitlementMap == null) {
            LOGGER.trace("No entitlement map present in projection context {}", projectionCtx);
            return null;
        }

        PrismObject<ShadowType> entitlement = entitlementMap.get(shadowRef.getOid());
        if (entitlement == null) {
            LOGGER.trace("No entitlement with oid {} found among resolved entitlement {}.", shadowRef, entitlementMap);
            return null;
        }
        LOGGER.trace("Returning resolved entitlement: {}", entitlement);
        return entitlement.asObjectable();

    }

    @Override
    public ExtensionType collectExtensions(AssignmentPathType path, int startAt)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return AssignmentPath.collectExtensions(path, startAt, modelService, getCurrentTask(), getCurrentResult());
    }
    @Override
    public TaskType executeChangesAsynchronously(Collection<ObjectDelta<?>> deltas, ModelExecuteOptions options,
            String templateTaskOid) throws SecurityViolationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        return executeChangesAsynchronously(deltas, options, templateTaskOid, getCurrentTask(), getCurrentResult());
    }

    @Override
    public TaskType executeChangesAsynchronously(Collection<ObjectDelta<?>> deltas, ModelExecuteOptions options,
            String templateTaskOid, Task opTask, OperationResult result) throws SecurityViolationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        MidPointPrincipal principal = securityContextManager.getPrincipal();
        if (principal == null) {
            throw new SecurityViolationException("No current user");
        }
        TaskType newTask;
        if (templateTaskOid != null) {
            newTask = modelService.getObject(TaskType.class, templateTaskOid,
                    getDefaultGetOptionCollection(), opTask, result).asObjectable();
        } else {
            newTask = new TaskType(prismContext);
            newTask.setName(PolyStringType.fromOrig("Execute changes"));
            newTask.setRecurrence(TaskRecurrenceType.SINGLE);
        }
        newTask.setName(PolyStringType.fromOrig(newTask.getName().getOrig() + " " + (int) (Math.random()*10000)));
        newTask.setOid(null);
        newTask.setTaskIdentifier(null);
        newTask.setOwnerRef(createObjectRef(principal.getFocus(), prismContext));
        newTask.setExecutionStatus(RUNNABLE);
        newTask.setHandlerUri(ModelPublicConstants.EXECUTE_DELTAS_TASK_HANDLER_URI);
        if (deltas.isEmpty()) {
            throw new IllegalArgumentException("No deltas to execute");
        }
        List<ObjectDeltaType> deltasBeans = new ArrayList<>();
        for (ObjectDelta<?> delta : deltas) {
            //noinspection unchecked
            deltasBeans.add(DeltaConvertor.toObjectDeltaType((ObjectDelta<? extends com.evolveum.prism.xml.ns._public.types_3.ObjectType>) delta));
        }
        //noinspection unchecked
        PrismPropertyDefinition<ObjectDeltaType> deltasDefinition = prismContext.getSchemaRegistry()
                .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTAS);
        PrismProperty<ObjectDeltaType> deltasProperty = deltasDefinition.instantiate();
        deltasProperty.setRealValues(deltasBeans.toArray(new ObjectDeltaType[0]));
        newTask.asPrismObject().addExtensionItem(deltasProperty);
        if (options != null) {
            //noinspection unchecked
            PrismPropertyDefinition<ModelExecuteOptionsType> optionsDefinition = prismContext.getSchemaRegistry()
                    .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_EXECUTE_OPTIONS);
            PrismProperty<ModelExecuteOptionsType> optionsProperty = optionsDefinition.instantiate();
            optionsProperty.setRealValue(options.toModelExecutionOptionsType());
            newTask.asPrismObject().addExtensionItem(optionsProperty);
        }
        ObjectDelta<TaskType> taskAddDelta = DeltaFactory.Object.createAddDelta(newTask.asPrismObject());
        Collection<ObjectDeltaOperation<? extends ObjectType>> operations = modelService
                .executeChanges(singleton(taskAddDelta), null, opTask, result);
        return (TaskType) operations.iterator().next().getObjectDelta().getObjectToAdd().asObjectable();
    }

    @Override
    public TaskType submitTaskFromTemplate(String templateTaskOid, List<Item<?, ?>> extensionItems)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        return modelInteractionService.submitTaskFromTemplate(templateTaskOid, extensionItems, getCurrentTask(), getCurrentResult());
    }

    @Override
    public TaskType submitTaskFromTemplate(String templateTaskOid, Map<QName, Object> extensionValues)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        return modelInteractionService.submitTaskFromTemplate(templateTaskOid, extensionValues, getCurrentTask(), getCurrentResult());
    }

    @Override
    public String translate(LocalizableMessage message) {
        return localizationService.translate(message, Locale.getDefault());
    }

    @Override
    public String translate(LocalizableMessageType message) {
        return localizationService.translate(LocalizationUtil.toLocalizableMessage(message), Locale.getDefault());
    }

    @Override
    public Object executeAdHocProvisioningScript(ResourceType resource, String language, String code)
            throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException, ObjectAlreadyExistsException {
        return executeAdHocProvisioningScript(resource.getOid(), language, code);
    }

    @Override
    public Object executeAdHocProvisioningScript(String resourceOid, String language, String code)
                    throws SchemaException, ObjectNotFoundException,
                    ExpressionEvaluationException, CommunicationException, ConfigurationException,
                    SecurityViolationException, ObjectAlreadyExistsException {
        OperationProvisioningScriptType script = new OperationProvisioningScriptType();
        script.setCode(code);
        script.setLanguage(language);
        script.setHost(ProvisioningScriptHostType.RESOURCE);

        return provisioningService.executeScript(resourceOid, script, getCurrentTask(), getCurrentResult());
    }

    @Override
    public Boolean isEvaluateNew() {
        ScriptExpressionEvaluationContext scriptContext = ScriptExpressionEvaluationContext.getThreadLocal();
        if (scriptContext == null) {
            return null;
        }
        return scriptContext.isEvaluateNew();
    }

    @Override
    @NotNull
    public Collection<PrismValue> collectAssignedFocusMappingsResults(@NotNull ItemPath path) throws SchemaException {
        ModelContext<ObjectType> lensContext = ModelExpressionThreadLocalHolder.getLensContext();
        if (lensContext == null) {
            throw new IllegalStateException("No lensContext");
        }
        DeltaSetTriple<? extends EvaluatedAssignment<?>> evaluatedAssignmentTriple = lensContext.getEvaluatedAssignmentTriple();
        if (evaluatedAssignmentTriple == null) {
            return emptySet();
        }
        Collection<PrismValue> rv = new HashSet<>();
        for (EvaluatedAssignment<?> evaluatedAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) {
            if (evaluatedAssignment.isValid()) {
                for (Mapping<?, ?> mapping : evaluatedAssignment.getFocusMappings()) {
                    if (path.equivalent(mapping.getOutputPath())) {
                        PrismValueDeltaSetTriple<?> outputTriple = mapping.getOutputTriple();
                        if (outputTriple != null) {
                            rv.addAll(outputTriple.getNonNegativeValues());
                        }
                    }
                }
            }
        }
        // Ugly hack - MID-4452 - When having an assignment giving focusMapping, and the assignment is being deleted, the
        // focus mapping is evaluated in wave 0 (results correctly being pushed to the minus set), but also in wave 1.
        // The results are sent to zero set; and they are not applied only because they are already part of a priori delta.
        // This causes problems here.
        //
        // Until MID-4452 is fixed, here we manually delete the values from the result.
        ModelElementContext<ObjectType> focusContext = lensContext.getFocusContext();
        if (focusContext != null) {
            ObjectDelta<ObjectType> delta = focusContext.getDelta();
            if (delta != null) {
                ItemDelta<PrismValue, ItemDefinition> targetItemDelta = delta.findItemDelta(path);
                if (targetItemDelta != null) {
                    rv.removeAll(emptyIfNull(targetItemDelta.getValuesToDelete()));
                }
            }
        }
        return rv;
    }

    private Collection<SelectorOptions<GetOperationOptions>> getDefaultGetOptionCollection() {
        return SelectorOptions.createCollection(GetOperationOptions.createExecutionPhase());
    }

    @Override
    public <F extends FocusType> List<F> getFocusesByCorrelationRule(Class<F> type, String resourceOid, ShadowKindType kind, String intent, ShadowType shadow) {
        ResourceType resource;
        try {
            resource = getObject(ResourceType.class, resourceOid, GetOperationOptions.createNoFetchCollection());
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException
                | SecurityViolationException | ExpressionEvaluationException e) {
            LOGGER.error("Cannot get resource, reason: {}", e.getMessage(), e);
            return null;
        }
        SynchronizationType synchronization = resource.getSynchronization();
        if (synchronization == null) {
            return null;
        }

        ObjectSynchronizationDiscriminatorType discriminator = new ObjectSynchronizationDiscriminatorType();
        discriminator.setKind(kind);
        discriminator.setIntent(intent);

        SynchronizationContext<F> syncCtx = new SynchronizationContext<>(shadow.asPrismObject(), shadow.asPrismObject(),
                null, resource.asPrismObject(), getCurrentTask().getChannel(), getPrismContext(), expressionFactory, getCurrentTask());

        ObjectSynchronizationType applicablePolicy = null;

        OperationResult result = getCurrentResult();

        try {

            SystemConfigurationType systemConfiguration = modelInteractionService.getSystemConfiguration(result);
            syncCtx.setSystemConfiguration(systemConfiguration.asPrismObject());

            for (ObjectSynchronizationType objectSync : synchronization.getObjectSynchronization()) {

                if (SynchronizationServiceUtils.isPolicyApplicable(objectSync, discriminator, expressionFactory, syncCtx, result)) {
                    applicablePolicy = objectSync;
                    break;
                }
            }

            if (applicablePolicy == null) {
                return null;
            }

            List<PrismObject<F>> correlatedFocuses = correlationConfirmationEvaluator.findFocusesByCorrelationRule(type, shadow, applicablePolicy.getCorrelation(), resource, systemConfiguration, syncCtx.getTask(), result);
            return MiscSchemaUtil.toObjectableList(correlatedFocuses);

        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
                | ConfigurationException | SecurityViolationException e) {
            LOGGER.error("Cannot find applicable policy for kind={}, intent={}. Reason: {}", kind, intent, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public <F extends ObjectType> ModelContext<F> previewChanges(Collection<ObjectDelta<? extends ObjectType>> deltas,
            ModelExecuteOptions options)
            throws CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SchemaException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
        return modelInteractionService.previewChanges(deltas, options, getCurrentTask(), getCurrentResult());
    }

    @Override
    public <O extends ObjectType> void applyDefinition(O object)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        if (object instanceof ShadowType || object instanceof ResourceType) {
            provisioningService.applyDefinition(object.asPrismObject(), getCurrentTask(), getCurrentResult());
        }
    }

    @Override
    public <C extends Containerable> S_ItemEntry deltaFor(Class<C> objectClass) throws SchemaException {
        return prismContext.deltaFor(objectClass);
    }

    // MID-5243
    @Override
    public <O extends ObjectType> boolean hasArchetype(O object, String archetypeOid) {
        if (object == null) {
            return false;
        }
        if (!(object instanceof AssignmentHolderType)) {
            return archetypeOid == null;
        }

        ModelContext<O> lensContext = ModelExpressionThreadLocalHolder.getLensContext();
        if (lensContext != null) {
            ModelElementContext<O> focusContext = lensContext.getFocusContext();
            ArchetypeType archetypeType = focusContext.getArchetype();
            if (archetypeType != null) {
                return archetypeType.getOid().equals(archetypeOid);
            }
        }

        List<ObjectReferenceType> archetypeRefs = ((AssignmentHolderType)object).getArchetypeRef();
        if (archetypeOid == null) {
            return archetypeRefs.isEmpty();
        }
        for (ObjectReferenceType archetypeRef : archetypeRefs) {
            if (archetypeOid.equals(archetypeRef.getOid())) {
                return true;
            }
        }
        return false;
    }

    // MID-5243
    @Override
    public <O extends ObjectType> ArchetypeType getArchetype(O object) throws SchemaException, ConfigurationException {
        if (!(object instanceof AssignmentHolderType)) {
            return null;
        }
        //noinspection unchecked
        PrismObject<ArchetypeType> archetype = archetypeManager.determineArchetype((PrismObject<? extends AssignmentHolderType>) object.asPrismObject(), getCurrentResult());
        if (archetype == null) {
            return null;
        }
        return archetype.asObjectable();
    }

    // MID-5243
    @Override
    public <O extends ObjectType> String getArchetypeOid(O object) throws SchemaException, ConfigurationException {
        if (!(object instanceof AssignmentHolderType)) {
            return null;
        }
        //noinspection unchecked
        ObjectReferenceType archetypeRef = archetypeManager.determineArchetypeRef((PrismObject<? extends AssignmentHolderType>) object.asPrismObject(), getCurrentResult());
        if (archetypeRef == null) {
            return null;
        }
        return archetypeRef.getOid();
    }

    // temporary
    public MessageWrapper wrap(AsyncUpdateMessageType message) {
        return new MessageWrapper(message, prismContext);
    }

    // temporary
    @SuppressWarnings("unused")
    public Map<String, Object> getMessageBodyAsMap(AsyncUpdateMessageType message) throws IOException {
        return wrap(message).getBodyAsMap();
    }

    // temporary
    @SuppressWarnings("unused")
    public Item<?, ?> getMessageBodyAsPrismItem(AsyncUpdateMessageType message) throws SchemaException {
        return wrap(message).getBodyAsPrismItem(PrismContext.LANG_XML);
    }

    @Override
    public <O extends ObjectType> void addRecomputeTrigger(O object, Long timestamp) throws ObjectAlreadyExistsException,
            SchemaException, ObjectNotFoundException {
        addRecomputeTrigger(object.asPrismObject(), timestamp);
    }

    @Override
    public <O extends ObjectType> void addRecomputeTrigger(PrismObject<O> object, Long timestamp)
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        TriggerType trigger = new TriggerType(prismContext)
                .handlerUri(RecomputeTriggerHandler.HANDLER_URI)
                .timestamp(XmlTypeConverter.createXMLGregorianCalendar(timestamp != null ? timestamp : System.currentTimeMillis()));
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(object.asObjectable().getClass())
                .item(ObjectType.F_TRIGGER).add(trigger)
                .asItemDeltas();
        repositoryService.modifyObject(object.getCompileTimeClass(), object.getOid(), itemDeltas,
                getCurrentResult(CLASS_DOT + "addRecomputeTrigger"));
    }

    @Override
    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    @NotNull
    @Override
    public OptimizingTriggerCreator getOptimizingTriggerCreator(long fireAfter, long safetyMargin) {
        return new OptimizingTriggerCreatorImpl(triggerCreatorGlobalState, this, fireAfter, safetyMargin);
    }

    @NotNull
    @Override
    public <T> ResourceAttributeDefinition<T> getAttributeDefinition(PrismObject<ResourceType> resource, QName objectClassName,
            QName attributeName) throws SchemaException {
        ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
        if (resourceSchema == null) {
            throw new SchemaException("No resource schema in " + resource);
        }
        ObjectClassComplexTypeDefinition ocDef = resourceSchema.findObjectClassDefinition(objectClassName);
        if (ocDef == null) {
            throw new SchemaException("No definition of object class " + objectClassName + " in " + resource);
        }
        ResourceAttributeDefinition<T> attrDef = ocDef.findAttributeDefinition(attributeName);
        if (attrDef == null) {
            throw new SchemaException("No definition of attribute " + attributeName + " in object class " + objectClassName
                    + " in " + resource);
        }
        return attrDef;
    }

    @NotNull
    @Override
    public <T> ResourceAttributeDefinition<T> getAttributeDefinition(PrismObject<ResourceType> resource, String objectClassName,
            String attributeName) throws SchemaException {
        return getAttributeDefinition(resource, new QName(objectClassName), new QName(attributeName));
    }
}
