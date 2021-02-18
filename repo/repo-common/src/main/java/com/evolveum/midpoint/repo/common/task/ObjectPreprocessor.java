package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface ObjectPreprocessor<O extends ObjectType> {

    /**
     * Pre-processes the object.
     *
     * Error handling: If the processing has to be stopped, an exception needs to be thrown.
     * Otherwise (e.g. if only an operation result is set) the processing will continue.
     */
    PrismObject<O> preprocess(PrismObject<O> originalObject, Task task, OperationResult result)
            throws CommonException;
}
