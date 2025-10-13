/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LensContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ModelContextUtil {

    public static <O extends ObjectType> ModelContext<O> unwrapModelContext(LensContextType lensContextType,
            ModelInteractionService modelInteractionService, Task opTask, OperationResult result) throws ObjectNotFoundException {
        if (lensContextType != null) {
            try {
                return modelInteractionService.unwrapModelContext(lensContextType, opTask, result);
            } catch (SchemaException | CommunicationException | ConfigurationException |ExpressionEvaluationException e) {   // todo treat appropriately
                throw new SystemException("Couldn't access model operation context in task: " + e.getMessage(), e);
            }
        } else {
            return null;
        }
    }
}
