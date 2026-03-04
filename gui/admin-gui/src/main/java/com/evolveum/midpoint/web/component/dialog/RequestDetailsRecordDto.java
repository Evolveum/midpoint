/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.util.SerializableFunction;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for {@link RequestDetailsConfirmationPanel}, holding the confirmation message and a list of request/permission records.
 */
public class RequestDetailsRecordDto<T extends Serializable> implements Serializable {

    /** Confirmation message displayed at the top of the smart suggestion dialog. */
    IModel<String> confirmationMessage;
    List<RequestRecord<T>> records;

    /** Represents a single request entry with title, description, selection state, and optional click handler. */
    public record RequestRecord<T extends Serializable>(
            String title,
            String description,
            IModel<Boolean> selected,
            T option,
            SerializableFunction<RequestRecord<T>, Void> onClick
    ) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        public boolean isSelected() {
            return selected.getObject();
        }

        public void setSelected(boolean selected) {
            this.selected.setObject(selected);
        }
    }

    public RequestDetailsRecordDto(IModel<String> confirmationMessage, List<RequestRecord<T>> records) {
        this.confirmationMessage = confirmationMessage;
        this.records = records;
    }

    public IModel<String> getConfirmationMessage() {
        return confirmationMessage;
    }

    public IModel<String> getRequestLabelModel(@NotNull PageBase pageBase) {
        return pageBase.createStringResource("SmartSuggestConfirmationPanel.request.component.title");
    }

    protected StringResourceModel getSubtitleModel(@NotNull PageBase pageBase) {
        return pageBase.createStringResource("SmartSuggestConfirmationPanel.subtitle");
    }

    public List<RequestRecord<T>> getRecords() {
        return records;
    }

    public void addRecord(RequestRecord<T> record) {
        if (this.records == null) {
            this.records = new ArrayList<>();
        }
        this.records.add(record);
    }

    // TODO Dummy data for requests (replace later with real dto structured data)
    public static List<RequestRecord<DataAccessPermission>> initDummyObjectTypePermissionData() {
        List<RequestRecord<DataAccessPermission>> records = initDummyCorrelationPermissionData();
        records.add(new RequestRecord<>(
                "Statistical data",
                "Allow collection of statistical data about resource usage patterns.",
                Model.of(true),
                DataAccessPermission.STATISTICS_ACCESS,
                null
        ));
        return records;
    }

    public static @NotNull List<RequestRecord<DataAccessPermission>> initDummyCorrelationPermissionData() {
        List<RequestRecord<DataAccessPermission>> records = new ArrayList<>();
        records.add(new RequestRecord<>(
                "Schema access",
                "Allow access to the resource schema for analysis and suggestions.",
                Model.of(true),
                DataAccessPermission.SCHEMA_ACCESS,
                null
        ));
        return records;
    }

    public static @NotNull List<RequestRecord<DataAccessPermission>> initDummyMappingPermissionData() {
        List<RequestRecord<DataAccessPermission>> records = initDummyCorrelationPermissionData();

        records.add(new RequestRecord<>(
                "Raw data",
                "Allow access to raw data on the resource for detailed analysis.",
                Model.of(true),
                DataAccessPermission.RAW_DATA_ACCESS,
                null
        ));
        return records;
    }

}
