/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import org.apache.wicket.model.IModel;
import org.wicketstuff.select2.ChoiceProvider;

import java.util.Collection;

public class Select2MultiChoiceColumnPanel<T> extends Select2MultiChoicePanel {

    public Select2MultiChoiceColumnPanel(String id, IModel<Collection<T>> model, ChoiceProvider<T> provider) {
        super(id, model, provider);
    }

    protected boolean isInColumn() {
        return true;
    }
}
