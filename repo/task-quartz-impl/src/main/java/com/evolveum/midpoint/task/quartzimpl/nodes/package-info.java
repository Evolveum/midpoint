/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

/**
 * Deals with nodes administration:
 *
 * 1. retrieves the nodes - {@link com.evolveum.midpoint.task.quartzimpl.nodes.NodeRetriever};
 * 2. cleans up obsolete nodes - {@link com.evolveum.midpoint.task.quartzimpl.nodes.NodeCleaner}.
 *
 * Does NOT:
 *
 * - does not do actual cluster management (including local node record maintenance) - see the `cluster` package.
 */
package com.evolveum.midpoint.task.quartzimpl.nodes;
