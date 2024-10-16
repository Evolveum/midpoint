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
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
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

    @NotNull private final ResourceObjectDefinition refinedDefinition;

    public ObjectClassDto(@NotNull ResourceObjectDefinition definition) {
        Validate.notNull(definition, "Refined object definition must not be null.");
        this.refinedDefinition = definition;
    }

    public QName getObjectClassName() {
        return refinedDefinition.getTypeName();
    }

    public String getDisplayName() {
        StringBuilder builder = new StringBuilder();
        if (refinedDefinition.getTypeName() != null) {
            builder.append(refinedDefinition.getTypeName().getLocalPart());
        }
        if (StringUtils.isNotEmpty(refinedDefinition.getDisplayName())) {
            builder.append(" (").append(refinedDefinition.getDisplayName()).append(")");
        }
        return builder.toString().trim();
    }

    public ResourceObjectDefinition getDefinition() {
        return refinedDefinition;
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

        return Objects.equals(refinedDefinition, that.refinedDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refinedDefinition);
    }

    public boolean isObjectTypeDef() {
        return refinedDefinition instanceof ResourceObjectTypeDefinition;
    }
}
