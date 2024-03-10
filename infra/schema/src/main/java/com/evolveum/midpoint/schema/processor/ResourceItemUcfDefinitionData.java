/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.AbstractFreezable;

import org.jetbrains.annotations.Nullable;

/** Contains real data for UCF aspect of resource attribute/association definition. */
public class ResourceItemUcfDefinitionData
        extends AbstractFreezable
        implements ResourceItemUcfDefinition, ResourceItemUcfDefinition.Mutable {

    /** Name that is native on the resource. It is provided e.g. as part of ConnId attribute information. */
    private String nativeAttributeName;

    /** Name under which this attribute is seen by the connection framework (like ConnId). */
    private String frameworkAttributeName;

    private Boolean returnedByDefault;

    @Override
    public String getNativeAttributeName() {
        return nativeAttributeName;
    }

    public void setNativeAttributeName(String nativeAttributeName) {
        checkMutable();
        this.nativeAttributeName = nativeAttributeName;
    }

    @Override
    public String getFrameworkAttributeName() {
        return frameworkAttributeName;
    }

    public void setFrameworkAttributeName(String frameworkAttributeName) {
        checkMutable();
        this.frameworkAttributeName = frameworkAttributeName;
    }

    @Override
    public @Nullable Boolean getReturnedByDefault() {
        return returnedByDefault;
    }

    public void setReturnedByDefault(Boolean returnedByDefault) {
        checkMutable();
        this.returnedByDefault = returnedByDefault;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        if (nativeAttributeName != null) {
            sb.append(" native=");
            sb.append(nativeAttributeName);
        }
        if (frameworkAttributeName !=null) {
            sb.append(" framework=");
            sb.append(frameworkAttributeName);
        }
        if (returnedByDefault != null) {
            sb.append(" returnedByDefault=");
            sb.append(returnedByDefault);
        }
    }
}
