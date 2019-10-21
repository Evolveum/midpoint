/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.connector;

import java.util.ArrayList;
import java.util.List;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;

/**
 * @author Radovan Semancik
 *
 */
public class DummyFilterTranslator implements FilterTranslator<String> {

    private static final Log LOG = Log.getLog(DummyFilterTranslator.class);

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.common.objects.filter.FilterTranslator#translate(org.identityconnectors.framework.common.objects.filter.Filter)
     */
    public List<String> translate(Filter filter) {
        LOG.info("translate::begin");

        LOG.info("translate::filter: {0}",filter == null ? "null" : filter.toString());

        LOG.info("translate::end");
        return new ArrayList<>();
    }

}
