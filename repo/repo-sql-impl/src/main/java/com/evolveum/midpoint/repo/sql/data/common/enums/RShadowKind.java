/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;

/**
 * @author lazyman
 */
public enum RShadowKind {

    ACCOUNT(ShadowKindType.ACCOUNT),
    ENTITLEMENT(ShadowKindType.ENTITLEMENT),
    GENERIC(ShadowKindType.GENERIC);

    private ShadowKindType kind;

    private RShadowKind(ShadowKindType kind) {
        this.kind = kind;
    }

    public ShadowKindType getKind() {
        return kind;
    }

    public static RShadowKind toRepoType(ShadowKindType kind) {
        if (kind == null) {
            return null;
        }

        for (RShadowKind repo : RShadowKind.values()) {
            if (kind.equals(repo.getKind())) {
                return repo;
            }
        }

        throw new IllegalArgumentException("Unknown shadow kind type " + kind);
    }
}
