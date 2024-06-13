/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.web.component.util.Selectable;

/**
 * @author lazyman
 */
public class ObjectClassDto extends Selectable<ObjectClassDto>
        implements Comparable<ObjectClassDto> {

    public static final String F_DISPLAY_NAME = "displayName";

    @NotNull private final ResourceObjectClassDefinition objectClassDefinition;

    public ObjectClassDto(@NotNull ResourceObjectClassDefinition definition) {
        Validate.notNull(definition, "Refined object definition must not be null.");
        this.objectClassDefinition = definition;
    }

    public QName getObjectClassName() {
        return objectClassDefinition.getTypeName();
    }

    public String getDisplayName() {
        StringBuilder builder = new StringBuilder();
        if (objectClassDefinition.getTypeName() != null) {
            builder.append(objectClassDefinition.getTypeName().getLocalPart());
        }
        if (StringUtils.isNotEmpty(objectClassDefinition.getDisplayName())) {
            builder.append(" (").append(objectClassDefinition.getDisplayName()).append(")");
        }
        return builder.toString().trim();
    }

    public ResourceObjectClassDefinition getDefinition() {
        return objectClassDefinition;
    }

    @Override
    public int compareTo(@NotNull ObjectClassDto o) {
        return String.CASE_INSENSITIVE_ORDER.compare(getDisplayName(), o.getDisplayName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof ObjectClassDto)) { return false; }

        ObjectClassDto that = (ObjectClassDto) o;

        return Objects.equals(objectClassDefinition, that.objectClassDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectClassDefinition);
    }
}
