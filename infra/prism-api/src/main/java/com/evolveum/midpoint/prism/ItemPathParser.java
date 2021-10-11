/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 *
 */
public interface ItemPathParser {

    ItemPathType asItemPathType(String value);

    UniformItemPath asItemPath(String value);
}
