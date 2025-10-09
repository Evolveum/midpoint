/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.impl.lens.indexing.IndexingManager;
import com.evolveum.midpoint.model.impl.lens.tasks.TaskOperationalDataManager;

/**
 * Just a marker interface for now, reminding us that there seems to be a repeated pattern of "delta execution preprocessors"
 * (currently {@link OperationalDataManager}, {@link TaskOperationalDataManager}, and {@link IndexingManager}) that tweak
 * the deltas before they are executed.
 *
 * In the future we can think about some generalization of these preprocessors, including extracting common
 * methods to this interface, code deduplication, and so on.
 */
public interface DeltaExecutionPreprocessor {
}
