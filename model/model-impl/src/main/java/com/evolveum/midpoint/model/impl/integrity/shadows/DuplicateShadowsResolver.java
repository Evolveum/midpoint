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
