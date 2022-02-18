/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.PrismObject;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.Set;

public class ConstraintsCheckingResult {

    private boolean satisfiesConstraints;
    private PrismObject conflictingShadow;
    private String messages;
    private Set<QName> checkedAttributes = new HashSet<>();
    private Set<QName> conflictingAttributes = new HashSet<>();

    public static ConstraintsCheckingResult createOk() {
        ConstraintsCheckingResult rv = new ConstraintsCheckingResult();
        rv.setSatisfiesConstraints(true);
        return rv;
    }

    public boolean isSatisfiesConstraints() {
        return satisfiesConstraints;
    }

    public void setSatisfiesConstraints(boolean satisfiesConstraints) {
        this.satisfiesConstraints = satisfiesConstraints;
    }

    public PrismObject getConflictingShadow() {
        return conflictingShadow;
    }

    public void setConflictingShadow(PrismObject conflictingShadow) {
        this.conflictingShadow = conflictingShadow;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }

    public Set<QName> getCheckedAttributes() {
        return checkedAttributes;
    }

    public void setCheckedAttributes(Set<QName> checkedAttributes) {
        this.checkedAttributes = checkedAttributes;
    }

    public Set<QName> getConflictingAttributes() {
        return conflictingAttributes;
    }

    public void setConflictingAttributes(Set<QName> conflictingAttributes) {
        this.conflictingAttributes = conflictingAttributes;
    }
}
