/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.event;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;

/** Broadcast after a value is successfully removed from an item wrapper. */
public record ItemValueRemovedEvent(ItemWrapper<?, ?> item, AjaxRequestTarget target) {
}
