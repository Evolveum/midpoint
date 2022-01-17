/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import com.evolveum.midpoint.model.api.correlator.idmatch.IdMatchService;
import com.evolveum.midpoint.model.api.correlator.idmatch.MatchingResult;
import com.evolveum.midpoint.model.api.correlator.idmatch.PotentialMatch;
import com.evolveum.midpoint.model.impl.correlator.idmatch.constants.ResponseType;
import com.evolveum.midpoint.model.impl.correlator.idmatch.data.structure.JsonRequest;
import com.evolveum.midpoint.model.impl.correlator.idmatch.operations.Client;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.evolveum.prism.xml.ns._public.types_3.RawType;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONException;
import com.github.openjson.JSONObject;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class IdMatchServiceImpl implements IdMatchService {

    private static final String REFERENCE_REQUEST_ID = "referenceId";
    private static final String MATCH_REQUEST_ID = "matchRequest";
    private static final String CANDIDATES = "candidates";

    private static final String MAPPED_ICFS_NAME = "icfs_name"; // temporary
    private static final String MAPPED_ICFS_UID = "icfs_uid"; // temporary

    private static final Trace LOGGER = TraceManager.getTrace(IdMatchServiceImpl.class);

    /** URL where the service resides. */
    @NotNull private final String url;

    /** User name used to access the service. */
    @Nullable private final String username;

    /** Password used to access the service. */
    @Nullable private final ProtectedStringType password;

    /** SOR Label to be used in ID Match requests. */
    @NotNull private final String sorLabel;

    private static final String DEFAULT_SOR_LABEL = "sor";

    /** A shadow attribute to be used as SOR ID in the requests. */
    @NotNull private final QName sorIdAttribute;

    private static final QName DEFAULT_SOR_ID_ATTRIBUTE = SchemaConstants.ICFS_UID;

    private IdMatchServiceImpl(
            @NotNull String url,
            @Nullable String username,
            @Nullable ProtectedStringType password,
            @Nullable String sorLabel,
            @Nullable QName sorIdAttribute) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.sorLabel = Objects.requireNonNullElse(sorLabel, DEFAULT_SOR_LABEL);
        this.sorIdAttribute = Objects.requireNonNullElse(sorIdAttribute, DEFAULT_SOR_ID_ATTRIBUTE);
    }

    @Override
    public @NotNull MatchingResult executeMatch(@NotNull ShadowAttributesType attributes, @NotNull OperationResult result)
            throws CommunicationException, SchemaException {

        LOGGER.trace("Executing match for:\n{}", attributes.debugDumpLazily(1));

        Client client = createClient();

        //noinspection unchecked
        PrismContainerValue<ShadowAttributesType> attributesPcv = attributes.asPrismContainerValue();

        JsonRequest jsonRequest = generateMatchRequest(attributesPcv);
        client.peoplePut(jsonRequest); // TODO reconsider

        int responseCode = client.getResponseCode();
        if (responseCode == ResponseType.CREATED.getResponseCode() ||
                responseCode == ResponseType.EXISTING.getResponseCode()) {
            return processKnownReferenceId(client, jsonRequest);
        } else if (responseCode == ResponseType.ACCEPTED.getResponseCode()) {
            return processUnknownReferenceId(client);
        } else {
            throw new IllegalStateException("Unsupported ID Match Service response: " + responseCode + ": " + client.getMessage());
        }
    }

    /**
     * The reference ID is known - either existing one was assigned or a new one was created.
     * (We treat both cases in the same way.)
     */
    private @NotNull MatchingResult processKnownReferenceId(Client client, JsonRequest jsonRequest) {
        // COmanage implementation sometimes returns no data on 200/201 response, so let's fetch it explicitly
        // TODO avoid re-fetching if the service returned the data
        client.peopleById(jsonRequest.getSorLabel(), jsonRequest.getSorId());

        String entity = client.getEntity();
        String metaSection = parseJsonObject(entity, "meta");
        String referenceId = parseJsonObject(metaSection, REFERENCE_REQUEST_ID);

        MiscUtil.stateCheck(referenceId != null && !referenceId.isEmpty(),
                "Null or empty reference ID in %s", entity);

        return MatchingResult.forReferenceId(referenceId);
    }

    /**
     * The reference ID is not known - the result is uncertain. We have to provide a list of potential matches.
     */
    private @NotNull MatchingResult processUnknownReferenceId(Client client) throws SchemaException {
        String entity = client.getEntity();
        String matchRequestId = parseJsonObject(entity, MATCH_REQUEST_ID);

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

    @NotNull
    private Client createClient() {
        return new Client(url, username, password != null ? password.getClearValue() : null);
    }

    /**
     * Creates a list of potential matches from the ID Match response.
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
        System.out.println("Creating potential matches from: "+ entity);
        List<PotentialMatch> potentialMatches = new ArrayList<>();

        JSONObject entityObject = new JSONObject(entity);
        JSONArray candidatesArray = entityObject.getJSONArray(CANDIDATES);
        System.out.println("Candidates array: " + candidatesArray);

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

        String referenceId = jsonObject.getString(REFERENCE_REQUEST_ID);
        if (referenceId == null || referenceId.isEmpty() || referenceId.equals("new")) {
            return List.of(); // Temporary: "new reference ID" should be NOT included among potential matches
        }
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

    private PotentialMatch createPotentialMatchFromSorAttributes(String referenceId, JSONObject sorAttributes)
            throws JSONException, SchemaException {
        ShadowAttributesType attributes = new ShadowAttributesType(PrismContext.get());
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
    private String parseJsonObject(String jsonObject, String elementName) {
        JSONObject object;
        String element;
        try {
            object = new JSONObject(jsonObject);
            element = object.getString(elementName); // TODO is this a re-serialization?
            return element;
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
    private JsonRequest generateJsonRequest(@NotNull PrismContainerValue<ShadowAttributesType> attributes,
            @Nullable String referenceId, @Nullable String matchRequestId) throws SchemaException {

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

            List<Item<?, ?>> sorIdentifiersFound = new ArrayList<>();

            for (Item<?, ?> item : attributes.getItems()) {
                String elementName = fromMidPointAttributeName(item.getElementName());
                String elementRealValue = getStringValue(item);

                if (QNameUtil.match(sorIdAttribute, item.getElementName())) {
                    sorIdentifiersFound.add(item);
                }

                generator.writeFieldName(elementName.toLowerCase());
                generator.writeString(elementRealValue);
            }

            generator.writeEndObject();
            if (referenceId != null && !referenceId.isEmpty()) {
                generator.writeFieldName("referenceId");
                generator.writeString(referenceId);
            }
            generator.writeEndObject();
            generator.close();

            Item<?, ?> sorId = MiscUtil.extractSingletonRequired(
                    sorIdentifiersFound,
                    () -> new SchemaException("Ambiguous SOR Identifier attribute '" + sorIdAttribute
                            + "', multiple values were found: " + sorIdentifiersFound),
                    () -> new SchemaException("No value for SOR Identifier attribute '" + sorIdAttribute + "' found"));
            String sorIdValue = getStringValue(sorId);
            return new JsonRequest(sorLabel, sorIdValue, String.valueOf(jsonString));
        } catch (IOException e) {
            // We really don't expect IOException here. Let's not bother with it, and throw
            // a SystemException, because this is most probably some weirdness.
            throw new SystemException("Unexpected IOException: " + e.getMessage(), e);
        }
    }

    // TEMPORARY!
    private String getStringValue(Item<?, ?> item) {
        Object realValue = item.getRealValue();
        if (realValue instanceof String) {
            return (String) realValue;
        } else if (realValue instanceof RawType) {
            return ((RawType) realValue).extractString();
        } else {
            throw new UnsupportedOperationException("Non-string attributes are not supported: " + item);
        }
    }

    public JsonRequest generateMatchRequest(@NotNull PrismContainerValue<ShadowAttributesType> attributes)
            throws SchemaException {
        return generateJsonRequest(attributes, null, null);
    }

    public JsonRequest generateResolveRequest(@NotNull PrismContainerValue<ShadowAttributesType> attributes,
            @NotNull String referenceId, @Nullable String matchRequestId) throws SchemaException {
        return generateJsonRequest(attributes, referenceId, matchRequestId);
    }

    @Override
    public void resolve(
            @NotNull ShadowAttributesType attributes,
            @Nullable String matchRequestId,
            @Nullable String referenceId,
            @NotNull OperationResult result) throws CommunicationException, SchemaException {

        String nativeReferenceId = referenceId != null ? referenceId : "new";
        //noinspection unchecked
        JsonRequest request = generateResolveRequest(
                attributes.asPrismContainerValue(), nativeReferenceId, matchRequestId);
        Client client = createClient();
        client.peoplePut(request);
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
                configuration.getSorLabel(),
                configuration.getSorIdentifierAttribute());
    }

    /**
     * Creates an instance of ID Match service with explicitly defined parameters.
     */
    @VisibleForTesting
    public static IdMatchServiceImpl instantiate(
            @NotNull String url,
            @Nullable String username,
            @Nullable ProtectedStringType password) {
        return new IdMatchServiceImpl(url, username, password, null, null);
    }
}
