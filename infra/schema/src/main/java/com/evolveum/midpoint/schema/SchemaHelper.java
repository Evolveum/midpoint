/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * TEMPORARY
 */
@Component
public class SchemaHelper {

    private static SchemaHelper instance;

    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static SchemaHelper get() {
        return instance;
    }

    public PrismContext prismContext() {
        return prismContext;
    }

    public RelationRegistry relationRegistry() {
        return relationRegistry;
    }

    public GetOperationOptionsBuilder getOperationOptionsBuilder() {
        return new GetOperationOptionsBuilderImpl(prismContext);
    }
}
