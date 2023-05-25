/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.buckets.segmentation;

import com.evolveum.midpoint.repo.common.activity.run.buckets.BaseBucketContentFactory;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.StringWorkBucketsBoundaryMarkingType.INTERVAL;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Creates content of string-based buckets (defined by {@link StringWorkSegmentationType} and its subtypes).
 */
public class StringBucketContentFactory extends BaseBucketContentFactory<StringWorkSegmentationType> {

    private static final Trace LOGGER = TraceManager.getTrace(StringBucketContentFactory.class);

    @NotNull private final StringWorkBucketsBoundaryMarkingType marking;
    @NotNull private final List<String> boundaries;

    private static final String OID_BOUNDARIES = "0-9a-f";

    StringBucketContentFactory(@NotNull StringWorkSegmentationType segmentationConfig) {
        super(segmentationConfig);
        this.marking = defaultIfNull(segmentationConfig.getComparisonMethod(), INTERVAL);
        this.boundaries = processBoundaries();
    }

    /**
     * This is to allow instantiation even for subtypes of {@link StringWorkSegmentationType}.
     * It is a price we pay for using simple {@link Class#getConstructor(Class[])} to look for
     * the applicable constructor.
     */
    StringBucketContentFactory(@NotNull OidWorkSegmentationType segmentationConfig) {
        this((StringWorkSegmentationType) segmentationConfig);
    }

    @Override
    public AbstractWorkBucketContentType createNextBucketContent(AbstractWorkBucketContentType lastBucketContent,
            Integer lastBucketSequentialNumber) {
        switch (marking) {
            case INTERVAL: return createAdditionalIntervalBucket(lastBucketContent, lastBucketSequentialNumber);
            case PREFIX: return createAdditionalPrefixBucket(lastBucketContent, lastBucketSequentialNumber);
            case EXACT_MATCH: return createAdditionalExactMatchBucket(lastBucketContent, lastBucketSequentialNumber);
            default: throw new AssertionError("unsupported marking: " + marking);
        }
    }

    private AbstractWorkBucketContentType createAdditionalIntervalBucket(AbstractWorkBucketContentType lastBucketContent,
            Integer lastBucketSequentialNumber) {
        String lastBoundary;
        if (lastBucketSequentialNumber != null) {
            if (!(lastBucketContent instanceof StringIntervalWorkBucketContentType)) {
                throw new IllegalStateException("Null or unsupported bucket content: " + lastBucketContent);
            }
            StringIntervalWorkBucketContentType lastContent = (StringIntervalWorkBucketContentType) lastBucketContent;
            if (lastContent.getTo() == null) {
                return null;
            }
            lastBoundary = lastContent.getTo();
        } else {
            lastBoundary = null;
        }
        return new StringIntervalWorkBucketContentType()
                .from(lastBoundary)
                .to(computeNextBoundary(lastBoundary));
    }

    private AbstractWorkBucketContentType createAdditionalPrefixBucket(AbstractWorkBucketContentType lastBucketContent,
            Integer lastBucketSequentialNumber) {
        String lastBoundary;
        if (lastBucketSequentialNumber != null) {
            if (!(lastBucketContent instanceof StringPrefixWorkBucketContentType)) {
                throw new IllegalStateException("Null or unsupported bucket content: " + lastBucketContent);
            }
            StringPrefixWorkBucketContentType lastContent = (StringPrefixWorkBucketContentType) lastBucketContent;
            if (lastContent.getPrefix().size() > 1) {
                throw new IllegalStateException("Multiple prefixes are not supported now: " + lastContent);
            } else if (lastContent.getPrefix().isEmpty()) {
                return null;
            } else {
                lastBoundary = lastContent.getPrefix().get(0);
            }
        } else {
            lastBoundary = null;
        }
        String nextBoundary = computeNextBoundary(lastBoundary);
        if (nextBoundary != null) {
            return new StringPrefixWorkBucketContentType()
                    .prefix(nextBoundary);
        } else {
            return null;
        }
    }

    private AbstractWorkBucketContentType createAdditionalExactMatchBucket(AbstractWorkBucketContentType lastBucketContent,
            Integer lastBucketSequentialNumber) {
        String lastBoundary;
        if (lastBucketSequentialNumber != null) {
            if (!(lastBucketContent instanceof StringValueWorkBucketContentType)) {
                throw new IllegalStateException("Null or unsupported bucket content: " + lastBucketContent);
            }
            StringValueWorkBucketContentType lastContent = (StringValueWorkBucketContentType) lastBucketContent;
            if (lastContent.getValue().size() > 1) {
                throw new IllegalStateException("Multiple values are not supported now: " + lastContent);
            } else if (lastContent.getValue().isEmpty()) {
                return null;
            } else {
                lastBoundary = lastContent.getValue().get(0);
            }
        } else {
            lastBoundary = null;
        }
        String nextBoundary = computeNextBoundary(lastBoundary);
        if (nextBoundary != null) {
            return new StringValueWorkBucketContentType()
                    .value(nextBoundary);
        } else {
            return null;
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
    public Integer estimateNumberOfBuckets() {
        int combinations = 1;
        for (String boundary : boundaries) {
            combinations *= boundary.length();
        }
        return marking == INTERVAL ? combinations+1 : combinations;
    }

    private List<String> processBoundaries() {
        List<String> expanded = getConfiguredBoundaries().stream()
                .map(this::expand)
                .collect(Collectors.toList());
        int depth = defaultIfNull(segmentationConfig.getDepth(), 1);
        List<String> rv = new ArrayList<>(expanded.size() * depth);
        for (int i = 0; i < depth; i++) {
            rv.addAll(expanded);
        }
        return rv;
    }

    private List<String> getConfiguredBoundaries() {
        if (!segmentationConfig.getBoundary().isEmpty()) {
            return new Boundaries(segmentationConfig.getBoundary())
                    .getConfiguredBoundaries();
        } else if (segmentationConfig instanceof OidWorkSegmentationType) {
            return singletonList(OID_BOUNDARIES);
        } else {
            return emptyList();
        }
    }

    private static class Scanner {
        private final String string;
        private int index;

        private Scanner(String string) {
            this.string = string;
        }

        private boolean hasNext() {
            return index < string.length();
        }

        // @pre hasNext()
        private boolean isDash() {
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

    private static class Boundaries {

        private final List<BoundarySpecificationType> specifications;
        private final List<String> configuredBoundaries = new ArrayList<>();

        private Boundaries(List<BoundarySpecificationType> specifications) {
            this.specifications = specifications;
        }

        public List<String> getConfiguredBoundaries() {
            for (BoundarySpecificationType specification : specifications) {
                process(specification);
            }
            checkConsistency();
            return configuredBoundaries;
        }

        private void process(BoundarySpecificationType specification) {
            if (specification.getPosition().isEmpty()) {
                configuredBoundaries.add(specification.getCharacters());
                return;
            }
            for (Integer position : specification.getPosition()) {
                argCheck(position != null, "Position is null in %s", specification);
                extendIfNeeded(position);
                set(position, specification.getCharacters());
            }
        }

        /**
         * @param position User-visible position, i.e. starts at 1 (not at zero)!
         */
        private void extendIfNeeded(int position) {
            int index = position - 1;
            while (configuredBoundaries.size() <= index) {
                configuredBoundaries.add(null);
            }
        }

        /**
         * @param position User-visible position, i.e. starts at 1 (not at zero)!
         */
        private void set(int position, String characters) {
            int index = position - 1;
            assert configuredBoundaries.size() > index;
            argCheck(configuredBoundaries.get(index) == null,
                    "Boundary characters for position %d defined more than once: %s", position, configuredBoundaries);
            configuredBoundaries.set(index, characters);
        }

        private void checkConsistency() {
            for (int i = 0; i < configuredBoundaries.size(); i++) {
                String configuredBoundary = configuredBoundaries.get(i);
                argCheck(configuredBoundary != null, "Boundary characters for position %s are not defined: %s",
                        i, configuredBoundaries);
            }
        }
    }
}
