/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

    void process(ItemDelta<?, ?> modification) throws RepositoryException, SchemaException;
}
