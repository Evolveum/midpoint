/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.event;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * Event payload asking the wizard to open the drawer with its operation results.
 *
 * @param target the AJAX target for adding components to re-render
 */
public record ShowOperationResultsEvent(AjaxRequestTarget target) {}
