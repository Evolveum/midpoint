/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.Serializable;

/**
 * One user action fired by {@link OperationLogListPanel} and handled by {@link OperationCompoundLogPanel}
 */
public sealed interface OperationLogListAction extends Serializable {

    /** The severity filter  was changed. */
    record LevelSelected(OperationLogLevel level) implements OperationLogListAction {
    }

    /** The user selected log entry */
    record EntrySelected(ProvidedLogEntry entry) implements OperationLogListAction {
    }
}
