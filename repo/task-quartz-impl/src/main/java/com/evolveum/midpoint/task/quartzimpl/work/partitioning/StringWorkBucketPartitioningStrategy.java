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

package com.evolveum.midpoint.task.quartzimpl.work.partitioning;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.task.quartzimpl.work.BaseWorkBucketPartitioningStrategy;
import com.evolveum.midpoint.task.quartzimpl.work.WorkBucketUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.StringWorkBucketsBoundaryMarkingType.INTERVAL;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.StringWorkBucketsBoundaryMarkingType.PREFIX;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * @author mederly
 */
public class StringWorkBucketPartitioningStrategy extends BaseWorkBucketPartitioningStrategy {

	@NotNull private final StringWorkSegmentationType bucketsConfiguration;
	@NotNull private final StringWorkBucketsBoundaryMarkingType marking;

	public StringWorkBucketPartitioningStrategy(@NotNull TaskWorkManagementType configuration,
			PrismContext prismContext) {
		super(prismContext);
		this.bucketsConfiguration = (StringWorkSegmentationType)
				WorkBucketUtil.getWorkBucketsConfiguration(configuration);
		this.marking = ObjectUtils.defaultIfNull(bucketsConfiguration.getComparisonMethod(), INTERVAL);
	}

	@NotNull
	@Override
	protected List<? extends AbstractWorkBucketContentType> createAdditionalBuckets(TaskWorkStateType workState) {
		if (marking == INTERVAL) {
			return createAdditionalIntervalBuckets(workState);
		} else if (marking == PREFIX) {
			return createAdditionalPrefixBuckets(workState);
		} else {
			throw new AssertionError("unsupported marking: " + marking);
		}
	}

	private List<? extends AbstractWorkBucketContentType> createAdditionalIntervalBuckets(TaskWorkStateType workState) {
		WorkBucketType lastBucket = TaskTypeUtil.getLastBucket(workState.getBucket());
		String lastBoundary;
		if (lastBucket != null) {
			if (!(lastBucket.getContent() instanceof StringIntervalWorkBucketContentType)) {
				throw new IllegalStateException("Null or unsupported bucket content: " + lastBucket.getContent());
			}
			StringIntervalWorkBucketContentType lastContent = (StringIntervalWorkBucketContentType) lastBucket.getContent();
			if (lastContent.getTo() == null) {
				return emptyList();
			}
			lastBoundary = lastContent.getTo();
		} else {
			lastBoundary = null;
		}
		StringIntervalWorkBucketContentType nextBucket =
				new StringIntervalWorkBucketContentType()
						.from(lastBoundary)
						.to(computeNextBoundary(lastBoundary));
		return singletonList(nextBucket);
	}

	private List<? extends AbstractWorkBucketContentType> createAdditionalPrefixBuckets(TaskWorkStateType workState) {
		WorkBucketType lastBucket = TaskTypeUtil.getLastBucket(workState.getBucket());
		String lastBoundary;
		if (lastBucket != null) {
			if (!(lastBucket.getContent() instanceof StringPrefixWorkBucketContentType)) {
				throw new IllegalStateException("Null or unsupported bucket content: " + lastBucket.getContent());
			}
			StringPrefixWorkBucketContentType lastContent = (StringPrefixWorkBucketContentType) lastBucket.getContent();
			if (lastContent.getPrefix().size() > 1) {
				throw new IllegalStateException("Multiple prefixes are not supported now: " + lastContent);
			} else if (lastContent.getPrefix().isEmpty()) {
				return emptyList();
			} else {
				lastBoundary = lastContent.getPrefix().get(0);
			}
		} else {
			lastBoundary = null;
		}
		String nextBoundary = computeNextBoundary(lastBoundary);
		if (nextBoundary != null) {
			StringPrefixWorkBucketContentType nextBucket =
					new StringPrefixWorkBucketContentType()
							.prefix(nextBoundary);
			return singletonList(nextBucket);
		} else {
			return emptyList();
		}
	}

	private String computeNextBoundary(String lastBoundary) {
		List<Integer> currentIndices = stringToIndices(lastBoundary);
		if (incrementIndices(currentIndices)) {
			return indicesToString(currentIndices);
		} else {
			return null;
		}
	}

	@NotNull
	private List<Integer> stringToIndices(String lastBoundary) {
		List<String> boundaries = bucketsConfiguration.getBoundaryCharacters();
		List<Integer> currentIndices = new ArrayList<>();
		if (lastBoundary == null) {
			for (int i = 0; i < boundaries.size(); i++) {
				if (i < boundaries.size() - 1) {
					currentIndices.add(0);
				} else {
					currentIndices.add(-1);
				}
			}
		} else {
			if (lastBoundary.length() != boundaries.size()) {
				throw new IllegalStateException("Unexpected length of lastBoundary ('" + lastBoundary + "'): "
						+ lastBoundary.length() + ", expected " + boundaries.size());
			}
			for (int i = 0; i < lastBoundary.length(); i++) {
				int index = boundaries.get(i).indexOf(lastBoundary.charAt(i));
				if (index < 0) {
					throw new IllegalStateException("Illegal character at position " + (i+1) + " of lastBoundary ("
							+ lastBoundary + "): expected one of '" + boundaries.get(i) + "'");
				}
				currentIndices.add(index);
			}
		}
		return currentIndices;
	}

	// true if the new state is a valid one
	private boolean incrementIndices(List<Integer> currentIndices) {
		List<String> boundaries = bucketsConfiguration.getBoundaryCharacters();
		assert boundaries.size() == currentIndices.size();

		for (int i = currentIndices.size() - 1; i >= 0; i--) {
			int nextValue = currentIndices.get(i) + 1;
			if (nextValue < boundaries.get(i).length()) {
				currentIndices.set(i, nextValue);
				return true;
			} else {
				currentIndices.set(i, 0);
			}
		}
		return false;
	}

	private String indicesToString(List<Integer> currentIndices) {
		List<String> boundaries = bucketsConfiguration.getBoundaryCharacters();
		assert boundaries.size() == currentIndices.size();

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < currentIndices.size(); i++) {
			sb.append(boundaries.get(i).charAt(currentIndices.get(i)));
		}
		return sb.toString();
	}
}
