/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectMultiplicityType;

public class RefinedDefinitionUtil {

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isMultiaccount(ResourceObjectMultiplicityType multiplicity) {
        if (multiplicity == null) {
            return false;
        }
        String maxOccurs = multiplicity.getMaxOccurs();
        if (maxOccurs == null || maxOccurs.equals("1")) {
            return false;
        }
        return true;
    }
}
