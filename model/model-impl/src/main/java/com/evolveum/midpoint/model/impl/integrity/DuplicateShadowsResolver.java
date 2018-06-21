/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.Collection;

/**
 * @author Pavol Mederly
 */
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
