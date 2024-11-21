/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

/**
 * Supports merging of (usually) configuration beans.
 *
 * For example, resource inheritance and object type inheritance is implemented here.
 *
 * Other uses will be considered in the future.
 *
 * == How to Use
 *
 * === Implementors
 *
 * When you want to create a new merger, you need to just extend {@link com.evolveum.midpoint.schema.merger.BaseMergeOperation}
 * and implement the merging logic. In most cases, it involves providing the appropriate root-level
 * {@link com.evolveum.midpoint.prism.delta.ItemMerger} to the constructor:
 * {@link com.evolveum.midpoint.schema.merger.BaseMergeOperation#BaseMergeOperation(com.evolveum.midpoint.prism.Containerable,
 * com.evolveum.midpoint.prism.Containerable, GenericItemMerger)}
 * Only in rare cases you will need to implement the merging logic yourself, like in
 * {@link com.evolveum.midpoint.schema.merger.objdef.LimitationsMerger} or
 * {@link com.evolveum.midpoint.schema.merger.resource.ObjectTypeDefinitionMerger}.
 *
 * Some of the mergers operate at the level of objects, for example:
 *
 * - {@link com.evolveum.midpoint.schema.merger.resource.ResourceMergeOperation} (for resources)
 * - {@link com.evolveum.midpoint.schema.merger.template.ObjectTemplateMergeOperation} (for object templates)
 *
 * Others are at the level of containerables, like:
 *
 * - {@link com.evolveum.midpoint.schema.merger.correlator.CorrelatorMergeOperation} (for correlator definitions)
 * - {@link com.evolveum.midpoint.schema.merger.simulation.SimulationDefinitionMergeOperation} (for simulation definitions)
 *
 * === Users
 *
 * After instantiating appropriate {@link com.evolveum.midpoint.schema.merger.BaseMergeOperation}, you just call its
 * {@link com.evolveum.midpoint.schema.merger.BaseMergeOperation#execute()} method. Just look for the usages in midPoint code
 * for inspiration.
 *
 * == Limitations
 *
 * The architecture of this package is not finished yet. It is more-or-less an experiment for now.
 *
 * Some mergers here are custom (handwritten) ones, like {@link com.evolveum.midpoint.schema.merger.securitypolicy.SecurityPolicyCustomMerger}.
 * They may be migrated to the generic merger in the future.
 */
package com.evolveum.midpoint.schema.merger;

import com.evolveum.midpoint.prism.impl.GenericItemMerger;
