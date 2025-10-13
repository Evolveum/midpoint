/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import java.io.Serializable;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * Used to access both "legacy" and "modern" association definitions. Should be quite self-explanatory.
 */
public interface AssociationConfigItem extends DebugDumpable {

    record AttributeBinding(
            @NotNull QName subjectSide,
            @NotNull QName objectSide) implements ShortDumpable, Serializable {

        @Override
        public void shortDump(StringBuilder sb) {
            sb.append(subjectSide.getLocalPart())
                    .append(" (subject) <-> ")
                    .append(objectSide.getLocalPart())
                    .append(" (object)");
        }
    }
}
