/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author mederly
 */
public class CustomEvent extends BaseEvent {

    private static final Trace LOGGER = TraceManager.getTrace(CustomEvent.class);

    private final String subtype;
    /**
     * Any object, e.g. PrismObject, any Item, any PrismValue, any real value. It can be even null.
     */
    private final Object object;
    @NotNull private final EventStatusType status;
    @NotNull private final EventOperationType operationType;

    public CustomEvent(LightweightIdentifierGenerator lightweightIdentifierGenerator, @Nullable String subtype, @Nullable EventHandlerType adHocHandler,
            @Nullable Object object, @NotNull EventOperationType operationType, @NotNull EventStatusType status, String channel) {
        super(lightweightIdentifierGenerator, adHocHandler);
        this.subtype = subtype;
        this.object = object;
        this.status = status;
        this.operationType = operationType;
        setChannel(channel);
    }

    @NotNull
    public EventOperationType getOperationType() {
        return operationType;
    }

    @NotNull
    public EventStatusType getStatus() {
        return status;
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatusType) {
        return status == eventStatusType;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperationType) {
        return this.operationType == eventOperationType;
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.CUSTOM_EVENT;
    }

    @Nullable
    public String getSubtype() {
        return subtype;
    }

    @Nullable
    public Object getObject() {
        return object;
    }

    @Override
    public boolean isRelatedToItem(ItemPath itemPath) {
        // TODO implement if needed
        return false;
    }

    @Override
    public boolean isUserRelated() {
        if (object instanceof UserType) {
            return true;
        } else if (object instanceof PrismObject) {
            PrismObject prismObject = (PrismObject) object;
            return prismObject.getCompileTimeClass() != null && UserType.class.isAssignableFrom(prismObject.getCompileTimeClass());
        } else {
            return false;
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabel(sb, "subtype", subtype, indent + 1);
        return sb.toString();
    }

}
