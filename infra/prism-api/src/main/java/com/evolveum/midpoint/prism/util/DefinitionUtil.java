/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
