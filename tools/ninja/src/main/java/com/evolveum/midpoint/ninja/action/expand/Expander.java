/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.expand;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class Expander {

    public static final Pattern PATTERN = Pattern.compile("\\$\\((.+?)\\)");

    private final ExpanderOptions options;

    private SecretsProvider secretsProvider;

    private Properties properties = new Properties();

    private boolean ignoreMissingKeys;

    private boolean expandEncrypted = true;

    private Charset charset = StandardCharsets.UTF_8;

    public Expander(@NotNull ExpanderOptions options) {
        this.options = options;
    }

    public void init() {
        File file = options.getProperties();
        if (file != null) {
            try (Reader reader = new FileReader(file, charset)) {
                properties.load(reader);
            } catch (IOException ex) {
                throw new ExpanderException("Couldn't load properties from file '" + file.getPath() + "'", ex);
            }
        }

        ExpanderOptions.SecretsProvider provider = options.getSecretsProvider();
        if (provider != null) {
            try {
                switch (provider) {
                    case KEEPASS:
                        secretsProvider = new KeepassSecretsProvider(options.getKeepassProviderOptions());
                        break;
                    case AWS:
                        secretsProvider = new AwsSecretsProvider(options.getAwsProviderOptions());
                        break;
                }

                secretsProvider.init();
            } catch (Exception ex) {
                throw new ExpanderException("Couldn't initialize secrets provider '" + provider + "'", ex);
            }
        }
    }

    public void destroy() {
        try {
            if (secretsProvider != null) {
                secretsProvider.destroy();
            }
        } catch (Exception ex) {
            throw new ExpanderException("Couldn't destroy secrets provider", ex);
        }
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public boolean isIgnoreMissingKeys() {
        return ignoreMissingKeys;
    }

    public void setIgnoreMissingKeys(boolean ignoreMissingKeys) {
        this.ignoreMissingKeys = ignoreMissingKeys;
    }

    public boolean isExpandEncrypted() {
        return expandEncrypted;
    }

    public void setExpandEncrypted(boolean expandEncrypted) {
        this.expandEncrypted = expandEncrypted;
    }

    public String expand(String object) {
        return expand(object, null);
    }

    public String expand(String object, File file) {
        if (object == null) {
            return null;
        }

        Matcher matcher = PATTERN.matcher(object);

        Set<String> missingKeys = new HashSet<>();

        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (key.isEmpty()) {
                matcher.appendReplacement(sb, "");
                continue;
            }

            String value = expandKey(key, file, expandEncrypted);
            if (value == null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
                missingKeys.add(key);
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
            }
        }
        matcher.appendTail(sb);

        if (!ignoreMissingKeys && !missingKeys.isEmpty()) {
            throw new MissingKeyException(missingKeys, "Couldn't translate keys: " + Arrays.toString(missingKeys.toArray()));
        }

        return sb.toString();
    }

    private String getFilePathFromKey(String key) {
        if (key == null) {
            return null;
        }

        key = key.trim();
        if (!key.startsWith("@")) {
            return null;
        }

        return key.replaceFirst("@", "").trim();
    }

    public String expandKey(String key) {
        return expandKey(key, null, expandEncrypted);
    }

    private String expandFilePath(String key, String filePath, File file) {
        filePath = filePath.replace("\\", "/");

        Path uri = Path.of(filePath);
        if (uri.isAbsolute()) {
            File content = uri.toFile();
            return loadContent(content, key, filePath, null);
        } else {
            if (file != null) {
                if (!file.isDirectory()) {
                    file = file.getParentFile();
                }

                File content = file.toPath().resolve(filePath).toFile();
                return loadContent(content, key, filePath, file);
            } else {
                if (ignoreMissingKeys) {
                    return null;
                }
                throw new MissingKeyException(key, "Couldn't load file '" + key + "', unknown path '" + key + "'");
            }
        }
    }

    private String expandKey(String key, File file, boolean expandEncrypted) {
        String filePath = getFilePathFromKey(key);
        if (filePath != null) {
            return expandFilePath(key, filePath, file);
        }

        String value = properties.getProperty(key);
        if (value != null) {
            return value;
        }

        if (!expandEncrypted || secretsProvider == null) {
            return expandKeyFromProperties(key, ignoreMissingKeys);
        }

        value = secretsProvider.getValue(key);
        if (value != null) {
            return value;
        }

        return expandKeyFromProperties(key, ignoreMissingKeys);
    }

    private String expandKeyFromProperties(String key, boolean ignoreMissingKeys) {
        String value = properties.getProperty(key);
        if (value != null) {
            return value;
        }

        if (ignoreMissingKeys) {
            return null;
        }

        throw new MissingKeyException(key, "Couldn't translate key '" + key + "'");
    }

    private String loadContent(File file, String key, String contentFilePath, File contentParent) {
        if (file == null) {
            if (ignoreMissingKeys) {
                return null;
            }
            throw new MissingKeyException(key, "Can't load content for key '" + key + "', file '" + contentFilePath + "' is not present in '" + contentParent + "'");
        }

        if (file.isDirectory()) {
            if (ignoreMissingKeys) {
                return null;
            }
            throw new MissingKeyException(key, "Can't load content for key '" + key + "', file '" + file.getPath() + "' is directory");
        }

        try {
            return IOUtils.toString(file.toURI(), charset);
        } catch (IOException ex) {
            if (ignoreMissingKeys) {
                return null;
            }
            throw new MissingKeyException(key, "Couldn't load content for key '" + key + "', file '" + file.getPath() + "', reason: " + ex.getMessage());
        }
    }
}
