/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.marshaller;

import com.evolveum.midpoint.prism.path.UniformItemPath;
import org.w3c.dom.Element;

/**
 *
 */
public class ItemPathParserTemp {
	public static UniformItemPath parseFromString(String string) {
		return ItemPathHolder.parseFromString(string);
	}

	public static UniformItemPath parseFromElement(Element element) {
		return ItemPathHolder.parseFromElement(element);
	}
}
