/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.objectclass;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * State of the mismatching attributes of the retrieved object: those marked by the user and the initial marks.
 */
public class ObjectVerificationMismatchState implements Serializable {

    private final SerializableSupplier<Set<String>> initialMarks;

    /**
     * Names of the attributes the user marked as mismatching.
     */
    private final Set<String> markedAttributes = new HashSet<>();

    /**
     * OID of the retrieved object the marks belong to; the marks are reset when another object is retrieved.
     */
    private String markedObjectOid;

    public ObjectVerificationMismatchState(SerializableSupplier<Set<String>> initialMarks) {
        this.initialMarks = initialMarks;
    }

    /**
     * Returns the names of the marked attributes.
     *
     * @return the names of the marked attributes
     */
    public Set<String> getMarkedAttributes() {
        return markedAttributes;
    }

    /**
     * Starts over with the initial marks when the retrieved object is another one than the marks belong to.
     *
     * @param retrievedObject the object currently retrieved from the external system
     */
    public void resetIfObjectChanged(ShadowType retrievedObject) {
        String oid = retrievedObject != null ? retrievedObject.getOid() : null;
        if (!Objects.equals(oid, markedObjectOid)) {
            markedObjectOid = oid;
            reset();
        }
    }

    /**
     * Replaces the marks with the initial ones.
     */
    public void reset() {
        markedAttributes.clear();
        markedAttributes.addAll(initialMarks.get());
    }

    /**
     * Drops the link to the retrieved object, so the next retrieved object starts with the initial marks.
     */
    public void forget() {
        markedObjectOid = null;
    }

    /**
     * Tells whether the attribute of the row is marked.
     *
     * @param row the attribute row
     * @return true if the attribute is marked
     */
    public boolean isMarked(RetrievedAttributeDto row) {
        return row != null && getMarkedAttributes().contains(row.getName());
    }

    /**
     * Marks or unmarks the attribute of the row.
     *
     * @param row the attribute row
     * @param marked whether the attribute should be marked
     */
    public void setMarked(RetrievedAttributeDto row, boolean marked) {
        if (row == null) {
            return;
        }
        if (marked) {
            getMarkedAttributes().add(row.getName());
        } else {
            getMarkedAttributes().remove(row.getName());
        }
    }

    /**
     * Tells whether any attribute is marked.
     *
     * @return true if at least one attribute is marked
     */
    public boolean isAnyMarked() {
        return !getMarkedAttributes().isEmpty();
    }
}
