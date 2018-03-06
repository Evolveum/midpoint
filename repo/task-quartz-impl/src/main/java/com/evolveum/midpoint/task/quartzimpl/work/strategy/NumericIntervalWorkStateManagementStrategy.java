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
import com.evolveum.midpoint.task.quartzimpl.work.BaseWorkStateManagementStrategy;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;

/**
 * Implements work state management strategy based on numeric identifier intervals.
 *
 * @author mederly
 */
public class NumericIntervalWorkStateManagementStrategy extends BaseWorkStateManagementStrategy<NumericIntervalWorkBucketType> {

	private final NumericIntervalWorkStateManagementConfigurationType configuration;

	public NumericIntervalWorkStateManagementStrategy(NumericIntervalWorkStateManagementConfigurationType configuration,
			PrismContext prismContext) {
		super(prismContext);
		this.configuration = configuration;
	}

	@Override
	protected GetBucketResult.NewBuckets createAdditionalBuckets(TaskWorkStateType workState) throws SchemaException {
		BigInteger bucketSize = getOrComputeBucketSize();
		BigInteger from = getFrom();
		BigInteger to = getOrComputeTo();
		if (workState.getBucket().isEmpty()) {
			NumericIntervalWorkBucketType newBucket = new NumericIntervalWorkBucketType(prismContext)
					.sequentialNumber(1)
					.from(from)
					.to(from.add(bucketSize))
					.state(WorkBucketStateType.READY);
			return new GetBucketResult.NewBuckets(singletonList(newBucket));
		} else {
			NumericIntervalWorkBucketType lastBucket = getLastBucket(cast(workState.getBucket()));
			if (lastBucket.getTo() == null || lastBucket.getTo().compareTo(to) >= 0) {
				// no more buckets
				return null;
			}
			BigInteger newEnd = lastBucket.getTo().add(bucketSize);
			if (newEnd.compareTo(to) > 0) {
				newEnd = to;
			}
			NumericIntervalWorkBucketType newBucket = new NumericIntervalWorkBucketType(prismContext)
					.sequentialNumber(lastBucket.getSequentialNumber() + 1)
					.from(lastBucket.getTo())
					.to(newEnd)
					.state(WorkBucketStateType.READY);
			return new GetBucketResult.NewBuckets(singletonList(newBucket));
		}
	}

	@NotNull
	private BigInteger getOrComputeBucketSize() throws SchemaException {
		if (configuration.getBucketSize() != null) {
			return configuration.getBucketSize();
		} else if (configuration.getTo() != null && configuration.getNumberOfBuckets() != null) {
			return computeIntervalSpan().divide(BigInteger.valueOf(getOrComputeNumberOfBuckets()));
		} else {
			throw new SchemaException("Neither bucketSize nor to + numberOfBuckets specified");
		}
	}

	@NotNull
	private BigInteger getFrom() {
		return configuration.getFrom() != null ? configuration.getFrom() : BigInteger.ZERO;
	}

	@NotNull
	private BigInteger getOrComputeTo() throws SchemaException {
		if (configuration.getTo() != null) {
			return configuration.getTo();
		} else if (configuration.getBucketSize() != null && configuration.getNumberOfBuckets() != null) {
			return configuration.getBucketSize().multiply(BigInteger.valueOf(configuration.getNumberOfBuckets()));
		} else {
			throw new SchemaException("Neither upper bound nor bucketSize + numberOfBucket specified");
		}
	}

	@NotNull
	private BigInteger computeIntervalSpan() throws SchemaException {
		return getOrComputeTo().subtract(getFrom());
	}

	public int getOrComputeNumberOfBuckets() throws SchemaException {
		if (configuration.getNumberOfBuckets() != null) {
			return configuration.getNumberOfBuckets();
		} else if (configuration.getTo() != null && configuration.getBucketSize() != null) {
			BigInteger[] divideAndRemainder = computeIntervalSpan().divideAndRemainder(configuration.getBucketSize());
			if (BigInteger.ZERO.equals(divideAndRemainder[1])) {
				return divideAndRemainder[0].intValue();
			} else {
				return divideAndRemainder[0].intValue() + 1;
			}
		} else {
			throw new SchemaException("Neither numberOfBuckets nor to + bucketSize is specified");
		}
	}

	@NotNull
	private NumericIntervalWorkBucketType getLastBucket(List<NumericIntervalWorkBucketType> buckets) {
		assert !buckets.isEmpty();
		NumericIntervalWorkBucketType lastBucket = null;
		for (NumericIntervalWorkBucketType bucket : buckets) {
			if (lastBucket == null || lastBucket.getSequentialNumber() < bucket.getSequentialNumber()) {
				lastBucket = bucket;
			}
		}
		return lastBucket;
	}

	// experimental implementation TODO
	@Override
	public List<ObjectFilter> createSpecificFilters(AbstractWorkBucketType bucket, Class<? extends ObjectType> type,
			Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider) {
		NumericIntervalWorkBucketType numBucket = (NumericIntervalWorkBucketType) bucket;
		TaskQueryTailoringSpecificationType qt = configuration.getQueryTailoring();
		if (qt == null || qt.getIdentifier() == null) {
			return new ArrayList<>();
		}
		ItemPath identifier = qt.getIdentifier().getItemPath();
		ItemDefinition<?> identifierDefinition = itemDefinitionProvider != null ? itemDefinitionProvider.apply(identifier) : null;
		List<ObjectFilter> filters = new ArrayList<>();
		if (numBucket.getFrom() != null) {
			if (identifierDefinition != null) {
				filters.add(QueryBuilder.queryFor(type, prismContext)
						.item(identifier, identifierDefinition).ge(numBucket.getFrom())
						.buildFilter());
			} else {
				filters.add(QueryBuilder.queryFor(type, prismContext)
						.item(identifier).ge(numBucket.getFrom())
						.buildFilter());
			}
		}
		if (numBucket.getTo() != null) {
			if (identifierDefinition != null) {
				filters.add(QueryBuilder.queryFor(type, prismContext)
						.item(identifier, identifierDefinition).lt(numBucket.getTo())
						.buildFilter());
			} else {
				filters.add(QueryBuilder.queryFor(type, prismContext)
						.item(identifier).lt(numBucket.getTo())
						.buildFilter());
			}
		}
		return filters;
	}
}
