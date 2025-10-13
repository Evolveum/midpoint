/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.SearchHierarchyConstraints;

/**
 * Aggregation of a search query with {@link SearchHierarchyConstraints}.
 *
 * Used as a return value of {@link DelineationProcessor} that waves object type delineation into client-specified
 * search parameters.
 */
record QueryWithConstraints(ObjectQuery query, SearchHierarchyConstraints constraints) {

}
