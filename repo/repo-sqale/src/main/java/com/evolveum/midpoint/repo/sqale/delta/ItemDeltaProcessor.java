/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Essential contract for processing item delta modifications.
 * There are two basic subtypes, {@link DelegatingItemDeltaProcessor} taking care of the path
 * and then various subtypes of {@link ItemDeltaValueProcessor} for processing the value changes.
 */
public interface ItemDeltaProcessor {

    ProcessingHint DEFAULT_PROCESSING = new ProcessingHint(false);
    ProcessingHint SKIP_FULL_OBJECT_UPDATE = new ProcessingHint(true);


    ProcessingHint process(ItemDelta<?, ?> modification) throws RepositoryException, SchemaException;


    record ProcessingHint(boolean skipFullObject) {

        public ProcessingHint combine(ProcessingHint other) {
            /* We can only skip full object if both hints allows it */
            var skipFullObject = this.skipFullObject && other.skipFullObject;
            return new ProcessingHint(skipFullObject);
        }
    }
}
