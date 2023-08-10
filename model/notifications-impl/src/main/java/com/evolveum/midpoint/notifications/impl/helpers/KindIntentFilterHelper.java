/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.helpers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.ResourceObjectEvent;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

@Component
public class KindIntentFilterHelper extends BaseNotificationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(KindIntentFilterHelper.class);

    public boolean processEvent(
            ConfigurationItem<? extends BaseEventHandlerType> handlerConfig, EventProcessingContext<?> ctx) {

        List<ShadowKindType> kinds = handlerConfig.value().getObjectKind();
        List<String> intents = handlerConfig.value().getObjectIntent();

        if (kinds.isEmpty() && intents.isEmpty()) {
            return true;
        }

        if (!(ctx.event() instanceof ResourceObjectEvent resourceObjectEvent)) {
            return true; // or should we return false?
        }

        logStart(LOGGER, handlerConfig, ctx, DebugUtil.lazy(() -> "kinds: " + kinds + ", intents: " + intents));

        boolean retval = true;

        if (!kinds.isEmpty()) {
            retval = false;
            for (ShadowKindType shadowKindType : kinds) {
                if (shadowKindType == null) {
                    LOGGER.warn("Filtering on null shadowKindType; filter = " + handlerConfig);
                } else if (resourceObjectEvent.isShadowKind(shadowKindType)) {
                    retval = true;
                    break;
                }
            }
        }

        if (retval) {
            // now check the intent
            if (!intents.isEmpty()) {
                retval = false;
                for (String intent : intents) {
                    if (intent == null) {
                        LOGGER.warn("Filtering on null intent; filter = " + handlerConfig);
                    } else if (resourceObjectEvent.isShadowIntent(intent)) {
                        retval = true;
                        break;
                    }
                }
            }
        }

        logEnd(LOGGER, handlerConfig, ctx, retval);
        return retval;
    }
}
