/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;


/**
 * Representation of SQL statement executed for one log entry.
 * @param query Query of sql statement.
 */
public record SqlProtocol(String query) implements OperationLogProtocol {
}
