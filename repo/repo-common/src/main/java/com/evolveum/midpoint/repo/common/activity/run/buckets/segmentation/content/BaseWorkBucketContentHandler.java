/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation.content;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkSegmentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OidWorkSegmentationType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseWorkBucketContentHandler implements WorkBucketContentHandler {

    @Autowired protected WorkBucketContentHandlerRegistry registry;
    @Autowired protected PrismContext prismContext;

    @NotNull ItemPath getDiscriminator(AbstractWorkSegmentationType configuration, AbstractWorkBucketContentType content) {
        ItemPathType discriminatorPathType = configuration.getDiscriminator();
        if (discriminatorPathType != null) {
            return discriminatorPathType.getItemPath();
        } else if (configuration instanceof OidWorkSegmentationType) {
            return ItemName.fromQName(PrismConstants.T_ID);
        } else {
            throw new IllegalStateException("No buckets discriminator defined; bucket content = " + content);
        }
    }
}
