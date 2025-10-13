/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.cleanup;

import java.util.ArrayList;
import java.util.List;

public class CleanupResult {

    private List<CleanupItem<?>> messages;

    public List<CleanupItem<?>> getMessages() {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        return messages;
    }

    public void setMessages(List<CleanupItem<?>> messages) {
        this.messages = messages;
    }
}
