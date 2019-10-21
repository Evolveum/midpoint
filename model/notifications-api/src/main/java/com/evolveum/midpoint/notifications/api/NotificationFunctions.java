/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.List;

/**
 * Useful functions to be used in notifications scripts (including velocity templates).
 *
 * @author mederly
 */
public interface NotificationFunctions {

    String getShadowName(PrismObject<? extends ShadowType> shadow);

    String getPlaintextPasswordFromDelta(ObjectDelta delta);

    String getContentAsFormattedList(Event event, boolean showSynchronizationItems, boolean showAuxiliaryAttributes);

    List<ItemPath> getSynchronizationPaths();

    List<ItemPath> getAuxiliaryPaths();

    // TODO: polish this method
    // TODO indicate somehow if password was erased from the focus
    // We should (probably) return only a value if it has been (successfully) written to the focus.
    String getFocusPasswordFromEvent(ModelEvent modelEvent);

}
