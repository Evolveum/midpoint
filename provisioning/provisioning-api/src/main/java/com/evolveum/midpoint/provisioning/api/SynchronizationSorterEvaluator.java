package com.evolveum.midpoint.provisioning.api;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Nullable;

/**
 * TODO
 */
@Experimental
public interface SynchronizationSorterEvaluator {

    /**
     * TODO
     *
     * @param combinedObject Resource object that we want to classify. It should be connected to the shadow,
     * however, exact "shadowization" is not required. Currently it should contain all the information from the shadow,
     * plus all the attributes from resource object. If needed, more elaborate processing (up to full shadowization)
     * can be added later.
     *
     * @param resource Resource on which the resource object was found
     */
    @Nullable ObjectSynchronizationDiscriminatorType evaluate(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceType resource,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException;
}
