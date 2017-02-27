/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.rest;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import com.evolveum.midpoint.prism.PrismParser;
import com.evolveum.midpoint.prism.PrismSerializer;

@Produces({"application/xml", "application/*+xml", "text/xml"})
@Consumes({"application/xml", "application/*+xml", "text/xml"})
@Provider
public class MidpointXmlProvider<T> extends MidpointAbstractProvider<T> {

	@Override
	protected PrismSerializer<String> getSerializer() {
		return prismContext.xmlSerializer();
	}

	@Override
	protected PrismParser getParser(InputStream entityStream) {
		return prismContext.parserFor(entityStream).xml();
	}

}
