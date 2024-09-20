/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.util.ItemPathConverter;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * Created by Viliam Repan (lazyman).
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "export")
public class ExportOptions extends BaseImportExportOptions implements BasicExportOptions {

    public static final String P_OUTPUT = "-o";
    public static final String P_OUTPUT_LONG = "--output";

    public static final String P_OVERWRITE = "-O";
    public static final String P_OVERWRITE_LONG = "--overwrite";

    public static final String P_NO_IDS = "-ni";
    public static final String P_NO_IDS_LONG = "--no-container-ids";

    public static final String P_EXCLUDE_ITEMS = "-ei";
    public static final String P_EXCLUDE_ITEMS_LONG = "--exclude-item";

    @Parameter(names = { P_OUTPUT, P_OUTPUT_LONG }, descriptionKey = "export.output")
    private File output;

    @Parameter(names = { P_OVERWRITE, P_OVERWRITE_LONG }, descriptionKey = "export.overwrite")
    private boolean overwrite;

    @Parameter(names = { P_NO_IDS, P_NO_IDS_LONG }, descriptionKey = "export.skipids")
    private boolean skipIds;

    @Parameter(names = { P_EXCLUDE_ITEMS, P_EXCLUDE_ITEMS_LONG }, descriptionKey = "export.exclude.items",
            validateWith = ItemPathConverter.class, converter = ItemPathConverter.class)
    private List<ItemPath> excludeItems = new ArrayList<>();

    @Override
    public File getOutput() {
        return output;
    }

    @Override
    public boolean isOverwrite() {
        return overwrite;
    }

    public boolean isSkipContainerIds() {
        return skipIds;
    }

    public ExportOptions setOutput(File output) {
        this.output = output;
        return this;
    }

    public ExportOptions setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    public ExportOptions setSkipIds(boolean skipIds) {
        this.skipIds = skipIds;
        return this;
    }

    public List<ItemPath> getExcludeItems() {
        return excludeItems;
    }

    public void setExcludeItems(List<ItemPath> excludeItems) {
        this.excludeItems = excludeItems;
    }
}
