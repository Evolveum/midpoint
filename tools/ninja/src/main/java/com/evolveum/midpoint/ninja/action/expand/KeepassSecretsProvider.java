/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.expand;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.linguafranca.pwdb.Database;
import org.linguafranca.pwdb.Entry;
import org.linguafranca.pwdb.kdbx.KdbxCreds;
import org.linguafranca.pwdb.kdbx.dom.DomDatabaseWrapper;

import com.evolveum.midpoint.ninja.util.InputParameterException;

public class KeepassSecretsProvider implements SecretsProvider {

    private final KeepassProviderOptions options;

    private Database database;

    public KeepassSecretsProvider(@NotNull KeepassProviderOptions options) {
        this.options = options;
    }

    @Override
    public void init() {
        File file = options.getFile();
        if (file == null) {
            throw new InputParameterException("Keepass file is not specified");
        }

        String password = options.getKeepassPassword();
        if (StringUtils.isEmpty(password)) {
            throw new InputParameterException("Keepass password is not specified");
        }

        try (InputStream is = new FileInputStream(file)) {
            database = DomDatabaseWrapper.load(new KdbxCreds(password.getBytes()), is);
        } catch (Exception ex) {
            throw new ExpanderException("Couldn't open keepass file, reason: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getValue(@NotNull String key) {
        if (database == null) {
            throw new ExpanderException("Keepass database is not initialized");
        }

        List<Entry> entries = database.findEntries(e -> Objects.equals(e.getTitle(), key));
        if (entries.isEmpty()) {
            return null;
        }

        if (entries.size() > 1) {
            throw new ExpanderException("Found more than one entry for key '" + key + "'");
        }

        return entries.get(0).getPassword();
    }
}
