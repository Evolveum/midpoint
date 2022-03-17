/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.ObjectHandler;

/**
 * Handles UCF objects, typically coming from iterative search.
 *
 * @author Radovan Semancik
 */
@FunctionalInterface
public interface UcfObjectHandler extends ObjectHandler<UcfObjectFound> {

}
