/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
