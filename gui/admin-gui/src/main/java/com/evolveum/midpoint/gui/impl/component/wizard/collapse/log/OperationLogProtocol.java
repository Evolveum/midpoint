/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.Serializable;

/**
 * Interface for the protocol-specific detail of one {@link OperationLogEntry} (e.g. {@link SqlProtocol},
 * {@link HttpProtocol}).
 */
public interface OperationLogProtocol extends Serializable {
}
