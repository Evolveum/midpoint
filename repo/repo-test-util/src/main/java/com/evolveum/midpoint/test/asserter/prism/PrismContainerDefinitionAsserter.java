/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.PrettyPrinter;


/**
 * Related to ComplexTypeDefinitionAsserter
 *
 * @author Radovan semancik
 */
public class PrismContainerDefinitionAsserter<C extends Containerable,RA> extends PrismDefinitionAsserter<RA> {

    public PrismContainerDefinitionAsserter(PrismContainerDefinition<C> definition) {
        super(definition);
    }

    public PrismContainerDefinitionAsserter(PrismContainerDefinition<C> definition, String detail) {
        super(definition, detail);
    }

    public PrismContainerDefinitionAsserter(PrismContainerDefinition<C> definition, RA returnAsserter, String detail) {
        super(definition, returnAsserter, detail);
    }

    public static <C extends Containerable> PrismContainerDefinitionAsserter<C,Void> forContainerDefinition(PrismContainerDefinition<C> definition) {
        return new PrismContainerDefinitionAsserter<>(definition);
    }

    public PrismContainerDefinition<C> getDefinition() {
        return (PrismContainerDefinition<C>) super.getDefinition();
    }

    public PrismContainerDefinitionAsserter<C,RA> assertSize(int expected) {
        assertEquals("Wrong number of item definitions in "+desc(), expected, getDefinition() != null ? getDefinition().getDefinitions().size() : 0);
        return this;
    }

    public <CC extends Containerable> PrismContainerDefinitionAsserter<CC,PrismContainerDefinitionAsserter<C,RA>> container(ItemPath subDefPath) {
        PrismContainerDefinition<CC> subDefinition = getDefinition().findContainerDefinition(subDefPath);
        assertNotNull("No container for path "+subDefPath+" in "+desc(), subDefinition);
        PrismContainerDefinitionAsserter<CC,PrismContainerDefinitionAsserter<C,RA>> subAsserter = new PrismContainerDefinitionAsserter<>(subDefinition, this, "container for "+subDefPath+" in "+desc());
        copySetupTo(subAsserter);
        return subAsserter;
    }

    protected String desc() {
        return descWithDetails("container definition " + PrettyPrinter.prettyPrint(getDefinition().getItemName()));
    }

    public PrismContainerDefinitionAsserter<C,RA> display() {
        display(desc());
        return this;
    }

    public PrismContainerDefinitionAsserter<C,RA> display(String message) {
        IntegrationTestTools.display(message, getDefinition());
        return this;
    }
}
