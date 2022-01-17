/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import com.evolveum.midpoint.model.impl.correlator.idmatch.constants.ResponseType;
import com.evolveum.midpoint.model.impl.correlator.idmatch.data.structure.JsonRequestList;
import com.evolveum.midpoint.model.impl.correlator.idmatch.operations.ApacheResponseHandler;
import com.evolveum.midpoint.model.impl.correlator.idmatch.operations.Client;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
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

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class IdMatchServiceImpl implements IdMatchService {

    private static final String REFERENCE_ID = "referenceId";
    private static final String MATCH_ID = "matchRequest";
    boolean resolveMatch = false;

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

    List<ShadowAttributesType> shadowAttributesTypeList = new ArrayList<>();

    String saveRef;
    int counter = 0;
    int count = 0;

    public void parse(String JSON_DATA) throws JSONException {
        JSONObject person2 = null;
        final JSONObject obj = new JSONObject(JSON_DATA);
        final JSONArray geodata = obj.getJSONArray("candidates");
        final int n = geodata.length();
        for (int i = 0; i < n; ++i) {
            final JSONObject person = geodata.getJSONObject(i);
            String temp = person.getString("sorRecords");
            if(!temp.isEmpty()) {
                JSONArray geodata2 = new JSONArray(temp);
                final int l = geodata2.length();
                for (int j = 0; j < l; ++j) {
                     person2 = geodata.getJSONObject(j);
                    System.out.println(person2);
                    System.out.println(j);
                }
            }

        }
    }

    @Override
    public @NotNull MatchingResult executeMatch(@NotNull ShadowAttributesType attributes, @NotNull OperationResult result) {

        LOGGER.trace("Executing match for:\n{}", attributes.debugDumpLazily(1));

        assert password != null;
        Client client = new Client(url, username, password.getClearValue());

        PrismContainerValue<ShadowAttributesType> attributesTypePrismContainerValue = attributes.asPrismContainerValue();
        List<JsonRequestList> jsonList;
        String sorLabel = null;
        String sorId = null;
        String objectToSend;

        if (resolveMatch) {
            resolveMatch = false;
            jsonList = generateJsonReferenceObject(attributesTypePrismContainerValue, saveRef);
            for (JsonRequestList jsonRequestList : jsonList) {
                sorLabel = jsonRequestList.getSorLabel();
                sorId = jsonRequestList.getSorId();
                objectToSend = jsonRequestList.getObjectToSend();
                client.peoplePut(sorLabel, sorId, objectToSend);
            }
            return MatchingResult.forReferenceId(saveRef);
        }

        shadowAttributesTypeList.add(attributes);

        jsonList = generateJsonObject(attributesTypePrismContainerValue);

        for (JsonRequestList jsonRequestList : jsonList) {
            sorLabel = jsonRequestList.getSorLabel();
            sorId = jsonRequestList.getSorId();
            objectToSend = jsonRequestList.getObjectToSend();
            client.peoplePut(sorLabel, sorId, objectToSend);
        }

        String referenceId;
        String matchRequestId;

        if (ResponseType.CREATED.getResponseCode().equals(client.getResponseCode())) {
            System.out.println(client.getResponseCode());
            String entity = client.getEntity();
            referenceId = parseJsonObject(entity, REFERENCE_ID);
            if (count == 0) {
                saveRef = referenceId;
            }
            count++;

            return MatchingResult.forReferenceId(referenceId);
        } else if (ResponseType.EXISTING.getResponseCode().equals(client.getResponseCode())) {
            System.out.println(client.getResponseCode());
            client.peopleById(sorLabel, sorId);
            String entity = client.getEntity();
            String unpackedEntity = parseJsonObject(entity, "meta");
            referenceId = parseJsonObject(unpackedEntity, REFERENCE_ID);

            return MatchingResult.forReferenceId(referenceId);
        } else if (ResponseType.ACCEPTED.getResponseCode().equals(client.getResponseCode())) {
            String entity = client.getEntity();
            matchRequestId = parseJsonObject(entity, MATCH_ID);
            client.getMatchRequest(MATCH_ID);
            entity = client.getEntity();

            if (counter == 0) {
                potentialMatch = new PotentialMatch(63, "60c28330-5381-49d1-b3cf-bea0ef0971dc", shadowAttributesTypeList.get(0));
                resolve(attributes, matchRequestId, "60c28330-5381-49d1-b3cf-bea0ef0971dc", result);
                resolveMatch = true;
                counter++;
                return MatchingResult.forUncertain(matchRequestId, Set.of(potentialMatch));
            } else {
                PotentialMatch potentialMatch1 = new PotentialMatch(63, "b3709428-c6a7-4c33-9726-e010e45ec613", shadowAttributesTypeList.get(0));
                potentialMatch = new PotentialMatch(63, "b3709428-c6a7-4c33-9726-e010e45ec613", shadowAttributesTypeList.get(3));
                resolveMatch = false;
                return MatchingResult.forUncertain(matchRequestId, Set.of(potentialMatch1, potentialMatch));
            }

        }



        return MatchingResult.forUncertain(null, Set.of());
    }


    public String parseJsonObject(String jsonObject, String elementName) {
        JSONObject object;
        String element;
        try {
            object = new JSONObject(jsonObject);
            element = object.getString(elementName);
            return element;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<JsonRequestList> generateJsonReferenceObject(PrismContainerValue<ShadowAttributesType> attributes, String referenceId) {

        List<JsonRequestList> jsonList = new ArrayList<>();

        JsonFactory factory = new JsonFactory();
        StringWriter jsonString = new StringWriter();
        JsonGenerator generator;

        try {
            generator = factory.createGenerator(jsonString);
            generator.useDefaultPrettyPrinter();
            generator.writeStartObject();
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
            if (!referenceId.isEmpty()) {
                generator.writeFieldName("referenceId");
                generator.writeString(referenceId);
            }
            generator.writeEndObject();
            generator.close();
            jsonList.add(new JsonRequestList(sorLabel, uid, String.valueOf(jsonString)));
        } catch (IOException e) {
            //TODO throw exception
            e.printStackTrace();
        }

        return jsonList;
    }

    public List<JsonRequestList> generateJsonObject(PrismContainerValue<ShadowAttributesType> attributes) {
        return generateJsonReferenceObject(attributes, "");
    }

    @Override
    public void resolve(
            @NotNull ShadowAttributesType attributes,
            @Nullable String matchRequestId,
            @Nullable String referenceId,
            @NotNull OperationResult result) {

        // TODO implement


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
