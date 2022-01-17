/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import com.evolveum.midpoint.model.impl.correlator.idmatch.constants.ResponseType;
import com.evolveum.midpoint.model.impl.correlator.idmatch.data.structure.JsonRequest;
import com.evolveum.midpoint.model.impl.correlator.idmatch.operations.Client;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class IdMatchServiceImpl implements IdMatchService {

    private static final String REFERENCE_REQUEST_ID = "referenceId";
    private static final String MATCH_REQUEST_ID = "matchRequest";

    private static final Trace LOGGER = TraceManager.getTrace(IdMatchServiceImpl.class);

    /** URL where the service resides. */
    @NotNull private final String url;

    /** User name used to access the service. */
    @Nullable private final String username;

    /** Password used to access the service. */
    @Nullable private final ProtectedStringType password;
    PotentialMatch potentialMatch;

    private IdMatchServiceImpl(@NotNull String url, @Nullable String username, @Nullable ProtectedStringType password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }



    @Override
    public @NotNull MatchingResult executeMatch(@NotNull ShadowAttributesType attributes, @NotNull OperationResult result)
            throws CommunicationException, SchemaException {

        LOGGER.trace("Executing match for:\n{}", attributes.debugDumpLazily(1));

        Client client = createClient();

        PrismContainerValue<ShadowAttributesType> attributesPcv = attributes.asPrismContainerValue();

//        if (resolveMatch) {
//            resolveMatch = false;
//            jsonList = generateJsonReferenceObject(attributesTypePrismContainerValue, saveRef);
//            for (JsonRequestList jsonRequestList : jsonList) {
//                sorLabel = jsonRequestList.getSorLabel();
//                sorId = jsonRequestList.getSorId();
//                objectToSend = jsonRequestList.getObjectToSend();
//                client.peoplePut(sorLabel, sorId, objectToSend);
//            }
//            return MatchingResult.forReferenceId(saveRef);
//        }
//
//        shadowAttributesTypeList.add(attributes);

        JsonRequest jsonRequest = generateMatchRequest(attributesPcv);
        client.peoplePut(jsonRequest); // TODO reconsider

        String responseCode = client.getResponseCode();
        if (ResponseType.CREATED.getResponseCode().equals(responseCode)) {
            String entity = client.getEntity();
            String referenceId = parseJsonObject(entity, REFERENCE_REQUEST_ID);
            return MatchingResult.forReferenceId(referenceId);
        } else if (ResponseType.EXISTING.getResponseCode().equals(responseCode)) {
            // COmanage implementation sometimes returns no data on "200" response, so let's fetch it explicitly
            // TODO avoid re-fetching if the service returned the data
            client.peopleById(jsonRequest.getSorLabel(), jsonRequest.getSorId());
            String entity = client.getEntity();
            String metaSection = parseJsonObject(entity, "meta");
            String referenceId = parseJsonObject(metaSection, REFERENCE_REQUEST_ID);
            MiscUtil.stateCheck(referenceId != null && !referenceId.isEmpty(), "Null or empty reference ID in %s", entity);
            return MatchingResult.forReferenceId(referenceId);
        } else if (ResponseType.ACCEPTED.getResponseCode().equals(responseCode)) {
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
        } else {
            throw new IllegalStateException("Unsupported ID Match Service response: " + responseCode + ": " + client.getMessage());
        }
    }

    @NotNull
    private Client createClient() {
        Client client = new Client(url, username, password != null ? password.getClearValue() : null);
        return client;
    }

    /**
     * Creates a list of potential matches from the ID Match response.
     *
     * Expected entity (see `entityObject` variable):
     *
     * ====
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
     * ====
     */

    private List<PotentialMatch> createPotentialMatches(String entity) throws SchemaException, JSONException {
        LOGGER.info("Creating potential matches from:\n{}", entity);
        System.out.println("Creating potential matches from: "+ entity);
        List<PotentialMatch> potentialMatches = new ArrayList<>();

        JSONObject entityObject = new JSONObject(entity);
        JSONArray candidatesArray = entityObject.getJSONArray("candidates");
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
        ItemFactory itemFactory = PrismContext.get().itemFactory();
        PrismContainerValue<ShadowAttributesType> attributesPcv = itemFactory.createContainerValue();
        Iterator<String> keys = sorAttributes.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if ("identifiers".equals(key)) {
                continue; // temporary hack
            }
            String stringValue = String.valueOf(sorAttributes.get(key));
            QName attributeName = new QName(MidPointConstants.NS_RI, key);
            PrismProperty<String> attribute = itemFactory.createProperty(attributeName, null);
            attribute.setRealValue(stringValue);
            attributesPcv.add(attribute);
        }
        ShadowAttributesType attributes = attributesPcv.asContainerable();
        return new PotentialMatch(null, referenceId, attributes);
    }

    /**
     * Parses a String representation of a JSON object and returns given sub-element.
     */
    public String parseJsonObject(String jsonObject, String elementName) {
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
    public JsonRequest generateJsonRequest(@NotNull PrismContainerValue<ShadowAttributesType> attributes,
            @Nullable String referenceId, @Nullable String matchRequestId) {

        try {
            JsonFactory factory = new JsonFactory();
            StringWriter jsonString = new StringWriter();
            JsonGenerator generator = factory.createGenerator(jsonString);

            generator.useDefaultPrettyPrinter();
            generator.writeStartObject();
            if (matchRequestId != null) {
                generator.writeFieldName("matchRequest");
                generator.writeString(matchRequestId);
            }
            generator.writeFieldName("sorAttributes");
            generator.writeStartObject();

            String sorLabel = "sor";
            String uid = null;
            String elementName;
            String elementRealValue;

            for (Item<?, ?> item : attributes.getItems()) {
                elementName = item.getElementName().getLocalPart();
                elementRealValue = item.getRealValue(String.class);

                if (elementName.equals("uid")) {
                    uid = elementRealValue;
                } else {
                    generator.writeFieldName(elementName.toLowerCase());
                    generator.writeString(elementRealValue);
                }
            }

            generator.writeEndObject();
            if (referenceId != null && !referenceId.isEmpty()) {
                generator.writeFieldName("referenceId");
                generator.writeString(referenceId);
            }
            generator.writeEndObject();
            generator.close();
            return new JsonRequest(sorLabel, uid, String.valueOf(jsonString));
        } catch (IOException e) {
            // We really don't expect IOException here. Let's not bother with it, and throw
            // a SystemException, because this is most probably some weirdness.
            throw new SystemException("Unexpected IOException: " + e.getMessage(), e);
        }
    }

    public JsonRequest generateMatchRequest(@NotNull PrismContainerValue<ShadowAttributesType> attributes) {
        return generateJsonRequest(attributes, null, null);
    }

    public JsonRequest generateResolveRequest(@NotNull PrismContainerValue<ShadowAttributesType> attributes,
            @NotNull String referenceId, @Nullable String matchRequestId) {
        return generateJsonRequest(attributes, referenceId, matchRequestId);
    }

    @Override
    public void resolve(
            @NotNull ShadowAttributesType attributes,
            @Nullable String matchRequestId,
            @Nullable String referenceId,
            @NotNull OperationResult result) throws CommunicationException {

        String nativeReferenceId = referenceId != null ? referenceId : "new";
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
                configuration.getPassword());
    }

    /**
     * Creates an instance of ID Match service with explicitly defined parameters.
     */
    @VisibleForTesting
    public static IdMatchServiceImpl instantiate(
            @NotNull String url,
            @Nullable String username,
            @Nullable ProtectedStringType password) {
        return new IdMatchServiceImpl(url, username, password);
    }

    private String getPasswordCleartext() throws EncryptionException {
        if (password != null) {
            return PrismContext.get().getDefaultProtector()
                    .decryptString(password);
        } else {
            return null;
        }
    }
}
