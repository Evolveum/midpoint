/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.cleanup;

import java.util.ArrayList;
import java.util.List;

public class CleanupResult {

    private List<CleanupMessage<?>> messages;

    public List<CleanupMessage<?>> getMessages() {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        return messages;
    }

    public void setMessages(List<CleanupMessage<?>> messages) {
        this.messages = messages;
    }
}
