/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.expand;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import com.evolveum.midpoint.ninja.util.EnumConverterValidator;

@Parameters(resourceBundle = "messages")
public class ExpanderOptions {

    public enum SecretsProvider {

        KEEPASS,

        AWS
    }

    public static class SecretsProviderConverter extends EnumConverterValidator<SecretsProvider> {

        public SecretsProviderConverter() {
            super(SecretsProvider.class);
        }
    }

    public static final String P_EXPAND = "--expand";

    public static final String P_SECRETS_PROVIDER_LONG = "--secrets-provider";

    public static final String P_PROPERTIES_LONG = "--properties";

    public static final String P_IGNORE_MISSING = "--ignore-missing";

    @Parameter(names = { P_EXPAND }, descriptionKey = "expand.expand")
    private boolean expand;

    @Parameter(names = { P_SECRETS_PROVIDER_LONG }, descriptionKey = "expand.secretsProvider", converter = SecretsProviderConverter.class)
    private SecretsProvider secretsProvider;

    @Parameter(names = { P_PROPERTIES_LONG }, descriptionKey = "expand.properties")
    private File properties;

    @Parameter(names = { P_IGNORE_MISSING }, descriptionKey = "expand.ignoreMissing")
    private boolean ignoreMissing;

    @ParametersDelegate
    private KeepassProviderOptions keepassProviderOptions = new KeepassProviderOptions();

    @ParametersDelegate
    private AwsProviderOptions awsProviderOptions = new AwsProviderOptions();

    public boolean isExpand() {
        return expand;
    }

    public void setExpand(boolean expand) {
        this.expand = expand;
    }

    public SecretsProvider getSecretsProvider() {
        return secretsProvider;
    }

    public void setSecretsProvider(SecretsProvider secretsProvider) {
        this.secretsProvider = secretsProvider;
    }

    public File getProperties() {
        return properties;
    }

    public void setProperties(File properties) {
        this.properties = properties;
    }

    public boolean isIgnoreMissing() {
        return ignoreMissing;
    }

    public void setIgnoreMissing(boolean ignoreMissing) {
        this.ignoreMissing = ignoreMissing;
    }

    public KeepassProviderOptions getKeepassProviderOptions() {
        return keepassProviderOptions;
    }

    public void setKeepassProviderOptions(KeepassProviderOptions keepassProviderOptions) {
        this.keepassProviderOptions = keepassProviderOptions;
    }

    public AwsProviderOptions getAwsProviderOptions() {
        return awsProviderOptions;
    }

    public void setAwsProviderOptions(AwsProviderOptions awsProviderOptions) {
        this.awsProviderOptions = awsProviderOptions;
    }
}
