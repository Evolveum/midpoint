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

package com.evolveum.midpoint.task.quartzimpl.work.strategy;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.task.quartzimpl.work.BaseWorkBucketPartitioningStrategy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Implements work state management strategy based on numeric identifier intervals.
 *
 * @author mederly
 */
//<NumericIntervalWorkBucketContentType, NumericIntervalWorkBucketsConfigurationType>
public class NumericIntervalWorkBucketPartitioningStrategy extends BaseWorkBucketPartitioningStrategy {

	@NotNull private final TaskWorkStateConfigurationType configuration;
	@NotNull private final NumericIntervalWorkBucketsConfigurationType bucketsConfiguration;

	public NumericIntervalWorkBucketPartitioningStrategy(@NotNull TaskWorkStateConfigurationType configuration,
			PrismContext prismContext) {
		super(prismContext);
		this.configuration = configuration;
		this.bucketsConfiguration = getBucketsConfiguration();
	}

//	private void checkConsistency() {
//		BigInteger bucketSize = getOrComputeBucketSize();
//		BigInteger to = getOrComputeTo();
//
//		if (bucketSize != null && to == null) {
//			throw new IllegalStateException("Inconsistent configuration: bucket size is known but upper bound is not: " + configuration);
//		} else if (bucketSize == null && to != null) {
//			throw new IllegalStateException("Inconsistent configuration: upper bound is known but bucket size is not: " + configuration);
//		}
//	}

	@NotNull
	@Override
	protected List<NumericIntervalWorkBucketContentType> createAdditionalBuckets(TaskWorkStateType workState) {
		BigInteger bucketSize = getOrComputeBucketSize();
		BigInteger from = getFrom();
		BigInteger to = getOrComputeTo();

		WorkBucketType lastBucket = TaskTypeUtil.getLastBucket(workState.getBucket());
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
					.to(bucketSize);
		}
		return singletonList(newContent);
	}

	@NotNull
	private NumericIntervalWorkBucketsConfigurationType getBucketsConfiguration() {
		if (configuration.getNumericIntervalBuckets() == null) {
			throw new IllegalStateException("No buckets content configuration");
		} else {
			return configuration.getNumericIntervalBuckets();       // TODO use generic configuration if there is any
		}
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

//	@NotNull
//	private BigInteger computeIntervalSpan() throws SchemaException {
//		return getOrComputeTo().subtract(getFrom());
//	}
//
//	public int getOrComputeNumberOfBuckets() throws SchemaException {
//		if (configuration.getNumberOfBuckets() != null) {
//			return configuration.getNumberOfBuckets();
//		} else if (configuration.getTo() != null && configuration.getBucketSize() != null) {
//			BigInteger[] divideAndRemainder = computeIntervalSpan().divideAndRemainder(configuration.getBucketSize());
//			if (BigInteger.ZERO.equals(divideAndRemainder[1])) {
//				return divideAndRemainder[0].intValue();
//			} else {
//				return divideAndRemainder[0].intValue() + 1;
//			}
//		} else {
//			throw new SchemaException("Neither numberOfBuckets nor to + bucketSize is specified");
//		}
//	}

	// experimental implementation TODO
	@Override
	public List<ObjectFilter> createSpecificFilters(WorkBucketType bucket, Class<? extends ObjectType> type,
			Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider) {
		NumericIntervalWorkBucketsConfigurationType bucketsConfig = getBucketsConfiguration();
		NumericIntervalWorkBucketContentType bucketContent = (NumericIntervalWorkBucketContentType) bucket.getContent();

		if (hasNoBoundaries(bucketContent)) {
			return new ArrayList<>();
		}
		if (bucketsConfig == null) {
			throw new IllegalStateException("No buckets configuration but having defined bucket content: " + bucket);
		}
		if (bucketsConfig.getDiscriminator() == null) {
			throw new IllegalStateException("No buckets discriminator defined; bucket = " + bucket);
		}
		ItemPath identifier = bucketsConfig.getDiscriminator().getItemPath();
		ItemDefinition<?> identifierDefinition = itemDefinitionProvider != null ? itemDefinitionProvider.apply(identifier) : null;
		List<ObjectFilter> filters = new ArrayList<>();
		if (bucketContent.getFrom() != null) {
			if (identifierDefinition != null) {
				filters.add(QueryBuilder.queryFor(type, prismContext)
						.item(identifier, identifierDefinition).ge(bucketContent.getFrom())
						.buildFilter());
			} else {
				filters.add(QueryBuilder.queryFor(type, prismContext)
						.item(identifier).ge(bucketContent.getFrom())
						.buildFilter());
			}
		}
		if (bucketContent.getTo() != null) {
			if (identifierDefinition != null) {
				filters.add(QueryBuilder.queryFor(type, prismContext)
						.item(identifier, identifierDefinition).lt(bucketContent.getTo())
						.buildFilter());
			} else {
				filters.add(QueryBuilder.queryFor(type, prismContext)
						.item(identifier).lt(bucketContent.getTo())
						.buildFilter());
			}
		}
		return filters;
	}

	private boolean hasNoBoundaries(NumericIntervalWorkBucketContentType bucketContent) {
		return bucketContent == null || isNullOrZero(bucketContent.getFrom()) && bucketContent.getTo() == null;
	}

	private boolean isNullOrZero(BigInteger i) {
		return i == null || BigInteger.ZERO.equals(i);
	}
}
