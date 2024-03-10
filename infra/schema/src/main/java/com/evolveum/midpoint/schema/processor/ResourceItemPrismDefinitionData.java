/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.AbstractFreezable;

/** Contains real data for prism aspect of resource attribute/association definition. */
public class ResourceItemPrismDefinitionData
        extends AbstractFreezable
        implements ResourceItemPrismDefinition, ResourceItemPrismDefinition.Mutable {

    private int minOccurs;
    private int maxOccurs;
    private Boolean canRead;
    private Boolean canModify;
    private Boolean canAdd;

    @Override
    public ResourceItemPrismDefinitionData prismData() {
        throw new UnsupportedOperationException("No delegation from here");
    }

    @Override
    public int getMinOccurs() {
        return minOccurs;
    }

    @Override
    public void setMinOccurs(int minOccurs) {
        checkMutable();
        this.minOccurs = minOccurs;
    }

    @Override
    public int getMaxOccurs() {
        return maxOccurs;
    }

    @Override
    public void setMaxOccurs(int maxOccurs) {
        checkMutable();
        this.maxOccurs = maxOccurs;
    }

    public Boolean getCanRead() {
        return canRead;
    }

    public void setCanRead(boolean canRead) {
        checkMutable();
        this.canRead = canRead;
    }

    public Boolean getCanModify() {
        return canModify;
    }

    public void setCanModify(boolean canModify) {
        checkMutable();
        this.canModify = canModify;
    }

    public Boolean getCanAdd() {
        return canAdd;
    }

    public void setCanAdd(boolean canAdd) {
        checkMutable();
        this.canAdd = canAdd;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(" ** TODO **");
    }
}
