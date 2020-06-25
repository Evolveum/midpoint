/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.google.common.annotations.VisibleForTesting;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;

/**
 * TEMPORARY.
 * <p>
 * This interface belongs to a coursebook on Software Engineering as a horrific design example ;)
 * <p>
 * Prism API and/or client code should be modified to get rid of these hacks.
 */
public interface Hacks {

    @VisibleForTesting
    <T> void parseProtectedType(ProtectedDataType<T> protectedType, MapXNode xmap, PrismContext prismContext, ParsingContext pc) throws SchemaException;
}
