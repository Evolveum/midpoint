/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.export;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;

import java.util.Collection;

/**
 * The util class that provides the necessary data for an export in a separate thread
 * @param query The query to llok for an objects to export
 * @param type The type of the objects which are to be exported
 * @param streamingOptions  Search options for the export in a separate thread
 * @param <T>
 */
public record ExportContext<T>(ObjectQuery query, Class<?> type,
                               Collection<SelectorOptions<GetOperationOptions>> streamingOptions) {
}
