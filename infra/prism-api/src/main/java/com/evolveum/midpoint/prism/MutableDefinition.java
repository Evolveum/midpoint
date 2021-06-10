/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;

import javax.xml.namespace.QName;

/**
 *
 */
public interface MutableDefinition extends Definition {

    void setProcessing(ItemProcessing processing);

    void setDeprecated(boolean deprecated);

    void setExperimental(boolean experimental);

    void setEmphasized(boolean emphasized);

    void setDisplayName(String displayName);

    void setDisplayOrder(Integer displayOrder);

    void setHelp(String help);

    void setRuntimeSchema(boolean value);

    void setTypeName(QName typeName);

    void setDocumentation(String value);

    void addSchemaMigration(SchemaMigration schemaMigration);

    void addDiagram(ItemDiagramSpecification diagram);
}
