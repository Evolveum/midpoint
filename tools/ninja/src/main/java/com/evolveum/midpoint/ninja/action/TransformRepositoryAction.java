/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.opts.TransformOptions;
import org.apache.commons.lang.NotImplementedException;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TransformRepositoryAction extends RepositoryAction<TransformOptions> {

    @Override
    public void execute() throws Exception {
        // todo xml/json/yaml transformations

        throw new NotImplementedException("Feel free to create pull request :)");
    }
}
