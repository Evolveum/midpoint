/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.List;

import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.datatype.XMLGregorianCalendar;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

/**
 * Prepares text output for notification purposes.
 *
 * This is only a facade for value and delta formatters.
 * It is probably used in various notification-related expressions so we'll keep it for some time.
 */
@Component
public class TextFormatter {

    @Autowired ValueFormatter valueFormatter;
    @Autowired DeltaFormatter deltaFormatter;

    public String formatAccountAttributes(ShadowType shadowType, List<ItemPath> hiddenAttributes, boolean showOperationalAttributes) {
        return valueFormatter.formatAccountAttributes(shadowType, hiddenAttributes, showOperationalAttributes);
    }

    public String formatObject(PrismObject object, List<ItemPath> hiddenPaths, boolean showOperationalAttributes) {
        return valueFormatter.formatObject(object, hiddenPaths, showOperationalAttributes);
    }

    @SuppressWarnings("unused")
    public String formatObjectModificationDelta(ObjectDelta<? extends Objectable> objectDelta, List<ItemPath> hiddenPaths,
            boolean showOperationalAttributes) {
        return deltaFormatter.formatObjectModificationDelta(objectDelta, hiddenPaths, showOperationalAttributes, null, null);
    }

    public String formatObjectModificationDelta(@NotNull ObjectDelta<? extends Objectable> objectDelta, List<ItemPath> hiddenPaths,
            boolean showOperationalAttributes, PrismObject<?> objectOld, PrismObject<?> objectNew) {
        return deltaFormatter.formatObjectModificationDelta(objectDelta, hiddenPaths, showOperationalAttributes, objectOld, objectNew);
    }
}
