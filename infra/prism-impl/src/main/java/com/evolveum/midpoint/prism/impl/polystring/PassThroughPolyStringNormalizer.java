/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.polystring;

import com.evolveum.midpoint.prism.PrismConstants;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class PassThroughPolyStringNormalizer extends AbstractPolyStringNormalizer {

    @Override
    protected String normalizeCore(String s) {
        return s;
    }

    @Override
    public QName getName() {
        return PrismConstants.PASSTHROUGH_POLY_STRING_NORMALIZER;
    }
}
