/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.match;

import com.evolveum.midpoint.model.impl.correlator.idmatch.IdMatchService;
import com.evolveum.midpoint.model.impl.correlator.idmatch.MatchingResult;
import com.evolveum.midpoint.model.impl.correlator.idmatch.PotentialMatch;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.model.impl.correlator.AbstractCorrelatorOrMatcherTest.*;
import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A dummy implementation of {@link IdMatchService} used to test IdMatchCorrelator without actual ID Match Service.
 */
public class DummyIdMatchServiceImpl implements IdMatchService {

    /**
     * Current records.
     */
    @NotNull private final List<Record> records = new ArrayList<>();

    @NotNull private final AtomicInteger matchIdCounter = new AtomicInteger();

    /**
     * The algorithm for matching:
     *
     * - If familyName, dateOfBirth, nationalId exactly match -> the this is 100% automatic match (regardless given name)
     * - If either nationalId matches (regardless of the rest), or givenName + familyName + dateOfBirth match (regardless
     * of nationalId), then this is an approximate match
     * - otherwise no match is reported
     */
    @Override
    public @NotNull MatchingResult executeMatch(@NotNull ShadowAttributesType attributes, @NotNull OperationResult result) {
        String givenName = getValue(attributes, ATTR_GIVEN_NAME);
        String familyName = getValue(attributes, ATTR_FAMILY_NAME);
        String dateOfBirth = getValue(attributes, ATTR_DATE_OF_BIRTH);
        String nationalId = getValue(attributes, ATTR_NATIONAL_ID);

        // 1. familyName + dateOfBirth + nationalId match -> automatic match
        Set<String> fullMatch = records.stream()
                .filter(r ->
                        r.hasReferenceId()
                                && r.matchesFamilyName(familyName)
                                && r.matchesDateOfBirth(dateOfBirth)
                                && r.matchesNationalId(nationalId))
                .map(r -> r.referenceId)
                .collect(Collectors.toSet());
        if (!fullMatch.isEmpty()) {
            assertThat(fullMatch).as("Fully matching reference IDs").hasSize(1);
            return MatchingResult.forReferenceId(fullMatch.iterator().next());
        }

        // 2. nationalId match without the others -> manual correlation
        // 3. givenName + familyName + dateOfBirth match -> manual correlation
        List<Record> approximateMatches = records.stream()
                .filter(r -> r.hasReferenceId()
                        && (r.matchesNationalId(nationalId)
                        || r.matchesGivenName(givenName) && r.matchesFamilyName(familyName) && r.matchesDateOfBirth(dateOfBirth)))
                .collect(Collectors.toList());
        if (!approximateMatches.isEmpty()) {
            Collection<PotentialMatch> potentialMatches = approximateMatches.stream()
                    .map(this::createPotentialMatch)
                    .collect(Collectors.toSet());
            Record equal = getSamePendingMatch(givenName, familyName, dateOfBirth, nationalId);
            if (equal != null) {
                return MatchingResult.forUncertain(equal.matchId, potentialMatches);
            } else {
                String matchId = String.valueOf(matchIdCounter.getAndIncrement());
                records.add(
                        new Record(attributes, null, matchId));
                return MatchingResult.forUncertain(matchId, potentialMatches);
            }
        }

        // No match (for sure)
        String referenceId = UUID.randomUUID().toString();
        records.add(new Record(attributes, referenceId, null));
        return MatchingResult.forReferenceId(referenceId);
    }

    private PotentialMatch createPotentialMatch(Record matchingRecord) {
        return new PotentialMatch(
                50,
                Objects.requireNonNull(matchingRecord.referenceId),
                matchingRecord.getAttributes());
    }

    private Record getSamePendingMatch(String givenName, String familyName, String dateOfBirth, String nationalId) {
        List<Record> equalPendingRecords = records.stream()
                .filter(r -> r.referenceId == null
                        && r.matchesGivenName(givenName)
                        && r.matchesFamilyName(familyName)
                        && r.matchesDateOfBirth(dateOfBirth)
                        && r.matchesNationalId(nationalId))
                .collect(Collectors.toList());
        return MiscUtil.extractSingleton(
                equalPendingRecords,
                () -> new IllegalStateException("Multiple equal pending records: " + equalPendingRecords));
    }

    @Override
    public void resolve(
            @NotNull ShadowAttributesType attributes,
            @Nullable String matchRequestId,
            @Nullable String referenceId,
            @NotNull OperationResult result) {
        argCheck(matchRequestId != null, "Match request ID must be provided");
        var matching = records.stream()
                .filter(r -> r.matchesMatchRequestId(matchRequestId))
                .collect(Collectors.toList());
        var singleMatching = MiscUtil.extractSingletonRequired(
                matching,
                () -> new IllegalStateException("Multiple records with match request ID " + matchRequestId + ": " + matching),
                () -> new IllegalArgumentException("No record with match request ID " + matchRequestId));
        singleMatching.referenceId = referenceId != null ?
                referenceId : UUID.randomUUID().toString();
    }

    private static String getValue(ShadowAttributesType attributes, String name) {
        PrismProperty<?> attribute = attributes.asPrismContainerValue().findProperty(new ItemName(NS_RI, name));
        return attribute != null ? attribute.getRealValue(String.class) : null;
    }

    private static class Record {
        private final ShadowAttributesType attributes;
        private String referenceId;
        private final String matchId;

        private Record(
                @NotNull ShadowAttributesType attributes,
                String referenceId,
                String matchId) {
            this.attributes = attributes;
            this.referenceId = referenceId;
            this.matchId = matchId;
        }

        private boolean matchesGivenName(String value) {
            return Objects.equals(value, getValue(this.attributes, ATTR_GIVEN_NAME));
        }

        private boolean matchesFamilyName(String value) {
            return Objects.equals(value, getValue(this.attributes, ATTR_FAMILY_NAME));
        }

        private boolean matchesDateOfBirth(String value) {
            return Objects.equals(value, getValue(this.attributes, ATTR_DATE_OF_BIRTH));
        }

        private boolean matchesNationalId(String value) {
            return Objects.equals(value, getValue(this.attributes, ATTR_NATIONAL_ID));
        }

        boolean hasReferenceId() {
            return referenceId != null;
        }

        boolean matchesMatchRequestId(String value) {
            return Objects.equals(value, matchId);
        }

        public @NotNull ShadowAttributesType getAttributes() {
            return attributes;
        }
    }
}
