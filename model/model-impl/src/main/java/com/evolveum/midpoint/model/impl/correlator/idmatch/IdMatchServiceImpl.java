/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.crypto.EncryptionException;

import com.evolveum.midpoint.util.exception.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;
import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.idmatch.*;
import com.evolveum.midpoint.model.impl.correlator.idmatch.constants.ResponseType;
import com.evolveum.midpoint.model.impl.correlator.idmatch.data.PersonRequest;
import com.evolveum.midpoint.model.impl.correlator.idmatch.operations.Client;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchCorrelatorType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * An interface from midPoint to real ID Match service.
 */
public class IdMatchServiceImpl implements IdMatchService {

    private static final Trace LOGGER = TraceManager.getTrace(IdMatchServiceImpl.class);

    private static final String REFERENCE_REQUEST_ID = "referenceId";
    private static final String MATCH_REQUEST_ID = "matchRequest";
    private static final String CANDIDATES = "candidates";

    private static final String MAPPED_ICFS_NAME = "icfs_name"; // temporary
    private static final String MAPPED_ICFS_UID = "icfs_uid"; // temporary

    /**
     * Value get from or sent to ID Match Service denoting the fact that a new reference ID is to be or should be created.
     * Part of ID Match API.
     */
    private static final String NEW_REFERENCE_ID = "new";

    /** URL where the service resides. */
    @NotNull private final String url;

    /** User name used to access the service. */
    @Nullable private final String username;

    /** Password used to access the service. */
    @Nullable private final ProtectedStringType password;

    /** SOR Label to be used in ID Match requests. */
    @NotNull private final String sorLabel;

    private static final String DEFAULT_SOR_LABEL = "midpoint";

    private IdMatchServiceImpl(
            @NotNull String url,
            @Nullable String username,
            @Nullable ProtectedStringType password,
            @Nullable String sorLabel) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.sorLabel = Objects.requireNonNullElse(sorLabel, DEFAULT_SOR_LABEL);
    }

    @Override
    public @NotNull MatchingResult executeMatch(@NotNull MatchingRequest request, @NotNull OperationResult result)
            throws CommunicationException, SchemaException, SecurityViolationException {

        LOGGER.trace("Executing match:\n{}", request.debugDumpLazily(1));

        Client client = createClient();

        PersonRequest matchRequest = generateMatchOrUpdateRequest(request.getObject());
        client.putPerson(matchRequest); // TODO reconsider

        int responseCode = client.getResponseCode();
        if (responseCode == ResponseType.CREATED.getResponseCode() ||
                responseCode == ResponseType.EXISTING.getResponseCode()) {
            return processKnownReferenceId(client, matchRequest);
        } else if (responseCode == ResponseType.ACCEPTED.getResponseCode()) {
            return processUnknownReferenceId(client, client.getEntity());
        } else {
            throw new IllegalStateException("Unsupported ID Match Service response: " + responseCode + ": " + client.getMessage());
        }
    }

    /**
     * The reference ID is known - either existing one was assigned or a new one was created.
     * (We treat both cases in the same way.)
     */
    private @NotNull MatchingResult processKnownReferenceId(Client client, PersonRequest personRequest)
            throws CommunicationException, SecurityViolationException {
        // COmanage implementation sometimes returns no data on 200/201 response, so let's fetch it explicitly
        // TODO avoid re-fetching if the service returned the data
        client.getPerson(personRequest);

        String entity = client.getEntity();
        String metaSection = getJsonElement(entity, "meta");
        String referenceId = getJsonElement(metaSection, REFERENCE_REQUEST_ID);

        MiscUtil.stateCheck(referenceId != null && !referenceId.isEmpty(),
                "Null or empty reference ID in %s", entity);

        return MatchingResult.forReferenceId(referenceId);
    }

    /**
     * The reference ID is not known - the result is uncertain. We have to provide a list of potential matches.
     */
    private @NotNull MatchingResult processUnknownReferenceId(Client client, String matchResponseText)
            throws SchemaException, CommunicationException, SecurityViolationException {
        String matchRequestId = getJsonElement(matchResponseText, MATCH_REQUEST_ID);

        // COmanage does not provide us with the complete information (potential matches), so we have to
        // fetch them ourselves.

        client.getMatchRequest(matchRequestId);

        try {
            return MatchingResult.forUncertain(matchRequestId,
                    createPotentialMatches(client.getEntity()));
        } catch (JSONException e) {
            throw new SchemaException("Unexpected exception while parsing ID Match response: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(
            @NotNull IdMatchObject idMatchObject,
            @Nullable String referenceId,
            @NotNull OperationResult result)
            throws CommunicationException, SchemaException, SecurityViolationException {

        LOGGER.trace("Updating object with reference ID {}:\n{}", referenceId, idMatchObject.debugDumpLazily(1));

        // It looks like COmanage Match ignores reference ID when updating. So we don't bother with putting it here.
        createClient().putPerson(
                generateMatchOrUpdateRequest(idMatchObject));

        // 4xx and 5xx errors are treated in the client. We accept all the other codes.
    }

    @NotNull
    private Client createClient() {
        return new Client(url, username, getPasswordCleartext());
    }

    @Nullable
    private String getPasswordCleartext() {
        try {
            if (password != null) {
                return PrismContext.get().getDefaultProtector().decryptString(password);
            } else {
                return null;
            }
        } catch (EncryptionException e) {
            throw new SystemException("Couldn't decrypt the password: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a list of potential matches from the ID Match response.
     *
     * All options presented by the ID Match service are converted. So if there is an option to create a new identity,
     * it is included in the returned value as well.
     *
     * Expected entity (see `entityObject` variable):
     *
     * ----
     * {
     *   ...
     *   "candidates": [     <--- denotes a list of candidates (see `candidatesArray`)
     *     {                 <--- first candidate
     *       "referenceId": "311fbdff-2ccf-4f98-b5d3-1534d96666d4",
     *       "sorRecords": [
     *         {
     *           "sorAttributes": {
     *             "givenname": "Ian",
     *             ...
     *           }
     *         },
     *         {
     *           "sorAttributes": {
     *             "givenname": "Jan",
     *             ...
     *           }
     *         }
     *       ]
     *     },
     *     ...              <--- second, third, etc candidates here (not shown)
     *     {                <--- last candidate (new person) - note: not necessarily the last one!
     *       "referenceId": "new",
     *       "sorAttributes": {
     *         "givenname": "Ian",
     *         ...
     *       }
     *     }
     *   ]
     * }
     * ----
     */

    private List<PotentialMatch> createPotentialMatches(String entity) throws SchemaException, JSONException {
        LOGGER.info("Creating potential matches from:\n{}", entity);
        List<PotentialMatch> potentialMatches = new ArrayList<>();

        JSONObject entityObject = new JSONObject(entity);
        JSONArray candidatesArray = entityObject.getJSONArray(CANDIDATES);

        for (int i = 0; i < candidatesArray.length(); i++) {
            potentialMatches.addAll(
                    createPotentialMatches(candidatesArray.get(i)));
        }

        return potentialMatches;
    }

    /**
     * Converts ID Match candidate to one or more {@link PotentialMatch} objects.
     *
     * (All of them share the same reference ID.)
     *
     * Note that in the future we may employ a nicer approach, preserving the structure of reference ID -> SOR records
     * links.
     */
    private List<PotentialMatch> createPotentialMatches(Object candidate) throws SchemaException, JSONException {
        JSONObject jsonObject = MiscUtil.castSafely(candidate, JSONObject.class);

        String rawReferenceId = jsonObject.getString(REFERENCE_REQUEST_ID);
        String referenceId = fromRawReferenceId(rawReferenceId);

        if (jsonObject.has("sorRecords")) {
            List<PotentialMatch> potentialMatches = new ArrayList<>();
            JSONArray sorRecordsArray = jsonObject.getJSONArray("sorRecords");
            for (int i = 0; i < sorRecordsArray.length(); i++) {
                JSONObject sorRecord = sorRecordsArray.getJSONObject(i);
                JSONObject sorAttributes = sorRecord.getJSONObject("sorAttributes");
                potentialMatches.add(
                        createPotentialMatchFromSorAttributes(referenceId, sorAttributes));
            }
            return potentialMatches;
        } else if (jsonObject.has("sorAttributes")) {
            return List.of(
                    createPotentialMatchFromSorAttributes(referenceId, jsonObject.getJSONObject("sorAttributes")));
        } else {
            throw new IllegalStateException("Unexpected candidate: neither sorRecords nor sorAttributes present: " + candidate);
        }
    }

    /**
     * Treats empty/"new" reference ID as no reference ID.
     */
    private String fromRawReferenceId(String rawReferenceId) {
        if (rawReferenceId != null && !rawReferenceId.isEmpty() && !rawReferenceId.equals(NEW_REFERENCE_ID)) {
            return rawReferenceId;
        } else {
            return null;
        }
    }

    private PotentialMatch createPotentialMatchFromSorAttributes(String referenceId, JSONObject sorAttributes)
            throws JSONException, SchemaException {
        IdMatchAttributesType attributes = new IdMatchAttributesType(PrismContext.get());
        Iterator<String> keys = sorAttributes.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if ("identifiers".equals(key)) {
                continue; // temporary hack
            }
            QName attributeName = toMidPointAttributeName(key);
            String stringValue = String.valueOf(sorAttributes.get(key));
            PrismProperty<String> attribute = PrismContext.get().itemFactory().createProperty(attributeName, null);
            attribute.setRealValue(stringValue);
            //noinspection unchecked
            attributes.asPrismContainerValue().add(attribute);
        }
        return new PotentialMatch(null, referenceId, attributes);
    }

    /**
     * Converts midPoint attribute name (QName) to ID Match attribute name (String).
     */
    private String fromMidPointAttributeName(ItemName midPointName) {
        if (SchemaConstants.ICFS_NAME.equals(midPointName)) {
            return MAPPED_ICFS_NAME;
        } else if (SchemaConstants.ICFS_UID.equals(midPointName)) {
            return MAPPED_ICFS_UID;
        } else {
            return midPointName.getLocalPart();
        }
    }

    /**
     * Converts incoming ID Match attribute name (String) to midPoint attribute name (QName).
     */
    private @NotNull QName toMidPointAttributeName(String key) {
        if (MAPPED_ICFS_NAME.equals(key)) {
            return SchemaConstants.ICFS_NAME;
        } else if (MAPPED_ICFS_UID.equals(key)) {
            return SchemaConstants.ICFS_UID;
        } else {
            return new QName(MidPointConstants.NS_RI, key);
        }
    }

    /**
     * Parses a String representation of a JSON object and returns given sub-element.
     */
    private String getJsonElement(String jsonObject, String elementName) {
        try {
            JSONObject object = new JSONObject(jsonObject);
            return object.getString(elementName); // TODO is this a re-serialization?
        } catch (JSONException e) {
            throw new SystemException(e); // at least for now
        }
    }

    /**
     * Generates a request to match a person or to resolve an open match request.
     *
     * Note that when resolving a person, both `referenceId` and `matchRequestId` are null.
     *
     * And when resolving an open match request, `referenceId` must be present - either real one or `new` (to create
     * a new ID), and `matchRequestId` should be present (if previously returned by the ID Match service).
     */
    private PersonRequest createPersonRequest(
            @NotNull IdMatchObject idMatchObject,
            @Nullable String referenceId,
            @Nullable String matchRequestId) {

        try {
            JsonFactory factory = new JsonFactory(); // todo from jackson-core?
            StringWriter jsonString = new StringWriter();
            JsonGenerator generator = factory.createGenerator(jsonString); // todo from jackson-core?

            generator.useDefaultPrettyPrinter();
            generator.writeStartObject();
            if (matchRequestId != null) {
                generator.writeFieldName("matchRequest");
                generator.writeString(matchRequestId);
            }
            generator.writeFieldName("sorAttributes");
            generator.writeStartObject();

            for (PrismProperty<?> property : idMatchObject.getProperties()) {
                String elementName = fromMidPointAttributeName(property.getElementName());
                String elementRealValue = getStringValue(property);

                generator.writeFieldName(elementName);
                generator.writeString(elementRealValue);
            }

            generator.writeEndObject();
            if (referenceId != null && !referenceId.isEmpty()) {
                generator.writeFieldName("referenceId");
                generator.writeString(referenceId);
            }
            generator.writeEndObject();
            generator.close();

            return new PersonRequest(sorLabel, idMatchObject.getSorIdentifierValue(), String.valueOf(jsonString));
        } catch (IOException e) {
            // We really don't expect IOException here. Let's not bother with it, and throw
            // a SystemException, because this is most probably some weirdness.
            throw SystemException.unexpected(e, "while constructing ID Match JSON request");
        }
    }

    // TEMPORARY!
    private String getStringValue(Item<?, ?> item) {
        Object realValue = item.getRealValue();
        if (realValue instanceof String) {
            return (String) realValue;
        } else if (realValue instanceof PolyString) {
            return ((PolyString) realValue).getOrig();
        } else if (realValue instanceof RawType) {
            return ((RawType) realValue).extractString();
        } else {
            throw new UnsupportedOperationException("Non-string attributes are not supported: " + item);
        }
    }

    private PersonRequest generateMatchOrUpdateRequest(@NotNull IdMatchObject idMatchObject) {
        return createPersonRequest(idMatchObject, null, null);
    }

    public PersonRequest generateResolveRequest(
            @NotNull IdMatchObject idMatchObject,
            @NotNull String referenceId,
            @Nullable String matchRequestId) throws SchemaException {
        return createPersonRequest(idMatchObject, referenceId, matchRequestId);
    }

    @Override
    public @NotNull String resolve(
            @NotNull IdMatchObject idMatchObject, @Nullable String matchRequestId,
            @Nullable String referenceId,
            @NotNull OperationResult result) throws CommunicationException, SchemaException, SecurityViolationException {

        String nativeReferenceId = referenceId != null ? referenceId : NEW_REFERENCE_ID;
        PersonRequest request = generateResolveRequest(idMatchObject, nativeReferenceId, matchRequestId);
        Client client = createClient();
        client.putPerson(request);

        return MiscUtil.requireNonNull(
                processKnownReferenceId(client, request)
                        .getReferenceId(),
                () -> new IllegalStateException("No reference ID after resolution of " + idMatchObject.getSorIdentifierValue()));
    }

    /**
     * Creates an instance of ID Match service for given ID Match correlator configuration.
     */
    public static IdMatchServiceImpl instantiate(@NotNull IdMatchCorrelatorType configuration) throws ConfigurationException {
        return new IdMatchServiceImpl(
                MiscUtil.requireNonNull(configuration.getUrl(),
                        () -> new ConfigurationException("No URL in ID Match service configuration")),
                configuration.getUsername(),
                configuration.getPassword(),
                configuration.getSorLabel());
    }

    /**
     * Creates an instance of ID Match service with explicitly defined parameters.
     */
    @VisibleForTesting
    public static IdMatchServiceImpl instantiate(
            @NotNull String url,
            @Nullable String username,
            @Nullable ProtectedStringType password) {
        return new IdMatchServiceImpl(
                url,
                username,
                password,
                null);
    }
}
