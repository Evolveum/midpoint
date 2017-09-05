/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.prism;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
public interface Objectable extends Containerable {

	String getOid();

	void setOid(String oid);

	String getVersion();

	void setVersion(String version);

	PolyStringType getName();

	void setName(PolyStringType name);

	String getDescription();

	void setDescription(String description);

	/**
	 * Returns short string representing identity of this object.
	 * It should container object type, OID and name. It should be presented
	 * in a form suitable for log and diagnostic messages (understandable for
	 * system administrator).
	 */
	String toDebugName();

	/**
	 * Returns short string identification of object type. It should be in a form
	 * suitable for log messages. There is no requirement for the type name to be unique,
	 * but it rather has to be compact. E.g. short element names are preferred to long
	 * QNames or URIs.
	 * @return
	 */
	String toDebugType();

    PrismObject asPrismObject();

    void setupContainer(PrismObject object);

//	public <O extends Objectable> PrismObject<O> asPrismObject();
//
//    public <O extends Objectable> void setupContainer(PrismObject<O> object);
}
