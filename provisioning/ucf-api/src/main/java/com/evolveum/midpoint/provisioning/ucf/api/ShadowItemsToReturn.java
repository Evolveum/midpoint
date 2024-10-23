/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;

import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;

/**
 * This is something that maps to the "attributes to return" (or to get) in the underlying framework.
 *
 * @author semancik
 */
public class ShadowItemsToReturn implements Serializable {
    @Serial private static final long serialVersionUID = 157146351122133667L;

    /** We request that all default attributes should be returned. */
    private boolean returnDefaultAttributes = true;

    /** We request that the password be returned. */
    private boolean returnPasswordExplicit = false;

    /** We request that the administrative status be returned. */
    private boolean returnAdministrativeStatusExplicit = false;

    /** We request that the lockout status be returned. */
    private boolean returnLockoutStatusExplicit = false;

    /** We request that the "valid form" be returned. */
    private boolean returnValidFromExplicit = false;

    /** We request that the "valid to" be returned. */
    private boolean returnValidToExplicit = false;

    /** We request that the "last login timestamp" be returned. */
    private boolean returnLastLoginTimestampExplicit = false;

    /** The list of items that we explicitly request to be returned. */
    private Collection<? extends ShadowAttributeDefinition<?, ?, ?, ?>> itemsToReturn = null;

    public boolean isReturnDefaultAttributes() {
        return returnDefaultAttributes;
    }

    public void setReturnDefaultAttributes(boolean returnDefaultAttributes) {
        this.returnDefaultAttributes = returnDefaultAttributes;
    }

    public Collection<? extends ShadowAttributeDefinition<?, ?, ?, ?>> getItemsToReturn() {
        return itemsToReturn;
    }

    public void setItemsToReturn(Collection<? extends ShadowAttributeDefinition<?, ?, ?, ?>> itemsToReturn) {
        this.itemsToReturn = itemsToReturn;
    }

    public boolean isReturnPasswordExplicit() {
        return returnPasswordExplicit;
    }

    public void setReturnPasswordExplicit(boolean returnPasswordExplicit) {
        this.returnPasswordExplicit = returnPasswordExplicit;
    }

    public boolean isReturnAdministrativeStatusExplicit() {
        return returnAdministrativeStatusExplicit;
    }

    public void setReturnAdministrativeStatusExplicit(boolean returnAdministrativeStatusExplicit) {
        this.returnAdministrativeStatusExplicit = returnAdministrativeStatusExplicit;
    }

    public boolean isReturnLockoutStatusExplicit() {
        return returnLockoutStatusExplicit;
    }

    public void setReturnLockoutStatusExplicit(boolean returnLockoutStatusExplicit) {
        this.returnLockoutStatusExplicit = returnLockoutStatusExplicit;
    }

    public boolean isReturnValidFromExplicit() {
        return returnValidFromExplicit;
    }

    public void setReturnValidFromExplicit(boolean returnValidFromExplicit) {
        this.returnValidFromExplicit = returnValidFromExplicit;
    }

    public boolean isReturnValidToExplicit() {
        return returnValidToExplicit;
    }

    public void setReturnValidToExplicit(boolean returnValidToExplicit) {
        this.returnValidToExplicit = returnValidToExplicit;
    }

    public boolean isReturnLastLoginTimestampExplicit() {
        return returnLastLoginTimestampExplicit;
    }

    public void setReturnLastLoginTimestampExplicit(boolean returnLastLoginTimestampExplicit) {
        this.returnLastLoginTimestampExplicit = returnLastLoginTimestampExplicit;
    }

    public boolean isAllDefault() {
        return returnDefaultAttributes
                && !returnPasswordExplicit
                && !returnAdministrativeStatusExplicit
                && !returnLockoutStatusExplicit
                && !returnValidFromExplicit
                && !returnValidToExplicit
                && (itemsToReturn == null || itemsToReturn.isEmpty());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(returnDefaultAttributes=" + returnDefaultAttributes
                + ", returnPasswordExplicit=" + returnPasswordExplicit
                + ", returnAdministrativeStatusExplicit=" + returnAdministrativeStatusExplicit
                + ", returnLockoutStatusExplicit=" + returnLockoutStatusExplicit
                + ", returnValidFromExplicit=" + returnValidFromExplicit
                + ", returnValidToExplicit=" + returnValidToExplicit
                + ", itemsToReturn=" + itemsToReturn + ")";
    }
}
