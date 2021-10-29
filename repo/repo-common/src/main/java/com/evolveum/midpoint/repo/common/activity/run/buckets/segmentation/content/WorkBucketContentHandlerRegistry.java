/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for creation of configured work bucket content handlers.
 */
@Component
public class WorkBucketContentHandlerRegistry {

    private final Map<Class<? extends AbstractWorkBucketContentType>, WorkBucketContentHandler> handlers = new HashMap<>();

    @NotNull
    public WorkBucketContentHandler getHandler(AbstractWorkBucketContentType content) {
        WorkBucketContentHandler handler = handlers.get(content != null ? content.getClass() : null);
        if (handler != null) {
            return handler;
        } else {
            throw new IllegalStateException("Unknown or unsupported work bucket content type: " + content);
        }
    }

    public void registerHandler(Class<? extends AbstractWorkBucketContentType> contentClass, WorkBucketContentHandler handler) {
        handlers.put(contentClass, handler);
    }
}
