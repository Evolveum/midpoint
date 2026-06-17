/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.component.dialog;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.web.component.dialog.privacy.DataAccessPermission;
import com.evolveum.midpoint.web.component.util.Describable;
import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;

/**
 * Represents a single confirmation option entry with selection state, and optional click handler.
 */
public record ConfirmationOption<T extends Describable>(
        IModel<Boolean> selected,
        T option,
        SerializableBiConsumer<ConfirmationOption<T>, AjaxRequestTarget> onClick
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static List<ConfirmationOption<DataAccessPermission>> delineationPermissionsOptions() {
        List<ConfirmationOption<DataAccessPermission>> records = correlationPermissionsOptions();
        records.add(new ConfirmationOption<>(
                Model.of(true),
                DataAccessPermission.STATISTICS_ACCESS,
                null
        ));
        return records;
    }

    public static @NotNull List<ConfirmationOption<DataAccessPermission>> correlationPermissionsOptions() {
        List<ConfirmationOption<DataAccessPermission>> records = new ArrayList<>();
        records.add(new ConfirmationOption<>(
                Model.of(true),
                DataAccessPermission.SCHEMA_ACCESS,
                null
        ));
        return records;
    }

    public static @NotNull List<ConfirmationOption<DataAccessPermission>> mappingPermissionsOptions() {
        List<ConfirmationOption<DataAccessPermission>> records = correlationPermissionsOptions();

        records.add(new ConfirmationOption<>(
                Model.of(true),
                DataAccessPermission.RAW_DATA_ACCESS,
                null
        ));
        return records;
    }

    public static <T extends Describable> ConfirmationOption<T> selectedOf(T option) {
        return new ConfirmationOption<>(Model.of(true), option, null);
    }

    public boolean isSelected() {
        return selected.getObject();
    }

    public void setSelected(boolean selected) {
        this.selected.setObject(selected);
    }

    public IModel<String> title() {
        return option.title();
    }

    public IModel<String> description() {
        return option.description();
    }

}
