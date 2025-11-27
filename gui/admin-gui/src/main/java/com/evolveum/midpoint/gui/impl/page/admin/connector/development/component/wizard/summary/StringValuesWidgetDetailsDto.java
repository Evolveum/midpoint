/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record StringValuesWidgetDetailsDto(@NotNull Map<IModel<String>, IModel<String>> values) implements ContainerWithStatusWidgetDetails{
}
