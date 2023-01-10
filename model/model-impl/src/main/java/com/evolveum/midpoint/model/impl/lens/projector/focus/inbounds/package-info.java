/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

/**
 * Implements gathering and evaluation of inbound mappings: both during clockwork and before it (for correlation purposes).
 *
 * Main classes:
 *
 * 1. {@link com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.ClockworkInboundsProcessing}: provides complete
 * processing during clockwork, i.e. collecting, evaluating, and consolidating inbound mappings.
 *
 * 2. {@link com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.PreInboundsProcessing}: provides processing before
 * the clockwork.
 *
 * Among helper classes, _mapping preparation_ is the most complex operation. It is carried out by classes in
 * {@link com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep} package.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;
