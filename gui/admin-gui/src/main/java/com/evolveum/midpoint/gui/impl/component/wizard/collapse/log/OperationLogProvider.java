/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.Serializable;
import java.util.List;

/**
 * Supplies the log entries shown in the log viewer drawer for one wizard panel, see {@link OperationCompoundLogPanel}.
 */
@FunctionalInterface
public interface OperationLogProvider extends Serializable {

    /**
     * Returns the log entries to display, ordered by {@link OperationLogEntry#getTimestamp()}. Called repeatedly
     * per drawer render (severity counts, filtering, visibility check).
     *
     * @return the entries for this provider, ordered by timestamp
     */
    public List<OperationLogEntry> getOperationLogEntries();

    /**
     * Whether the protocol detail (SQL/HTTP) of an entry without a {@link OperationLogEntry#getProtocol()} is
     * simply not captured outside of development mode - used by {@link OperationLogEventDetailPanel} to decide whether the
     * "Protocol" tab is shown in an explanatory disabled state, or not shown at all.
     *
     * @return true if this provider's entries were captured with development/debug mode enabled
     */
    default boolean isDebugModeEnabled() {
        return false;
    }
}
