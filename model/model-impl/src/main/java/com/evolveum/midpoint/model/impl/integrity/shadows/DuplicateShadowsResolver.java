/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.integrity.shadows;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.Collection;

@FunctionalInterface
public interface DuplicateShadowsResolver {

    /**
     * Takes a collection of duplicate shadows - i.e. shadows pointing to (presumably) one resource object,
     * and returns a treatment instruction: a collection of shadows that have to be deleted + which OID to use in owner object as a replacement.
     *
     * @param shadows
     * @return
     */
    DuplicateShadowsTreatmentInstruction determineDuplicateShadowsTreatment(Collection<PrismObject<ShadowType>> shadows);
}
