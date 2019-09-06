/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
