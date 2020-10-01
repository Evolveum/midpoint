/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work.segmentation.content;

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

    @NotNull
    protected ItemPath getDiscriminator(AbstractWorkSegmentationType configuration, AbstractWorkBucketContentType content) {
        ItemPathType discriminatorPathType = configuration.getDiscriminator();
        if (discriminatorPathType != null) {
            return discriminatorPathType.getItemPath();
        } else if (configuration instanceof OidWorkSegmentationType) {
            return ItemName.fromQName(PrismConstants.T_ID);     // fixme
        } else {
            throw new IllegalStateException("No buckets discriminator defined; bucket content = " + content);
        }
    }
}
