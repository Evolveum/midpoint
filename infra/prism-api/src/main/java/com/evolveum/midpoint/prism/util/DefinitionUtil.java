/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.util;

import com.evolveum.midpoint.util.QNameUtil;
import org.apache.commons.lang.StringUtils;

import javax.xml.namespace.QName;

/**
 *
 */
public class DefinitionUtil {
	public static final String MULTIPLICITY_UNBOUNDED = "unbounded";

	public static Integer parseMultiplicity(String stringMultiplicity) {
		if (stringMultiplicity == null) {
			return null;
		}
		if (stringMultiplicity.equals(MULTIPLICITY_UNBOUNDED)) {
			return -1;
		}
		return Integer.parseInt(stringMultiplicity);
	}

	// add namespace from the definition if it's safe to do so
    public static QName addNamespaceIfApplicable(QName name, QName definitionName) {
        if (StringUtils.isEmpty(name.getNamespaceURI())) {
            if (QNameUtil.match(name, definitionName)) {
                return definitionName;
            }
        }
        return name;
    }
}
