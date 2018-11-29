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
