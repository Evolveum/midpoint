/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.context.ProjectionContextKeyFactory;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

import java.util.Collection;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.INTENT_DEFAULT;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

@Component
public class ProjectionContextKeyFactoryImpl implements ProjectionContextKeyFactory {

    private static final Trace LOGGER = TraceManager.getTrace(ProjectionContextKeyFactoryImpl.class);
    private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

    @Autowired private ProvisioningService provisioningService;

    @Override
    public ProjectionContextKey createKey(@NotNull ShadowType shadow, @NotNull Task task, @NotNull OperationResult result) {
        if (ShadowUtil.isClassified(shadow)) {
            return ProjectionContextKey.fromClassifiedShadow(shadow);
        }

        ShadowType updatedShadow = classify(shadow, task, result);
        if (ShadowUtil.isClassified(updatedShadow)) {
            return ProjectionContextKey.fromClassifiedShadow(updatedShadow);
        }

        EmergencyClassificationResult classificationResult = doEmergencyClassification(shadow, task, result);
        ResourceObjectTypeIdentification typeId = classificationResult.typeId;
        PERFORMANCE_ADVISOR.info("Emergency classification of {} ({}): {}", shadow, classificationResult.resource, typeId);
        if (typeId == null) {
            LOGGER.warn("Unclassified shadow: {} on {} ({}). This may cause performance and functional issues. Please make sure"
                    + " that all shadows in your repository are appropriately classified.",
                    shadow, shadow.getObjectClass(), classificationResult.resource);
        }
        return ProjectionContextKey.fromShadow(shadow, typeId);
    }

    private ShadowType classify(@NotNull ShadowType shadow, @NotNull Task task, @NotNull OperationResult result) {
        String resourceOid = ShadowUtil.getResourceOidRequired(shadow);

        // We assume that the regular classification check has been already done by the caller, along with appropriate
        // error message. This is last chance, to avoid cryptic NPEs or similar messages.
        String shadowOid = shadow.getOid();
        stateCheck(shadowOid != null,
                "It is not possible to classify a shadow with no OID. "
                        + "Please specify the kind/intent precisely for %s", shadow);

        try {
            // We use the "get" operation to classify the shadow.
            //
            // TODO integrate with the context loader - full shadow should be stored in the context, if the fetch
            //  from resource is successful
            //
            // TODO reconsider the options to use
            //   - We use "future point in time" to allow pending changes to be processed. But this is a bit questionable: if we
            //     have any pending changes, doesn't it mean that the shadow is most probably already classified?
            //   - We also don't do discovery - to avoid unexpected processing here. At least for now.
            Collection<SelectorOptions<GetOperationOptions>> options = SchemaService.get().getOperationOptionsBuilder()
                    .futurePointInTime()
                    .doNotDiscovery()
                    .build();
            ShadowType updatedShadow = provisioningService
                    .getObject(ShadowType.class, shadowOid, options, task, result)
                    .asObjectable();
            PERFORMANCE_ADVISOR.info("Fetched {} (resource: {}) in order to classify it (result: {})",
                    updatedShadow, resourceOid, ShadowUtil.getTypeIdentification(updatedShadow));
            return updatedShadow;
        } catch (CommonException e) {
            LoggingUtils.logExceptionAsWarning(
                    LOGGER, "Couldn't load the resource object {} (in order to classify it)", e, shadow);
            return shadow;
        }
    }

    /**
     * The shadow cannot be classified in "normal" way. All we can do now is to try some hacks, like to find
     * default object type for given object class, or check if the object class is not of "account/default" type.
     */
    private @NotNull EmergencyClassificationResult doEmergencyClassification(
            ShadowType shadow, Task task, OperationResult result) {
        QName objectClassName = shadow.getObjectClass();
        if (objectClassName == null) {
            LOGGER.debug("No object class name in {}", shadow);
            return new EmergencyClassificationResult(null, null); // Maybe we could even throw an exception here
        }
        String resourceOid = ShadowUtil.getResourceOidRequired(shadow);
        try {
            ResourceType resourceBean =
                    provisioningService
                            .getObject(ResourceType.class, resourceOid, readOnly(), task, result)
                            .asObjectable();
            ResourceSchema schema = Resource.of(resourceBean).getCompleteSchema();
            if (schema == null) {
                LOGGER.debug("No schema for {}, no classification of {}", resourceBean, shadow);
                return new EmergencyClassificationResult(resourceBean, null);
            }

            // The following finds a "default for class" definition, if there's any. Otherwise it returns object class definition.
            ResourceObjectDefinition definition = schema.findDefinitionForObjectClass(objectClassName);
            LOGGER.trace("Definition for {} ({} on {}): {}", shadow, objectClassName, resourceBean, definition);
            if (definition == null) {
                LOGGER.debug("No definition for {} on {}, no classification for {}", objectClassName, resourceBean, shadow);
                return new EmergencyClassificationResult(resourceBean, null);
            }
            ResourceObjectTypeIdentification typeId = definition.getTypeIdentification();
            if (typeId != null) {
                LOGGER.debug("Emergency classification of {} on {}: {} ({})", shadow, resourceBean, typeId, definition);
                return new EmergencyClassificationResult(resourceBean, typeId);
            }

            if (definition.getObjectClassDefinition().isDefaultAccountDefinition()) {
                LOGGER.debug("{} ({} on {}) is 'default account'", definition, shadow, resourceBean);
                return new EmergencyClassificationResult(
                        resourceBean,
                        ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_DEFAULT));
            }

            LOGGER.debug("Couldn't classify {} on {} (relevant definition: {})", shadow, resourceBean, definition);
            return new EmergencyClassificationResult(resourceBean, null);

        } catch (CommonException e) {
            LoggingUtils.logExceptionAsWarning(
                    LOGGER, "Couldn't do the emergency classification of {}", e, shadow);
            return new EmergencyClassificationResult(null, null);
        }
    }

    private static class EmergencyClassificationResult {
        private final ResourceType resource;
        private final ResourceObjectTypeIdentification typeId;

        private EmergencyClassificationResult(ResourceType resource, ResourceObjectTypeIdentification typeId) {
            this.resource = resource;
            this.typeId = typeId;
        }
    }
}
