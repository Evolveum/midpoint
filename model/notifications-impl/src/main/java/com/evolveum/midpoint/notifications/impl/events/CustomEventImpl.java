/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.events;

import com.evolveum.midpoint.prism.Safe;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.notifications.api.events.CustomEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class CustomEventImpl extends BaseEventImpl implements CustomEvent {

    private final String subtype;
    /**
     * Any object, e.g. PrismObject, any Item, any PrismValue, any real value. It can be even null.
     */
    private final Object object;
    @NotNull private final EventStatusType status;
    @NotNull private final EventOperationType operationType;

    public CustomEventImpl(
            LightweightIdentifierGenerator lightweightIdentifierGenerator,
            @Nullable String subtype,
            @Nullable Object object,
            @NotNull EventOperationType operationType,
            @NotNull EventStatusType status,
            String channel) {
        super(lightweightIdentifierGenerator);
        this.subtype = subtype;
        this.object = object;
        this.status = status;
        this.operationType = operationType;
        setChannel(channel);
    }

    @Override
    @NotNull
    @Safe
    public EventOperationType getOperationType() {
        return operationType;
    }

    @Override
    @NotNull
    @Safe
    public EventStatusType getStatus() {
        return status;
    }

    @Override
    @Safe
    public boolean isStatusType(EventStatusType eventStatus) {
        return status == eventStatus;
    }

    @Override
    @Safe
    public boolean isOperationType(EventOperationType eventOperation) {
        return this.operationType == eventOperation;
    }

    @Override
    @Safe
    public boolean isCategoryType(EventCategoryType eventCategory) {
        return eventCategory == EventCategoryType.CUSTOM_EVENT;
    }

    @Override
    @Nullable
    @Safe
    public String getSubtype() {
        return subtype;
    }

    @Override
    @Nullable
    @Safe // safe to get; the security is ensured by restrictions on methods that can be called on the object
    public Object getObject() {
        return object;
    }

    @Override
    @Safe
    public boolean isUserRelated() {
        if (object instanceof UserType) {
            return true;
        } else if (object instanceof PrismObject<?> prismObject) {
            return prismObject.getCompileTimeClass() != null
                    && UserType.class.isAssignableFrom(prismObject.getCompileTimeClass());
        } else {
            return false;
        }
    }

    @Override
    @Safe
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabel(sb, "subtype", subtype, indent + 1);
        return sb.toString();
    }
}
