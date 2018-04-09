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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.StringWorkBucketsBoundaryMarkingType.INTERVAL;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.StringWorkBucketsBoundaryMarkingType.PREFIX;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author mederly
 */
public class StringWorkSegmentationStrategy extends BaseWorkSegmentationStrategy {

	private static final Trace LOGGER = TraceManager.getTrace(StringWorkSegmentationStrategy.class);

	@NotNull private final StringWorkSegmentationType bucketsConfiguration;
	@NotNull private final StringWorkBucketsBoundaryMarkingType marking;
	@NotNull private final List<String> boundaries;

	private static final String OID_BOUNDARIES = "0-9a-f";

	public StringWorkSegmentationStrategy(@NotNull TaskWorkManagementType configuration, PrismContext prismContext) {
		super(prismContext);
		this.bucketsConfiguration = (StringWorkSegmentationType)
				TaskWorkStateTypeUtil.getWorkSegmentationConfiguration(configuration);
		this.marking = defaultIfNull(bucketsConfiguration.getComparisonMethod(), INTERVAL);
		this.boundaries = processBoundaries();
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
		WorkBucketType lastBucket = TaskWorkStateTypeUtil.getLastBucket(workState.getBucket());
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
		WorkBucketType lastBucket = TaskWorkStateTypeUtil.getLastBucket(workState.getBucket());
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
		assert boundaries.size() == currentIndices.size();

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < currentIndices.size(); i++) {
			sb.append(boundaries.get(i).charAt(currentIndices.get(i)));
		}
		return sb.toString();
	}

	@Override
	public Integer estimateNumberOfBuckets(@Nullable TaskWorkStateType workState) {
		int combinations = 1;
		for (String boundary : boundaries) {
			combinations *= boundary.length();
		}
		return marking == INTERVAL ? combinations+1 : combinations;
	}

	private List<String> processBoundaries() {
		List<String> configuredBoundaries;
		if (bucketsConfiguration instanceof OidWorkSegmentationType && bucketsConfiguration.getBoundaryCharacters().isEmpty()) {
			configuredBoundaries = singletonList(OID_BOUNDARIES);
		} else {
			configuredBoundaries = bucketsConfiguration.getBoundaryCharacters();
		}
		int depth = defaultIfNull(bucketsConfiguration.getDepth(), 1);
		List<String> expanded = configuredBoundaries.stream()
				.map(this::expand)
				.collect(Collectors.toList());
		List<String> rv = new ArrayList<>(expanded.size() * depth);
		for (int i = 0; i < depth; i++) {
			rv.addAll(expanded);
		}
		return rv;
	}

	private static class Scanner {
		final String string;
		int index;

		Scanner(String string) {
			this.string = string;
		}

		boolean hasNext() {
			return index < string.length();
		}

		// @pre hasNext()
		boolean isDash() {
			return string.charAt(index) == '-';
		}

		// @pre hasNext()
		public char next() {
			char c = string.charAt(index++);
			if (c != '\\') {
				return c;
			} else if (index != string.length()) {
				return string.charAt(index++);
			} else {
				throw new IllegalArgumentException("Boundary specification cannot end with '\\': " + string);
			}
		}
	}

	private String expand(String s) {
		StringBuilder sb = new StringBuilder();
		Scanner scanner = new Scanner(s);

		while (scanner.hasNext()) {
			if (scanner.isDash()) {
				if (sb.length() == 0) {
					throw new IllegalArgumentException("Boundary specification cannot start with '-': " + s);
				} else {
					scanner.next();
					if (!scanner.hasNext()) {
						throw new IllegalArgumentException("Boundary specification cannot end with '-': " + s);
					} else {
						appendFromTo(sb, sb.charAt(sb.length()-1), scanner.next());
					}
				}
			} else {
				sb.append(scanner.next());
			}
		}
		String expanded = sb.toString();
		if (marking == INTERVAL) {
			checkBoundary(expanded);
		}
		return expanded;
	}

	// this is a bit tricky: we do not know what matching rule will be used to execute the comparisons
	// but let's assume it will be consistent with the default ordering
	private void checkBoundary(String boundary) {
		for (int i = 1; i < boundary.length(); i++) {
			char before = boundary.charAt(i - 1);
			char after = boundary.charAt(i);
			if (before >= after) {
				LOGGER.warn("Boundary characters are not sorted in ascending order ({}); comparing '{}' and '{}'",
						boundary, before, after);
			}
		}
	}

	private void appendFromTo(StringBuilder sb, char fromExclusive, char toInclusive) {
		for (char c = (char) (fromExclusive+1); c <= toInclusive; c++) {
			sb.append(c);
		}
	}

	// just for testing
	@NotNull
	public List<String> getBoundaries() {
		return boundaries;
	}
}
