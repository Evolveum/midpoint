/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api;

import java.io.Serializable;

import com.evolveum.midpoint.schema.AbstractOptions;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * Options for {@link RepositoryService#addObject}.
 */
public class RepoAddOptions extends AbstractOptions implements Serializable, ShortDumpable {
    private static final long serialVersionUID = -6243926109579064467L;

    /**
     * Allows overwriting existing object *of the same type*.
     * Overwriting different type is not allowed because it is unlikely done on purpose.
     */
    private boolean overwrite = false;
    private boolean allowUnencryptedValues = false;

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public static boolean isOverwrite(RepoAddOptions options) {
        if (options == null) {
            return false;
        }
        return options.isOverwrite();
    }

    public static RepoAddOptions createOverwrite() {
        RepoAddOptions opts = new RepoAddOptions();
        opts.setOverwrite(true);
        return opts;
    }

    public boolean isAllowUnencryptedValues() {
        return allowUnencryptedValues;
    }

    public void setAllowUnencryptedValues(boolean allowUnencryptedValues) {
        this.allowUnencryptedValues = allowUnencryptedValues;
    }

    public static boolean isAllowUnencryptedValues(RepoAddOptions options) {
        if (options == null) {
            return false;
        }
        return options.isAllowUnencryptedValues();
    }

    public static RepoAddOptions createAllowUnencryptedValues() {
        RepoAddOptions opts = new RepoAddOptions();
        opts.setAllowUnencryptedValues(true);
        return opts;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RepoAddOptions(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        appendFlag(sb, "overwrite", overwrite);
        appendFlag(sb, "allowUnencryptedValues", allowUnencryptedValues);
        removeLastComma(sb);
    }

}
