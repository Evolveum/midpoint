/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
