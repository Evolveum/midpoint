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

import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

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
     * Current records, indexed by SOR identifier value. (Assuming single SOR.)
     * It looks like that COmanage Match does it in the same way.
     */
    @NotNull private final LinkedHashMap<String, Record> recordsMap = new LinkedHashMap<>();

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
        IdMatchObject idMatchObject = request.getObject();
        String sorIdentifierValue = idMatchObject.getSorIdentifierValue();
        IdMatchAttributesType attributes = idMatchObject.getAttributes();
        String givenName = getValue(attributes, ATTR_GIVEN_NAME);
        String familyName = getValue(attributes, ATTR_FAMILY_NAME);
        String dateOfBirth = getValue(attributes, ATTR_DATE_OF_BIRTH);
        String nationalId = getValue(attributes, ATTR_NATIONAL_ID);
        LOGGER.info("Looking for {}:{}:{}:{}", givenName, familyName, dateOfBirth, nationalId);

        Record existingRecord = recordsMap.get(sorIdentifierValue);
        if (existingRecord != null && existingRecord.referenceId != null) {
            LOGGER.info("Existing record found, with reference ID of {}:\n{}", existingRecord.referenceId, existingRecord);
            return MatchingResult.forReferenceId(existingRecord.referenceId);
        }

        Record currentRecord;
        if (existingRecord == null) {
            currentRecord = addRecord(idMatchObject);
        } else {
            currentRecord = updateRecord(existingRecord, idMatchObject);
        }

        // 1. familyName + dateOfBirth + nationalId match -> automatic match
        Set<String> fullMatch = recordsMap.values().stream()
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
            String referenceId = fullMatch.iterator().next();
            currentRecord.referenceId = referenceId;
            return MatchingResult.forReferenceId(referenceId);
        }

        // 2. nationalId match without the others -> manual correlation
        // 3. givenName + familyName + dateOfBirth match -> manual correlation
        List<Record> approximateMatches = recordsMap.values().stream()
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

            if (currentRecord.matchId == null) {
                currentRecord.matchId = String.valueOf(matchIdCounter.getAndIncrement());
            }
            // "no match" option
            potentialMatches.add(
                    new PotentialMatch(
                            50,
                            null,
                            idMatchObject.getAttributes()));
            return MatchingResult.forUncertain(currentRecord.matchId, potentialMatches);
        }

        LOGGER.info("No match");
        // No match (for sure)
        currentRecord.referenceId = UUID.randomUUID().toString();
        return MatchingResult.forReferenceId(currentRecord.referenceId);
    }

    @Override
    public void update(@NotNull IdMatchObject idMatchObject, @Nullable String referenceId, @NotNull OperationResult result)
            throws CommunicationException, SchemaException, SecurityViolationException {

        LOGGER.info("Updating:\n{}", idMatchObject.debugDump(1));

        Record existingRecord =
                Objects.requireNonNull(
                        recordsMap.get(idMatchObject.getSorIdentifierValue()),
                        () -> "No record for SOR identifier value: " + idMatchObject.getSorIdentifierValue());

        // It looks like COmanage Match ignores referenceId when updating a record. So neither do we.
        updateRecord(existingRecord, idMatchObject);
    }

    private Record addRecord(@NotNull IdMatchObject idMatchObject) {
        Record newRecord = new Record(
                idMatchObject.getSorIdentifierValue(),
                idMatchObject.getAttributes(),
                null,
                null);
        recordsMap.put(
                idMatchObject.getSorIdentifierValue(),
                newRecord);
        return newRecord;
    }

    private Record updateRecord(Record existing, @NotNull IdMatchObject idMatchObject) {
        Record newRecord = new Record(
                idMatchObject.getSorIdentifierValue(),
                idMatchObject.getAttributes(),
                existing.referenceId,
                existing.matchId);
        recordsMap.put(
                idMatchObject.getSorIdentifierValue(),
                newRecord);
        return newRecord;
    }

    private PotentialMatch createPotentialMatch(Record matchingRecord) {
        return new PotentialMatch(
                50,
                Objects.requireNonNull(matchingRecord.referenceId),
                matchingRecord.getAttributes());
    }

    @Override
    public @NotNull String resolve(
            @NotNull IdMatchObject idMatchObject,
            @Nullable String matchRequestId,
            @Nullable String referenceId,
            @NotNull OperationResult result) {
        argCheck(matchRequestId != null, "Match request ID must be provided");
        var matching = recordsMap.values().stream()
                .filter(r -> r.matchesMatchRequestId(matchRequestId))
                .collect(Collectors.toList());
        var singleMatching = MiscUtil.extractSingletonRequired(
                matching,
                () -> new IllegalStateException("Multiple records with match request ID " + matchRequestId + ": " + matching),
                () -> new IllegalArgumentException("No record with match request ID " + matchRequestId));
        singleMatching.referenceId = referenceId != null ?
                referenceId : UUID.randomUUID().toString();
        return singleMatching.referenceId;
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
    public void addRecord(
            @NotNull String sorIdentifierValue,
            @NotNull ShadowAttributesType attributes,
            @Nullable String referenceId,
            @Nullable String matchId)
            throws SchemaException {
        IdMatchAttributesType repackaged =
                IdMatchObject.create("dummy", attributes).getAttributes();
        addRecord(sorIdentifierValue, repackaged, referenceId, matchId);
    }

    /** Used for manual setup of the matcher state. */
    public void addRecord(
            @NotNull String sorIdentifierValue,
            @NotNull IdMatchAttributesType attributes,
            @Nullable String referenceId,
            @Nullable String matchId) {
        recordsMap.put(
                sorIdentifierValue,
                new Record(sorIdentifierValue, attributes, referenceId, matchId));
    }

    private static class Record {
        @NotNull private final String sorIdentifierValue;
        @NotNull private final IdMatchAttributesType attributes;
        private String referenceId;
        private String matchId;

        private Record(
                @NotNull String sorIdentifierValue,
                @NotNull IdMatchAttributesType attributes,
                String referenceId,
                String matchId) {
            this.sorIdentifierValue = sorIdentifierValue;
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

        @Override
        public String toString() {
            return "Record{" +
                    "sorIdentifierValue='" + sorIdentifierValue + '\'' +
                    ", attributes=" + attributes +
                    ", referenceId='" + referenceId + '\'' +
                    ", matchId='" + matchId + '\'' +
                    '}';
        }
    }
}
