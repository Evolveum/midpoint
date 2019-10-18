/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.converter;

import java.util.Locale;

import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

/**
 * @author katkav
 *
 */
public class CleanupPoliciesTypeConverter implements IConverter<QueryType> {

    private static final long serialVersionUID = 1L;
    private PrismContext prismContext;

    public CleanupPoliciesTypeConverter(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    public QueryType convertToObject(String arg0, Locale arg1) throws ConversionException {
        try {
            return prismContext.parserFor(arg0).parseRealValue();
        } catch (SchemaException e) {
            throw new ConversionException(e);
        }
    }

    @Override
    public String convertToString(QueryType value, Locale arg1) {
         try {
             return prismContext.xmlSerializer().serializeAnyData(value);
         } catch (SchemaException e) {
             throw new SystemException(
                 "Couldn't serialize property value of type: " + value + ": " + e.getMessage(), e);
         }
    }

}
