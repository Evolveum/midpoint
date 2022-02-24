/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.idmatch;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.idmatch.*;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

/**
 * A dummy implementation of {@link IdMatchService} used to test IdMatchCorrelator without actual ID Match Service.
 */
public class DummyIdMatchServiceImpl implements IdMatchService {

    private static final Trace LOGGER = TraceManager.getTrace(DummyIdMatchServiceImpl.class);

    private static final String ATTR_GIVEN_NAME = "givenName";
    private static final String ATTR_FAMILY_NAME = "familyName";
    private static final String ATTR_DATE_OF_BIRTH = "dateOfBirth";
    private static final String ATTR_NATIONAL_ID = "nationalId";

    /**
     * Current records.
     */
    @NotNull private final List<Record> records = new ArrayList<>();

    @NotNull private final AtomicInteger matchIdCounter = new AtomicInteger();

    /**
     * The algorithm for matching:
     *
     * - If familyName, dateOfBirth, nationalId exactly match -> the this is 100% automatic match (regardless of given name)
     * - If either nationalId matches (regardless of the rest), or givenName + familyName + dateOfBirth match (regardless
     * of nationalId), then this is an approximate match
     * - otherwise no match is reported
     */
    @Override
    public @NotNull MatchingResult executeMatch(@NotNull MatchingRequest request, @NotNull OperationResult result) {
        IdMatchAttributesType attributes = request.getObject().getAttributes();
        String givenName = getValue(attributes, ATTR_GIVEN_NAME);
        String familyName = getValue(attributes, ATTR_FAMILY_NAME);
        String dateOfBirth = getValue(attributes, ATTR_DATE_OF_BIRTH);
        String nationalId = getValue(attributes, ATTR_NATIONAL_ID);
        LOGGER.info("Looking for {}:{}:{}:{}", givenName, familyName, dateOfBirth, nationalId);

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
            LOGGER.info("Full match(es):\n{}", String.join("\n", fullMatch));
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
            LOGGER.info("Approximate match(es):\n{}",
                    approximateMatches.stream().map(String::valueOf).collect(Collectors.joining("\n")));
            Collection<PotentialMatch> potentialMatches = approximateMatches.stream()
                    .map(this::createPotentialMatch)
                    .collect(Collectors.toCollection(HashSet::new));
            Record existingPendingMatch = getSamePendingMatch(givenName, familyName, dateOfBirth, nationalId);
            String matchId;
            if (existingPendingMatch != null) {
                matchId = existingPendingMatch.matchId;
            } else {
                matchId = String.valueOf(matchIdCounter.getAndIncrement());
                records.add(
                        new Record(attributes, null, matchId));
            }
            // "no match" potential match
            potentialMatches.add(
                    new PotentialMatch(
                            50,
                            null,
                            request.getObject().getAttributes()));
            return MatchingResult.forUncertain(matchId, potentialMatches);
        }

        LOGGER.info("No match");
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
            @NotNull IdMatchObject idMatchObject, @Nullable String matchRequestId,
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

    private static String getValue(IdMatchAttributesType attributes, String name) {
        PrismProperty<?> attribute = attributes.asPrismContainerValue().findProperty(new ItemName(name));
        if (attribute == null) {
            return null;
        } else {
            Object realValue = attribute.getRealValue();
            return realValue != null ? realValue.toString() : null;
        }
    }

    /** Used for manual setup of the matcher state. */
    public void addRecord(@NotNull ShadowAttributesType attributes, @Nullable String referenceId, @Nullable String matchId)
            throws SchemaException {
        IdMatchAttributesType repackaged =
                IdMatchObject.create("dummy", attributes).getAttributes();
        addRecord(repackaged, referenceId, matchId);
    }

    /** Used for manual setup of the matcher state. */
    public void addRecord(@NotNull IdMatchAttributesType attributes, @Nullable String referenceId, @Nullable String matchId) {
        records.add(
                new Record(attributes, referenceId, matchId));
    }

    private static class Record {
        private final IdMatchAttributesType attributes;
        private String referenceId;
        private final String matchId;

        private Record(
                @NotNull IdMatchAttributesType attributes,
                String referenceId,
                String matchId) {
            this.attributes = attributes;
            this.referenceId = referenceId;
            this.matchId = matchId;
        }

        private boolean matchesGivenName(String value) {
            return Objects.equals(value, getValue(attributes, ATTR_GIVEN_NAME));
        }

        private boolean matchesFamilyName(String value) {
            return Objects.equals(value, getValue(attributes, ATTR_FAMILY_NAME));
        }

        private boolean matchesDateOfBirth(String value) {
            return Objects.equals(value, getValue(attributes, ATTR_DATE_OF_BIRTH));
        }

        private boolean matchesNationalId(String value) {
            return Objects.equals(value, getValue(attributes, ATTR_NATIONAL_ID));
        }

        boolean hasReferenceId() {
            return referenceId != null;
        }

        boolean matchesMatchRequestId(String value) {
            return Objects.equals(value, matchId);
        }

        public @NotNull IdMatchAttributesType getAttributes() {
            return attributes;
        }
    }
}
