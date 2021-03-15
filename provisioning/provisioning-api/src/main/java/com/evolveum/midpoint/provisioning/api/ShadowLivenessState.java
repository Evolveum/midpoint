/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Describes shadow liveness (or death) state.
 */
public enum ShadowLivenessState {

    /**
     * Shadow is live, i.e. it exists in repo and its dead property is not true.
     */
    LIVE,

    /**
     * Shadow is dead, i.e. it exists in repo and its dead property is true.
     */
    DEAD,

    /**
     * Shadow does not exist in repo.
     */
    DELETED;

    public static ShadowLivenessState forShadow(PrismObject<ShadowType> shadow) {
        if (shadow == null) {
            return DELETED;
        } else if (ShadowUtil.isDead(shadow)) {
            return DEAD;
        } else {
            return LIVE;
        }
    }
}
