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

package com.evolveum.midpoint.prism.schema;

import com.evolveum.midpoint.util.DebugDumpable;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import java.io.InputStream;
import java.util.Map;

/**
 *
 */
public interface SchemaDescription extends DebugDumpable {

	String getPath();

	void setResourcePath(String path);

	String getNamespace();

	void setNamespace(String namespace);

	String getUsualPrefix();

	void setUsualPrefix(String usualPrefix);

	String getSourceDescription();

	void setSourceDescription(String sourceDescription);

	void setPath(String path);

	boolean isPrismSchema();

	void setPrismSchema(boolean isMidPointSchema);

	boolean isDefault();

	void setDefault(boolean isDefault);

	boolean isDeclaredByDefault();

	void setDeclaredByDefault(boolean isDeclaredByDefault);

	PrismSchema getSchema();

	void setSchema(PrismSchema schema);

	Package getCompileTimeClassesPackage();

	void setCompileTimeClassesPackage(Package compileTimeClassesPackage);

	Map<QName, Class<?>> getXsdTypeTocompileTimeClassMap();

	void setXsdTypeTocompileTimeClassMap(Map<QName, Class<?>> xsdTypeTocompileTimeClassMap);

	boolean canInputStream();

	InputStream openInputStream();

	Source getSource();

	Element getDomElement();
}
