/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.polystring;

import javax.xml.namespace.QName;

/**
 * Normalizer for PolyStrings. Classes implementing this interface are able to take an
 * original string (in the PolyString sense) and return a normalized version of the
 * string.
 *
 * @see PolyString
 * @author Radovan Semancik
 */
public interface PolyStringNormalizer {

	/**
	 * Returns a normalized version of the string.
	 * @return normalized version of the string
	 */
	String normalize(String orig);

	QName getName();
}
