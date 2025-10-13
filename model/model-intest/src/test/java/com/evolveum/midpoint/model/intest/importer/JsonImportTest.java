/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.importer;

import com.evolveum.midpoint.prism.PrismContext;

public class JsonImportTest extends AbstractImportTest {

    @Override
    String getSuffix() {
        return "json";
    }

    @Override
    String getLanguage() {
        return PrismContext.LANG_JSON;
    }
}
