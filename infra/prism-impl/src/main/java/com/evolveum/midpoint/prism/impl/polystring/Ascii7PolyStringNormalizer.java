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
public class Ascii7PolyStringNormalizer extends AbstractPolyStringNormalizer {
	
	@Override
	public String normalizeCore(String s) {
		s = removeAll(s, 0x20, 0x7f);
		return s;
	}

	@Override
	public QName getName() {
		return PrismConstants.ASCII7_POLY_STRING_NORMALIZER;
	}
}
