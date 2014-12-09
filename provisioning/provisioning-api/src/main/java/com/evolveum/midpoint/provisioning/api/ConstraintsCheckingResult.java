/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.PrismObject;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.Set;

/**
 * @author mederly
 */
public class ConstraintsCheckingResult {

    private boolean satisfiesConstraints;
    private PrismObject conflictingShadow;
    private String messages;
    private Set<QName> checkedAttributes = new HashSet<>();
    private Set<QName> conflictingAttributes = new HashSet<>();

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
