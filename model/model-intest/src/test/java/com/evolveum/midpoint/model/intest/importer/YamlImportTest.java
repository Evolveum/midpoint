/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.importer;

import com.evolveum.midpoint.prism.PrismContext;

public class YamlImportTest extends AbstractImportTest {

    @Override
    String getSuffix() {
        return "yaml";
    }

    @Override
    String getLanguage() {
        return PrismContext.LANG_YAML;
    }
}
