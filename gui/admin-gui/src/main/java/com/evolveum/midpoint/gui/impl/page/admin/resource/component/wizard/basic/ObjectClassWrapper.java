/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.web.component.util.Selectable;

public class ObjectClassWrapper extends Selectable<ObjectClassWrapper>
        implements Comparable<ObjectClassWrapper> {

    public static final String F_OBJECT_CLASS_NAME = "objectClassNameAsString";
    public static final String F_NATIVE_OBJECT_CLASS = "nativeObjectClass";

    @NotNull private final ResourceObjectClassDefinition definition;

    public ObjectClassWrapper(@NotNull ResourceObjectClassDefinition definition) {
        Validate.notNull(definition, "Refined object definition must not be null.");
        this.definition = definition;
    }

    public QName getObjectClassName() {
        return definition.getTypeName();
    }

    public String getObjectClassNameAsString() {
        return definition.getTypeName().getLocalPart();
    }

    public String getNativeObjectClass() {
        return definition.getNativeObjectClass();
    }

    public ResourceObjectClassDefinition getDefinition() {
        return definition;
    }

    @Override
    public int compareTo(@NotNull ObjectClassWrapper o) {
        String name = getObjectClassNameAsString();
        if (StringUtils.isNotEmpty(getNativeObjectClass())) {
            name = getNativeObjectClass();
        }

        String oName = o.getObjectClassNameAsString();
        if (StringUtils.isNotEmpty(o.getNativeObjectClass())) {
            oName = o.getNativeObjectClass();
        }

        return String.CASE_INSENSITIVE_ORDER.compare(name, oName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof ObjectClassWrapper)) { return false; }

        ObjectClassWrapper that = (ObjectClassWrapper) o;

        return Objects.equals(definition, that.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(definition);
    }

    public boolean isAuxiliary() {
        return definition.isAuxiliary();
    }
}
