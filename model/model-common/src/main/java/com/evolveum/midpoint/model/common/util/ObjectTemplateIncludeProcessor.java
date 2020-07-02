/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

public class ObjectTemplateIncludeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTemplateIncludeProcessor.class);

    private final ObjectResolver objectResolver;

    public ObjectTemplateIncludeProcessor(ObjectResolver objectResolver) {
        this.objectResolver = objectResolver;
    }

    /**
     * Internal interface used for handling includeRef references.
     * If needed, exceptions and operation result can be added to the process() method in the future.
     */
    @FunctionalInterface
    public interface TemplateProcessor {
        void process(ObjectTemplateType includedTemplate);
    }

    public void processThisAndIncludedTemplates(ObjectTemplateType objectTemplate, String contextDesc, Task task,
            OperationResult result, TemplateProcessor processor)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        processor.process(objectTemplate);
        for (ObjectReferenceType includeRef: objectTemplate.getIncludeRef()) {
            PrismObject<ObjectTemplateType> includedObject;
            if (includeRef.asReferenceValue().getObject() != null) {
                //noinspection unchecked
                includedObject = includeRef.asReferenceValue().getObject();
            } else {
                ObjectTemplateType includeObjectType = objectResolver.resolve(includeRef, ObjectTemplateType.class,
                        null, "include reference in "+objectTemplate + " in " + contextDesc, task, result);
                includedObject = includeObjectType.asPrismObject();
                // Store resolved object for future use (e.g. next waves).
                includeRef.asReferenceValue().setObject(includedObject);
            }
            LOGGER.trace("Including template {}", includedObject);
            processThisAndIncludedTemplates(includedObject.asObjectable(), includedObject.toString() + " in " + contextDesc,
                    task, result, processor);
        }
    }
}
