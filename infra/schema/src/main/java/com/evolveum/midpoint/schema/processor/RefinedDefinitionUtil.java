/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
