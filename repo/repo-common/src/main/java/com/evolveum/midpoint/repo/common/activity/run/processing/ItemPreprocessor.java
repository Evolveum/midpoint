/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.processing;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;

public interface ItemPreprocessor<C extends Containerable> {

    /**
     * Pre-processes the item.
     *
     * Error handling: If the processing has to be stopped, an exception needs to be thrown.
     * Otherwise (e.g. if only an operation result is set) the processing will continue.
     */
    C preprocess(C originalItem, Task task, OperationResult result)
            throws CommonException;
}
