/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.web.component.util.SerializableFunction;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for {@link SmartSuggestConfirmationPanel}, holding the confirmation message and a list of permission records.
 */
public class SmartPermissionRecordDto implements Serializable {

    /** Confirmation message displayed at the top of the smart suggestion dialog. */
    IModel<String> confirmationMessage;
    List<PermissionRecord> records;

    /** Represents a single permission entry with title, description, selection state, and optional click handler. */
    public record PermissionRecord(
            String title,
            String description,
            boolean selected,
            SerializableFunction<PermissionRecord, Void> onClick) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    public SmartPermissionRecordDto(IModel<String> confirmationMessage, List<PermissionRecord> records) {
        this.confirmationMessage = confirmationMessage;
        this.records = records;
    }

    public IModel<String> getConfirmationMessage() {
        return confirmationMessage;
    }

    public List<PermissionRecord> getRecords() {
        return records;
    }

    public void addRecord(PermissionRecord record) {
        if (this.records == null) {
            this.records = new ArrayList<>();
        }
        this.records.add(record);
    }

    // TODO Dummy data for permissions (replace later with real dto structured data)
    public static List<PermissionRecord> initDummyObjectTypePermissionData() {
        List<PermissionRecord> records = initDummyCorrelationPermissionData();
        records.add(new PermissionRecord(
                "Statistical data",
                "Allow collection of statistical data about resource usage patterns.",
                true,
                null
        ));
        return records;
    }

    public static @NotNull List<PermissionRecord> initDummyCorrelationPermissionData() {
        List<PermissionRecord> records = new ArrayList<>();
        records.add(new PermissionRecord(
                "Schema access",
                "Allow access to the resource schema for analysis and suggestions.",
                true,
                null
        ));
        return records;
    }

    public static @NotNull List<PermissionRecord> initDummyMappingPermissionData() {
        List<PermissionRecord> records = initDummyCorrelationPermissionData();

        records.add(new PermissionRecord(
                "Raw data",
                "Allow access to raw data on the resource for detailed analysis.",
                true,
                null
        ));
        return records;
    }
}
