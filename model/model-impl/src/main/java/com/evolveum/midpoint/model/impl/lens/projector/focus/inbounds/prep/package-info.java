/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

/**
 * Prepares inbound mappings related to a _single projection_ (shadow) for evaluation.
 *
 * (With the exception of password/activation mappings, that are - temporarily - evaluated here.)
 *
 * The main class is {@link com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.SingleShadowInboundsPreparation}
 * (with two subclasses for full/limited situations).
 *
 * See its javadoc for the description of the further decomposition of the work.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;
