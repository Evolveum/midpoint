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

import javax.xml.datatype.Duration;

import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.MiscUtil;

public class DurationConverter implements IConverter<Duration>{

	private static final long serialVersionUID = 1L;

	@Override
	public Duration convertToObject(String value, Locale locale) throws ConversionException {
		return XmlTypeConverter.createDuration(MiscUtil.nullIfEmpty(value));
	}

	@Override
	public String convertToString(Duration value, Locale locale) {
		return value.toString();
	}

}
