/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.action.mining.generator;

import com.beust.jcommander.Parameters;

import com.evolveum.midpoint.ninja.action.BasicGeneratorOptions;

/**
 * Options for generating mining data.
 * These options extend the basic generator options.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
@Parameters(resourceBundle = "messages", commandDescriptionKey = "generatorRbac")
public class GeneratorOptions extends BaseGeneratorOptions implements BasicGeneratorOptions {

    @Override
    public boolean isOverwrite() {
        return false;
    }
}
