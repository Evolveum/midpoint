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
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

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
        void process(ObjectTemplateType includedTemplate) throws ConfigurationException;
    }

    public void processThisAndIncludedTemplates(
            ObjectTemplateType objectTemplate, String contextDesc, Task task, OperationResult result, TemplateProcessor processor)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (!SimulationUtil.isVisible(objectTemplate, task.getExecutionMode())) {
            LOGGER.trace("Ignoring template {} as it is not visible for the current task", objectTemplate);
            return;
        }
        processor.process(objectTemplate);
        for (ObjectReferenceType includeRef: objectTemplate.getIncludeRef()) {
            PrismObject<ObjectTemplateType> includedObject;
            if (includeRef.asReferenceValue().getObject() != null) {
                includedObject = includeRef.asReferenceValue().getObject();
            } else {
                ObjectTemplateType includedObjectBean =
                        objectResolver.resolve(includeRef, ObjectTemplateType.class, readOnly(),
                        "include reference in "+objectTemplate + " in " + contextDesc, task, result);
                includedObject = includedObjectBean.asPrismObject();
                if (!includeRef.asReferenceValue().isImmutable()) {
                    // Store resolved object for future use (e.g. next waves).
                    // TODO If we have a template including other templates (i.e. !includeRef.isEmpty) we might clone
                    //  it right after fetching from the repository. But this is quite rare case, and resolving template
                    //  from the cache is fast. So maybe we shouldn't bother with that.
                    includeRef.asReferenceValue().setObject(includedObject);
                }
            }
            LOGGER.trace("Including template {}", includedObject);
            processThisAndIncludedTemplates(
                    includedObject.asObjectable(), includedObject + " in " + contextDesc, task, result, processor);
        }
    }
}
