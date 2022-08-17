/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;
import java.util.Objects;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Conflict implements Serializable {

    private String shortMessage;

    private ObjectReferenceType personOfInterest;

    private ConflictItem added;

    private ConflictItem exclusion;

    private String message;

    private ConflictState state = ConflictState.UNRESOLVED;

    private boolean warning;

    private ConflictItem toBeRemoved;

    public Conflict(ObjectReferenceType personOfInterest, ConflictItem added, ConflictItem exclusion, String shortMessage, String message, boolean warning) {
        this.personOfInterest = personOfInterest;
        this.added = added;
        this.exclusion = exclusion;
        this.warning = warning;
        this.shortMessage = shortMessage;
        this.message = message;
    }

    public ObjectReferenceType getPersonOfInterest() {
        return personOfInterest;
    }

    public ConflictItem getAdded() {
        return added;
    }

    public ConflictItem getExclusion() {
        return exclusion;
    }

    public ConflictState getState() {
        return state;
    }

    public void setState(ConflictState state) {
        this.state = state;
    }

    public boolean isWarning() {
        return warning;
    }

    public String getMessage() {
        return message;
    }

    public ConflictItem getToBeRemoved() {
        return toBeRemoved;
    }

    public void setToBeRemoved(ConflictItem toBeRemoved) {
        if (!Objects.equals(toBeRemoved, added) && !Objects.equals(toBeRemoved, exclusion)) {
            return;
        }

        this.toBeRemoved = toBeRemoved;
    }

    public String getShortMessage() {
        return shortMessage;
    }
}
