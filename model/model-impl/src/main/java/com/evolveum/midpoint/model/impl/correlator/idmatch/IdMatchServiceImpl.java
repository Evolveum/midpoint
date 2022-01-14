/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch;

import com.evolveum.midpoint.model.impl.correlator.idmatch.data.structure.JsonListStructure;
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

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

public class IdMatchServiceImpl implements IdMatchService {

    private static final Trace LOGGER = TraceManager.getTrace(IdMatchServiceImpl.class);

    /** URL where the service resides. */
    @NotNull private final String url;

    /** User name used to access the service. */
    @Nullable private final String username;

    /** Password used to access the service. */
    @Nullable private final ProtectedStringType password;

    private IdMatchServiceImpl(@NotNull String url, @Nullable String username, @Nullable ProtectedStringType password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public @NotNull MatchingResult executeMatch(@NotNull ShadowAttributesType attributes, @NotNull OperationResult result) {

        LOGGER.trace("Executing match for:\n{}", attributes.debugDumpLazily(1));

        Client client = new Client(url, username, password.getClearValue());

        PrismContainerValue<ShadowAttributesType> pcv = attributes.asPrismContainerValue();

        List<JsonListStructure> jsonList;

        jsonList = generateJson(pcv);

        String sorLabel;
        String sorId;
        String objectToSend;

        for (JsonListStructure jsonListStructure : jsonList) {
            sorLabel = jsonListStructure.getSorLabel();
            sorId = jsonListStructure.getSorId();
            objectToSend = jsonListStructure.getObjectToSend();

            client.peoplePut(sorLabel, sorId, objectToSend);
        }

        return MatchingResult.forUncertain(null, Set.of());
    }

    public List<JsonListStructure> generateJson(PrismContainerValue<ShadowAttributesType> attributes) {

        List<JsonListStructure> jsonList = new ArrayList<>();

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
            generator.writeEndObject();
            generator.close();
            jsonList.add(new JsonListStructure(sorLabel, uid, String.valueOf(jsonString)));
        } catch (IOException e) {
            //TODO throw exception
            e.printStackTrace();
        }

        return jsonList;
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
