/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.event;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * Event payload broadcast when a form component triggers an AJAX "change" update.
 *
 * <p>This record enables decoupled communication between form components and UI elements
 * that need to react to form changes. It is automatically created and broadcast by
 * {@link com.evolveum.midpoint.web.security.MidPointApplication} when an
 * {@link org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior} fires with the "change" event.</p>
 *
 * <p>Listeners receive this event by overriding {@link Component#onEvent(org.apache.wicket.event.IEvent)}
 * and checking the payload type.</p>
 *
 * @param AjaxRequestTarget the AJAX target for adding components to re-render
 * @param component         the form component that triggered the event
 */
public record FormComponentUpdatingEvent (
        AjaxRequestTarget AjaxRequestTarget,
        Component component
) {}
