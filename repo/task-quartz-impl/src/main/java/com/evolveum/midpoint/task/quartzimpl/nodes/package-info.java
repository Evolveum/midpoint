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
