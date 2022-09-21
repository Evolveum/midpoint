/**
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.api;

/**
 * See https://wiki.evolveum.com/display/midPoint/Pending+Operations+and+Dead+Shadows
 *
 * @author semancik
 */
public enum ShadowState {

    PROPOSED,
    CONCEPTION,
    GESTATION,
    LIFE,
    REAPING,
    CORPSE,
    TOMBSTONE;

}
