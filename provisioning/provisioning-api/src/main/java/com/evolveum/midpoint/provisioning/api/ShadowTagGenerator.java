package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generates tags for shadows.
 */
@Experimental
public interface ShadowTagGenerator {

    /**
     * Generates a tag for the shadow.
     *
     * @param combinedObject Resource object that we want to generate the tag for. It should be connected to the shadow,
     * just like the object for {@link ResourceObjectClassifier#classify(ShadowType, ResourceType, Task, OperationResult)}.
     * @param resource Resource on which the resource object was found
     * @param definition Object type definition for the shadow. Included for performance reasons (we assume the client knows it).
     */
    @Nullable String generateTag(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceType resource,
            @NotNull ResourceObjectTypeDefinition definition,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException;
}
