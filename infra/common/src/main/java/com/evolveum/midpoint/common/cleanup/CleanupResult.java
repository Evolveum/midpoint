/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.cleanup;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.ArrayList;
import java.util.List;

public class CleanupResult {

    private List<CleanupMessage> messages;

    private List<ObjectReferenceType> missingReferences;

    public List<CleanupMessage> getMessages() {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        return messages;
    }

    public void setMessages(List<CleanupMessage> messages) {
        this.messages = messages;
    }

    public List<CleanupMessage> getMessages(CleanupMessage.Status status) {
        return getMessages().stream().filter(m -> m.status() == status).toList();
    }

    public List<ObjectReferenceType> getMissingReferences() {
        if (missingReferences == null) {
            missingReferences = new ArrayList<>();
        }
        return missingReferences;
    }

    public void setMissingReferences(List<ObjectReferenceType> missingReferences) {
        this.missingReferences = missingReferences;
    }
}
