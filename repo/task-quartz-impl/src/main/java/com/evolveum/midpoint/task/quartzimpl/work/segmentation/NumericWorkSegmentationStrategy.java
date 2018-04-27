/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.quartzimpl.work.segmentation;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.TaskWorkStateTypeUtil;
import com.evolveum.midpoint.task.quartzimpl.work.BaseWorkSegmentationStrategy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Implements work state management strategy based on numeric identifier intervals.
 *
 * @author mederly
 */
//<NumericIntervalWorkBucketContentType, NumericIntervalWorkBucketsConfigurationType>
public class NumericWorkSegmentationStrategy extends BaseWorkSegmentationStrategy {

	@NotNull private final TaskWorkManagementType configuration;
	@NotNull private final NumericWorkSegmentationType bucketsConfiguration;

	public NumericWorkSegmentationStrategy(@NotNull TaskWorkManagementType configuration,
			PrismContext prismContext) {
		super(prismContext);
		this.configuration = configuration;
		this.bucketsConfiguration = (NumericWorkSegmentationType)
				TaskWorkStateTypeUtil.getWorkSegmentationConfiguration(configuration);
	}

	@NotNull
	@Override
	protected List<NumericIntervalWorkBucketContentType> createAdditionalBuckets(TaskWorkStateType workState) {
		BigInteger bucketSize = getOrComputeBucketSize();
		BigInteger from = getFrom();
		BigInteger to = getOrComputeTo();

		WorkBucketType lastBucket = TaskWorkStateTypeUtil.getLastBucket(workState.getBucket());
		NumericIntervalWorkBucketContentType newContent;
		if (lastBucket != null) {
			if (!(lastBucket.getContent() instanceof NumericIntervalWorkBucketContentType)) {
				throw new IllegalStateException("Null or unsupported bucket content: " + lastBucket.getContent());
			}
			NumericIntervalWorkBucketContentType lastContent = (NumericIntervalWorkBucketContentType) lastBucket.getContent();
			if (lastContent.getTo() == null || lastContent.getTo().compareTo(to) >= 0) {
				return emptyList();            // no more buckets
			}
			BigInteger newEnd = lastContent.getTo().add(bucketSize);
			if (newEnd.compareTo(to) > 0) {
				newEnd = to;
			}
			newContent = new NumericIntervalWorkBucketContentType()
					.from(lastContent.getTo())
					.to(newEnd);
		} else {
			newContent = new NumericIntervalWorkBucketContentType()
					.from(from)
					.to(from.add(bucketSize));
		}
		return singletonList(newContent);
	}

	@NotNull
	private BigInteger getOrComputeBucketSize() {
		if (bucketsConfiguration.getBucketSize() != null) {
			return bucketsConfiguration.getBucketSize();
		} else if (bucketsConfiguration.getTo() != null && bucketsConfiguration.getNumberOfBuckets() != null) {
			return bucketsConfiguration.getTo().subtract(getFrom()).divide(BigInteger.valueOf(bucketsConfiguration.getNumberOfBuckets()));
		} else {
			throw new IllegalStateException("Neither numberOfBuckets nor to + bucketSize is specified");
		}
	}

	@NotNull
	private BigInteger getFrom() {
		return bucketsConfiguration.getFrom() != null ? bucketsConfiguration.getFrom() : BigInteger.ZERO;
	}

	@NotNull
	private BigInteger getOrComputeTo() {
		if (bucketsConfiguration.getTo() != null) {
			return bucketsConfiguration.getTo();
		} else if (bucketsConfiguration.getBucketSize() != null && bucketsConfiguration.getNumberOfBuckets() != null) {
			return getFrom()
					.add(bucketsConfiguration.getBucketSize()
							.multiply(BigInteger.valueOf(bucketsConfiguration.getNumberOfBuckets())));
		} else {
			throw new IllegalStateException("Neither upper bound nor bucketSize + numberOfBucket specified");
		}
	}

	@NotNull
	private BigInteger computeIntervalSpan() {
		return getOrComputeTo().subtract(getFrom());
	}

	@Override
	public Integer estimateNumberOfBuckets(@Nullable TaskWorkStateType workState) {
		if (bucketsConfiguration.getNumberOfBuckets() != null) {
			return bucketsConfiguration.getNumberOfBuckets();
		} else if (bucketsConfiguration.getTo() != null && bucketsConfiguration.getBucketSize() != null) {
			BigInteger[] divideAndRemainder = computeIntervalSpan().divideAndRemainder(bucketsConfiguration.getBucketSize());
			if (BigInteger.ZERO.equals(divideAndRemainder[1])) {
				return divideAndRemainder[0].intValue();
			} else {
				return divideAndRemainder[0].intValue() + 1;
			}
		} else {
			throw new IllegalStateException("Neither numberOfBuckets nor to + bucketSize is specified");
		}
	}
}
