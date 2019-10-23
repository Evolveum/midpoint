/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 *  @author shood
 * */
public class WizardUtil {

    public static MappingType createEmptyMapping(){
        MappingType mapping = new MappingType();
        mapping.setAuthoritative(true);

        return mapping;
    }

    public static boolean isEmptyMapping(MappingType toCompare){
        return createEmptyMapping().equals(toCompare);

    }
}
