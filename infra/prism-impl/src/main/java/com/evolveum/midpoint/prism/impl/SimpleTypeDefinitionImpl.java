/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.MutableDefinition;
import com.evolveum.midpoint.prism.MutableTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.SimpleTypeDefinition;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class SimpleTypeDefinitionImpl extends TypeDefinitionImpl implements SimpleTypeDefinition, MutableTypeDefinition {

    private QName baseTypeName;
    private DerivationMethod derivationMethod;        // usually RESTRICTION

    public SimpleTypeDefinitionImpl(QName typeName, QName baseTypeName, DerivationMethod derivationMethod,
            PrismContext prismContext) {
        super(typeName, prismContext);
        this.baseTypeName = baseTypeName;
        this.derivationMethod = derivationMethod;
    }

    @Override
    public void revive(PrismContext prismContext) {
    }

    public String getDebugDumpClassName() {
        return "STD";
    }

    @Override
    public String getDocClassName() {
        return "simple type";
    }

    public QName getBaseTypeName() {
        return baseTypeName;
    }

    @Override
    public DerivationMethod getDerivationMethod() {
        return derivationMethod;
    }

    @NotNull
    @Override
    public SimpleTypeDefinitionImpl clone() {
        SimpleTypeDefinitionImpl clone = new SimpleTypeDefinitionImpl(typeName, baseTypeName, derivationMethod, prismContext);
        super.copyDefinitionData(clone);
        return clone;
    }

    @Override
    public MutableDefinition toMutable() {
        checkMutableOnExposing();
        return this;
    }
}
