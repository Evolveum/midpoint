/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
