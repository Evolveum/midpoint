/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import com.google.common.annotations.VisibleForTesting;

import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.soap.Detail;

/**
 * TEMPORARY.
 *
 * This interface belongs to a coursebook on Software Engineering as a horrific design example ;)
 *
 * Prism API and/or client code should be modified to get rid of these hacks.
 */
public interface Hacks {

    void serializeFaultMessage(Detail detail, Object faultInfo, QName faultMessageElementName, Trace logger);

    @VisibleForTesting
    <T> void parseProtectedType(ProtectedDataType<T> protectedType, MapXNode xmap, PrismContext prismContext, ParsingContext pc) throws SchemaException;

    Element serializeSingleElementMapToElement(MapXNode filterClauseXNode) throws SchemaException;
}
