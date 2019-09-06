/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.work.segmentation;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.quartzimpl.work.BaseWorkSegmentationStrategy;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkManagementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWorkStateType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Implements work state "segmentation" into single null work bucket.
 *
 * @author mederly
 */
public class SingleNullWorkSegmentationStrategy extends BaseWorkSegmentationStrategy {

	@SuppressWarnings("unused")
	public SingleNullWorkSegmentationStrategy(TaskWorkManagementType configuration,
			PrismContext prismContext) {
		super(configuration, prismContext);
	}

	@NotNull
	@Override
	protected List<AbstractWorkBucketContentType> createAdditionalBuckets(TaskWorkStateType workState) {
		if (workState.getBucket().isEmpty()) {
			return singletonList(null);
		} else {
			return emptyList();
		}
	}

	@Override
	protected AbstractWorkBucketContentType createAdditionalBucket(AbstractWorkBucketContentType lastBucketContent,
			Integer lastBucketSequentialNumber) throws SchemaException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer estimateNumberOfBuckets(@Nullable TaskWorkStateType workState) {
		return 1;
	}
}
