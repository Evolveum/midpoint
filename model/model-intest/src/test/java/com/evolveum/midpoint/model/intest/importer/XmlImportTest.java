/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.importer;

import com.evolveum.midpoint.prism.PrismContext;

/**
 * @author mederly
 */
public class XmlImportTest extends AbstractImportTest {

    @Override
    String getSuffix() {
        return "xml";
    }

    @Override
    String getLanguage() {
        return PrismContext.LANG_XML;
    }
}
