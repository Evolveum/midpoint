/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity.shadows;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.Collection;

public class DuplicateShadowsTreatmentInstruction {

    private Collection<PrismObject<ShadowType>> shadowsToDelete;
    private String shadowOidToReplaceDeletedOnes;

    public Collection<PrismObject<ShadowType>> getShadowsToDelete() {
        return shadowsToDelete;
    }

    public void setShadowsToDelete(Collection<PrismObject<ShadowType>> shadowsToDelete) {
        this.shadowsToDelete = shadowsToDelete;
    }

    public String getShadowOidToReplaceDeletedOnes() {
        return shadowOidToReplaceDeletedOnes;
    }

    public void setShadowOidToReplaceDeletedOnes(String shadowOidToReplaceDeletedOnes) {
        this.shadowOidToReplaceDeletedOnes = shadowOidToReplaceDeletedOnes;
    }

    @Override
    public String toString() {
        return "DuplicateShadowsTreatmentInstruction{" +
                "shadowsToDelete=" + shadowsToDelete +
                ", shadowOidToReplaceDeletedOnes='" + shadowOidToReplaceDeletedOnes + '\'' +
                '}';
    }
}
