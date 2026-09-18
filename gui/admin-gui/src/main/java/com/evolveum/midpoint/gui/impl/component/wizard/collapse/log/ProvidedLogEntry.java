/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.Serial;
import java.io.Serializable;

/**
 * One log entry together with the provider it came from, needed to know whether that provider runs in debug mode.
 * @param provider Provider which provided log entry.
 * @param entry Provided log entry.
 */
record ProvidedLogEntry(OperationLogProvider provider, OperationLogEntry entry) implements Serializable {

    @Serial private static final long serialVersionUID = 1L;
}
