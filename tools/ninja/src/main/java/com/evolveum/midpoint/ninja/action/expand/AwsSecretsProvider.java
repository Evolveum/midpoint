/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.expand;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

public class AwsSecretsProvider implements SecretsProvider {

    private static final String COMMAND = "aws secretsmanager get-secret-value --secret-id";

    private final AwsProviderOptions options;

    private Map<String, String> cache = new HashMap<>();

    public AwsSecretsProvider(@NotNull AwsProviderOptions options) {
        this.options = options;
    }

    @Override
    public synchronized String getValue(@NotNull String key) {
        if (options.isCacheValues()) {
            String cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
        }

        List<String> commands = Arrays.asList(COMMAND.split(" "));
        commands.add(key);

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(commands);

        try {
            Process process = builder.start();
            boolean done = process.waitFor(options.getTimeout(), TimeUnit.SECONDS);
            if (!done) {
                throw new ExpanderException("Couldn't get secret value for " + key + ", timeout reached");
            }

            InputStream is = process.getInputStream();
            String response = IOUtils.toString(is, StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(response);
            JsonNode secret = node.get("SecretString");

            String value = secret != null ? secret.asText() : null;
            if (options.isCacheValues()) {
                cache.put(key, value);
            }

            return value;
        } catch (Exception ex) {
            throw new ExpanderException("Couldn't get secret value for " + key + ", reason: " + ex.getMessage(), ex);
        }
    }
}
