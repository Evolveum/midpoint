/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.opts.SchemaOptions;
import org.apache.commons.lang.NotImplementedException;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SchemaRepositoryAction extends RepositoryAction<SchemaOptions> {

    @Override
    public void execute() throws Exception {
        // todo implement import-sql (create schema) or validate-schema

        throw new NotImplementedException("Feel free to create pull request :)");
    }
}
