/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import com.evolveum.midpoint.prism.ParsingContext;

/**
 * Result for deserialization of prism values stored in the repository.
 * It can be whole object or part of object, e.g. delta or any other prism structure.
 */
public class RepositoryObjectParseResult<T> {

    public final ParsingContext parsingContext;
    public final T prismValue;

    public RepositoryObjectParseResult(ParsingContext parsingContext, T prismValue) {
        this.parsingContext = parsingContext;
        this.prismValue = prismValue;
    }
}
