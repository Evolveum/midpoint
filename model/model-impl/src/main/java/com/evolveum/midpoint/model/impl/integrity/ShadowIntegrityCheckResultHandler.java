/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author mederly
 */
public class ShadowIntegrityCheckResultHandler extends AbstractSearchIterativeResultHandler<ShadowType> {

    static final Trace LOGGER = TraceManager.getTrace(ShadowIntegrityCheckResultHandler.class);

    private static final String CLASS_DOT = ShadowIntegrityCheckResultHandler.class.getName() + ".";

    private PrismContext prismContext;
    private ProvisioningService provisioningService;
    private MatchingRuleRegistry matchingRuleRegistry;
    private RepositoryService repositoryService;

    // derived from task extension diagnose/fix values at instantiation
    private boolean checkIntents;
    private boolean checkUniqueness;
    private boolean checkNormalization;
    private boolean fixIntents;
    private boolean fixUniqueness;
    private boolean fixNormalization;

    public static final String INTENTS = "intents";
    public static final String UNIQUENESS = "uniqueness";
    public static final String NORMALIZATION = "normalization";
    public static final List<String> KNOWN_KEYS = Arrays.asList(INTENTS, UNIQUENESS, NORMALIZATION);

    // resource oid + kind -> ROCD
    // we silently assume that all intents for a given kind share a common attribute definition
    private Map<Pair<String,ShadowKindType>, ObjectTypeContext> contextMap = new HashMap<>();

    private Map<String,PrismObject<ResourceType>> resources = new HashMap<>();

    private Statistics statistics = new Statistics();

    public ShadowIntegrityCheckResultHandler(Task coordinatorTask, String taskOperationPrefix, String processShortName,
                                             String contextDesc, TaskManager taskManager, PrismContext prismContext,
                                             ProvisioningService provisioningService, MatchingRuleRegistry matchingRuleRegistry,
                                             RepositoryService repositoryService) {
        super(coordinatorTask, taskOperationPrefix, processShortName, contextDesc, taskManager);
        this.prismContext = prismContext;
        this.provisioningService = provisioningService;
        this.matchingRuleRegistry = matchingRuleRegistry;
        this.repositoryService = repositoryService;
        setStopOnError(false);
        setLogErrors(false);            // we do log errors ourselves

        Integer tasks = getWorkerThreadsCount(coordinatorTask);
        if (tasks != null && tasks != 0) {
            throw new UnsupportedOperationException("Unsupported number of worker threads: " + tasks + ". This task cannot be run with worker threads. Please remove workerThreads extension property or set its value to 0.");
        }

        PrismProperty<String> diagnosePrismProperty = coordinatorTask.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_DIAGNOSE);
        if (diagnosePrismProperty == null || diagnosePrismProperty.isEmpty()) {
            checkIntents = true;
            checkUniqueness = true;
            checkNormalization = true;
        } else {
            checkIntents = contains(diagnosePrismProperty, INTENTS);
            checkUniqueness = contains(diagnosePrismProperty, UNIQUENESS);
            checkNormalization = contains(diagnosePrismProperty, NORMALIZATION);
            checkProperty(diagnosePrismProperty);
        }
        PrismProperty<String> fixPrismProperty = coordinatorTask.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_FIX);
        if (fixPrismProperty == null || fixPrismProperty.isEmpty()) {
            fixIntents = false;
            fixUniqueness = false;
            fixNormalization = false;
        } else {
            fixIntents = contains(fixPrismProperty, INTENTS);
            fixUniqueness = contains(fixPrismProperty, UNIQUENESS);
            fixNormalization = contains(fixPrismProperty, NORMALIZATION);
            checkProperty(fixPrismProperty);
        }

    }

    private void checkProperty(PrismProperty<String> property) {
        for (PrismPropertyValue<String> value : property.getValues()) {
            if (!KNOWN_KEYS.contains(value.getValue())) {
                throw new IllegalArgumentException("Unknown diagnose/fix keyword: " + value.getValue() + ". Known keys are: " + KNOWN_KEYS);
            }
        }
    }

    private boolean contains(PrismProperty<String> property, String keyword) {
        return property.containsRealValue(new PrismPropertyValue<String>(keyword));
    }

    @Override
    protected boolean handleObject(PrismObject<ShadowType> shadow, Task workerTask, OperationResult parentResult) throws CommonException {
        OperationResult result = parentResult.createMinorSubresult(CLASS_DOT + "handleObject");
        ShadowCheckResult checkResult = new ShadowCheckResult(shadow);
        try {
            checkShadow(checkResult, shadow, workerTask, result);
            for (Exception e : checkResult.getErrors()) {
                result.createSubresult(CLASS_DOT + "handleObject.result").recordPartialError(e.getMessage(), e);
            }
            for (String message : checkResult.getWarnings()) {
                result.createSubresult(CLASS_DOT + "handleObject.result").recordWarning(message);
            }
            if (!checkResult.getErrors().isEmpty()) {
                statistics.incrementShadowsWithErrors();
            } else if (!checkResult.getWarnings().isEmpty()) {
                statistics.incrementShadowsWithWarnings();
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Checking shadow {} (resource {}) finished - errors: {}, warnings: {}",
                        ObjectTypeUtil.toShortString(checkResult.getShadow()),
                        ObjectTypeUtil.toShortString(checkResult.getResource()),
                        checkResult.getErrors().size(), checkResult.getWarnings().size());
            }
        } catch (RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unexpected error while checking shadow {} integrity", e, ObjectTypeUtil.toShortString(shadow));
            result.recordPartialError("Unexpected error while checking shadow integrity", e);
            statistics.incrementShadowsWithErrors();
        }

        statistics.registerProblemCodeOccurrences(checkResult.getProblemCodes());
        if (checkResult.isFixApplied()) {
            statistics.registerProblemsFixes(checkResult.getFixForProblems());
        }

        result.computeStatusIfUnknown();
        return true;
    }

    private void checkShadow(ShadowCheckResult checkResult, PrismObject<ShadowType> shadow, Task workerTask, OperationResult result) throws SchemaException {
        ShadowType shadowType = shadow.asObjectable();
        ObjectReferenceType resourceRef = shadowType.getResourceRef();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Checking shadow {} (resource {})", ObjectTypeUtil.toShortString(shadowType), resourceRef!=null?resourceRef.getOid():"(null)");
        }
        statistics.incrementShadows();

        if (resourceRef == null) {
            checkResult.recordError(Statistics.NO_RESOURCE_OID, new SchemaException("No resourceRef"));
            return;
        }
        String resourceOid = resourceRef.getOid();
        if (resourceOid == null) {
            checkResult.recordError(Statistics.NO_RESOURCE_OID, new SchemaException("Null resource OID"));
            return;
        }
        PrismObject<ResourceType> resource = resources.get(resourceOid);
        if (resource == null) {
            statistics.incrementResources();
            try {
                resource = provisioningService.getObject(ResourceType.class, resourceOid, null, workerTask, result);
            } catch (ObjectNotFoundException e) {
                checkResult.recordError(Statistics.CANNOT_GET_RESOURCE, new ObjectNotFoundException("Resource object does not exist: " + e.getMessage(), e));
                return;
            } catch (SchemaException e) {
                checkResult.recordError(Statistics.CANNOT_GET_RESOURCE, new ObjectNotFoundException("Resource object has schema problems: " + e.getMessage(), e));
                return;
            } catch (CommonException|RuntimeException e) {
                checkResult.recordError(Statistics.CANNOT_GET_RESOURCE, new ObjectNotFoundException("Resource object cannot be fetched for some reason: " + e.getMessage(), e));
                return;
            }
            resources.put(resourceOid, resource);
        }
        checkResult.setResource(resource);

        ShadowKindType kind = shadowType.getKind();
        if (kind == null) {
            // TODO or simply assume account?
            checkResult.recordError(Statistics.NO_KIND_SPECIFIED, new SchemaException("No kind specified"));
            return;
        }

        String intent = shadowType.getIntent();
        if (checkIntents && intent == null || intent.isEmpty()) {
            checkResult.recordWarning(Statistics.NO_INTENT_SPECIFIED, "None or empty intent");
        }

        Pair<String,ShadowKindType> key = new ImmutablePair<>(resourceOid, kind);
        ObjectTypeContext context = contextMap.get(key);
        if (context == null) {
            context = new ObjectTypeContext();
            context.setResource(resource);
            RefinedResourceSchema resourceSchema;
            try {
                resourceSchema = RefinedResourceSchema.getRefinedSchema(context.getResource(), LayerType.MODEL, prismContext);
            } catch (SchemaException e) {
                checkResult.recordError(Statistics.CANNOT_GET_REFINED_SCHEMA, new SchemaException("Couldn't derive resource schema: " + e.getMessage(), e));
                return;
            }
            if (resourceSchema == null) {
                checkResult.recordError(Statistics.NO_RESOURCE_REFINED_SCHEMA, new SchemaException("No resource schema"));
                return;
            }
            context.setObjectClassDefinition(resourceSchema.getRefinedDefinition(kind, shadowType));
            if (context.getObjectClassDefinition() == null) {
                // TODO or warning only?
                checkResult.recordError(Statistics.NO_OBJECT_CLASS_REFINED_SCHEMA, new SchemaException("No refined object class definition for kind=" + kind + ", intent=" + intent));
                return;
            }
            contextMap.put(key, context);
        }

        try {
            provisioningService.applyDefinition(shadow, result);
        } catch (SchemaException|ObjectNotFoundException|CommunicationException|ConfigurationException e) {
            checkResult.recordError(Statistics.OTHER_FAILURE, new SystemException("Couldn't apply definition to shadow from repo", e));
            return;
        }

        Set<RefinedAttributeDefinition<?>> identifiers = new HashSet<>();
        identifiers.addAll(context.getObjectClassDefinition().getIdentifiers());
        identifiers.addAll(context.getObjectClassDefinition().getSecondaryIdentifiers());

        PrismContainer<ShadowAttributesType> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null) {
            // might happen on unfinished shadows?
            checkResult.recordError(Statistics.OTHER_FAILURE, new SchemaException("No attributes container"));
            return;
        }

        for (RefinedAttributeDefinition<?> identifier : identifiers) {
            PrismProperty property = attributesContainer.getValue().findProperty(identifier.getName());
            if (property == null || property.size() == 0) {
                checkResult.recordWarning(Statistics.OTHER_FAILURE, "No value for identifier " + identifier.getName());
                continue;
            }
            if (property.size() > 1) {
                // we don't expect multi-valued identifiers
                checkResult.recordError(Statistics.OTHER_FAILURE, new SchemaException("Multi-valued identifier " + identifier.getName() + " with values " + property.getValues()));
                continue;
            }
            // size == 1
            String value = (String) property.getValue().getValue();
            if (value == null) {
                checkResult.recordWarning(Statistics.OTHER_FAILURE, "Null value for identifier " + identifier.getName());
                continue;
            }
            if (checkUniqueness) {
                addIdentifierValue(checkResult, context, identifier.getName(), value, shadow);
            }
            if (checkNormalization) {
                doCheckNormalization(checkResult, identifier, value, context);
            }
        }

        if (checkResult.getFixDeltas().size() > 0) {
            try {
                applyFix(checkResult, shadow, workerTask, result);
                checkResult.setFixApplied(true);
            } catch (CommonException e) {
                checkResult.recordError(Statistics.CANNOT_APPLY_FIX, new SystemException("Couldn't apply the shadow fix", e));
                return;
            }
        }
    }

    private void applyFix(ShadowCheckResult checkResult, PrismObject<ShadowType> shadow, Task workerTask, OperationResult result) throws CommonException {
        LOGGER.info("Applying shadow fix:\n{}", DebugUtil.debugDump(checkResult.getFixDeltas()));
        repositoryService.modifyObject(ShadowType.class, shadow.getOid(), checkResult.getFixDeltas(), result);
    }

    private void doCheckNormalization(ShadowCheckResult checkResult, RefinedAttributeDefinition<?> identifier, String value, ObjectTypeContext context) throws SchemaException {
        QName matchingRuleQName = identifier.getMatchingRuleQName();
        if (matchingRuleQName == null) {
            return;
        }

        MatchingRule<Object> matchingRule;
        try {
            matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, identifier.getTypeName());
        } catch (SchemaException e) {
            checkResult.recordError(Statistics.OTHER_FAILURE, new SchemaException("Couldn't retrieve matching rule for identifier " +
                    identifier.getName() + " (rule name = " + matchingRuleQName + ")"));
            return;
        }

        Object normalizedValue = matchingRule.normalize(value);
        if (!(normalizedValue instanceof String)) {
            checkResult.recordError(Statistics.OTHER_FAILURE, new SchemaException("Normalized value is not a string, it's " + normalizedValue.getClass() +
                    " (identifier " + identifier.getName() + ", value " + value));
            return;
        }
        if (value.equals(normalizedValue)) {
            return;     // OK
        }
        String normalizedStringValue = (String) normalizedValue;

        checkResult.recordError(Statistics.NON_NORMALIZED_IDENTIFIER_VALUE,
                new SchemaException("Non-normalized value of identifier " + identifier.getName()
                        + ": " + value + " (normalized form: " + normalizedValue + ")"));

        if (fixNormalization) {
            PropertyDelta delta = identifier.createEmptyDelta(new ItemPath(ShadowType.F_ATTRIBUTES, identifier.getName()));
            delta.setValueToReplace(new PrismPropertyValue<>(normalizedStringValue));
            checkResult.addFixDelta(delta, Statistics.NON_NORMALIZED_IDENTIFIER_VALUE);
        }
    }

    private void addIdentifierValue(ShadowCheckResult checkResult, ObjectTypeContext context, QName identifierName, String identifierValue, PrismObject<ShadowType> shadow) {

        Map<String, List<PrismObject<ShadowType>>> valueMap = context.getIdentifierValueMap().get(identifierName);
        if (valueMap == null) {
            valueMap = new HashMap<>();
            context.getIdentifierValueMap().put(identifierName, valueMap);
        }
        List<PrismObject<ShadowType>> existingShadows = valueMap.get(identifierValue);
        if (existingShadows == null) {
            // all is well
            existingShadows = new ArrayList();
            existingShadows.add(shadow);
            valueMap.put(identifierValue, existingShadows);
        } else {
            checkResult.recordError(Statistics.DUPLICATE_SHADOWS, new SchemaException("Multiple shadows with the value of identifier " +
                    "attribute " + identifierName + " = " + identifierValue + ": existing one(s): " + shortDumpList(existingShadows) +
                    ", duplicate: " + ObjectTypeUtil.toShortString(shadow.asObjectable())));
            existingShadows.add(shadow);
        }
    }

    private String shortDumpList(List<PrismObject<ShadowType>> list) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (PrismObject<ShadowType> object : list) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(ObjectTypeUtil.toShortString(object.asObjectable()));
        }
        return sb.toString();
    }

    public Statistics getStatistics() {
        return statistics;
    }

    @Override
    public void completeProcessing(OperationResult result) {
        super.completeProcessing(result);

        LOGGER.info("Shadow integrity check finished.\n" +
                "    Shadows processed: {} ({} resources),\n" +
                "    Shadows with no problems: {}\n" +
                "    Shadows with warnings: {}\n" +
                "    Shadows with errors: {}\n" +
                //"    Shadows with uncompleted operations: {}\n" +
                "    Details:\n{}",
                statistics.getShadows(), statistics.getResources(),
                statistics.getShadows() - statistics.getShadowsWithErrors() - statistics.getShadowsWithWarnings(),
                statistics.getShadowsWithWarnings(), statistics.getShadowsWithErrors(),
                statistics.getDetailsFormatted());

        if (checkUniqueness) {
            StringBuilder sb = new StringBuilder();

            for (Map.Entry<Pair<String,ShadowKindType>, ObjectTypeContext> entry : contextMap.entrySet()) {
                String resourceOid = entry.getKey().getLeft();
                ShadowKindType kind = entry.getKey().getRight();
                ObjectTypeContext ctx = entry.getValue();
                PrismObject<ResourceType> resource = resources.get(resourceOid);
                if (resource == null) {
                    LOGGER.error("No resource for {}", resourceOid);        // should not happen
                    continue;
                }
                for (Map.Entry<QName, Map<String, List<PrismObject<ShadowType>>>> idValEntry : ctx.getIdentifierValueMap().entrySet()) {
                    QName identifier = idValEntry.getKey();
                    boolean first = true;
                    for (Map.Entry<String, List<PrismObject<ShadowType>>> valListEntry : idValEntry.getValue().entrySet()) {
                        List<PrismObject<ShadowType>> shadows = valListEntry.getValue();
                        if (shadows.size() > 1) {
                            if (first) {
                                sb.append("Duplicates for ").append(ObjectTypeUtil.toShortString(resource));
                                sb.append(", kind = ").append(kind);
                                sb.append(", identifier = ").append(identifier).append(":\n");
                                first = false;
                            }
                            sb.append(" - value: ").append(valListEntry.getKey()).append(", shadows: ").append(shadows.size()).append("\n");
                            for (PrismObject<ShadowType> shadow : shadows) {
                                sb.append("   - ").append(ObjectTypeUtil.toShortString(shadow));
                                sb.append("; sync situation = ").append(shadow.asObjectable().getSynchronizationSituation()).append("\n");
                                PrismContainer<ShadowAttributesType> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
                                if (attributesContainer != null && !attributesContainer.isEmpty()) {
                                    for (Item item : attributesContainer.getValue().getItems()) {
                                        sb.append("     - ").append(item.getElementName().getLocalPart()).append(" = ");
                                        sb.append(((PrismProperty) item).getRealValues());
                                        sb.append("\n");
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (sb.length() == 0) {
                sb.append("No duplicates.\n");
            }
            LOGGER.info("Uniqueness report:\n{}", sb.toString());
        }
    }

}
