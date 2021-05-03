/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.PrismObject;

/** Result for deserialization of prism object stored in the repository. */
public class RepositoryObjectParseResult<T extends Objectable> {

    public final ParsingContext parsingContext;
    public final PrismObject<T> prismObject;

    public RepositoryObjectParseResult(ParsingContext parsingContext, PrismObject<T> prismObject) {
        this.parsingContext = parsingContext;
        this.prismObject = prismObject;
    }
}
