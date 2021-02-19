package com.evolveum.midpoint.model.impl.sync;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassifier;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

@Component
public class ResourceObjectClassifierImpl implements ResourceObjectClassifier {

    private static final String OP_CLASSIFY = ResourceObjectClassifierImpl.class.getName() + ".classify";

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private SynchronizationService synchronizationService;
    @Autowired private ProvisioningService provisioningService;

    @PostConstruct
    void initialize() {
        provisioningService.setResourceObjectClassifier(this);
    }

    @PreDestroy
    void destroy() {
        provisioningService.setResourceObjectClassifier(null);
    }

    @Override
    public @NotNull Classification classify(@NotNull PrismObject<ShadowType> combinedObject,
            @NotNull PrismObject<ResourceType> resource, @NotNull Task task, @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        OperationResult result = parentResult.subresult(OP_CLASSIFY)
                .addParam("combinedObject", combinedObject)
                .addParam("resource", resource)
                .build();
        try {
            return doClassify(combinedObject, resource, task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private Classification doClassify(PrismObject<ShadowType> combinedObject, PrismObject<ResourceType> resource,
            Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        PrismObject<SystemConfigurationType> configuration = systemObjectCache.getSystemConfiguration(result);
        SynchronizationContext<?> syncCtx = synchronizationService.loadSynchronizationContext(
                combinedObject, combinedObject, null, resource,
                task.getCategory(), null, configuration, task, result);

        return createClassification(combinedObject, syncCtx);
    }

    @NotNull
    private Classification createClassification(PrismObject<ShadowType> combinedObject, SynchronizationContext<?> syncCtx)
            throws SchemaException {
        ShadowType objectBean = combinedObject.asObjectable();

        // This is how original synchronization service was implemented: it did not overwrite previously known values.
        ShadowKindType newKind = ShadowUtil.isKnown(objectBean.getKind()) ? objectBean.getKind() : syncCtx.getKind();
        String newIntent = ShadowUtil.isKnown(objectBean.getIntent()) ? objectBean.getIntent() : syncCtx.getIntent();

        // And as for the tag, currently it creates syncCtx.tag value only if it really wants it to be changed.
        // Otherwise it is null.
        String newTag = objectBean.getTag() != null ? objectBean.getTag() : syncCtx.getTag();

        return new Classification(newKind, newIntent, newTag);
    }
}
