/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 *  @author shood
 * */
public class ResourceObjectTypeDefinitionTypeDto implements Serializable{

    public static final String F_OBJECT_TYPE = "objectType";

    @NotNull private final ResourceObjectTypeDefinitionType objectType;

    public ResourceObjectTypeDefinitionTypeDto(@NotNull ResourceObjectTypeDefinitionType objectType){
        this.objectType = objectType;
    }

    @NotNull
    public ResourceObjectTypeDefinitionType getObjectType() {
        return objectType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceObjectTypeDefinitionTypeDto)) return false;

        ResourceObjectTypeDefinitionTypeDto that = (ResourceObjectTypeDefinitionTypeDto) o;

        return objectType.equals(that.objectType);
    }

    @Override
    public int hashCode() {
        return 31 + (objectType != null ? objectType.hashCode() : 0);
    }
}
