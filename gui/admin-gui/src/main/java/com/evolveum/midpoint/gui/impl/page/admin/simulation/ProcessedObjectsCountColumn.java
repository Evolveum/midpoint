/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.web.component.data.column.ProgressBarColumn;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class ProcessedObjectsCountColumn<R extends Serializable, S extends Serializable> extends ProgressBarColumn<R, S> {

    public ProcessedObjectsCountColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    protected abstract @NotNull IModel<ObjectCounts> createCountModel(IModel<R> rowModel);

    @Override
    protected @NotNull IModel<List<ProgressBar>> createProgressBarModel(IModel<R> rowModel) {
        return () -> {
            ObjectCounts counts = createCountModel(rowModel).getObject();

            List<ProgressBar> list = new ArrayList<>();
            addProgressBar(list, ProgressBar.State.SUCCESS, counts.getAdded(), counts, "ProcessedObjectsCountColumn.added");
            addProgressBar(list, ProgressBar.State.INFO, counts.getModified(), counts, "ProcessedObjectsCountColumn.modified");
            addProgressBar(list, ProgressBar.State.DANGER, counts.getDeleted(), counts, "ProcessedObjectsCountColumn.deleted");
            addProgressBar(list, ProgressBar.State.SECONDARY, counts.getUnmodified(), counts, "ProcessedObjectsCountColumn.unmodified");

            return list;
        };
    }

    private void addProgressBar(List<ProgressBar> list, ProgressBar.State state, int value, ObjectCounts counts, String key) {
        list.add(new ProgressBar(value * 100 / (double) counts.getTotal(), state, new SingleLocalizableMessage(key, new Object[] { value })));
    }

    @Override
    protected @NotNull IModel<String> createTextModel(IModel<R> rowModel, IModel<List<ProgressBar>> model) {
        return () -> {
            Object[] array = model.getObject().stream()
                    .filter(p -> p.getValue() > 0)
                    .map(p -> LocalizationUtil.translateMessage(p.getText()))
                    .toArray();

            if (array.length == 0) {
                return LocalizationUtil.translate("ProcessedObjectsCountColumn.noChanges");
            }

            return StringUtils.joinWith(" / ", array);
        };
    }

    public static class ObjectCounts implements Serializable {

        private int added;

        private int modified;

        private int deleted;

        private int unmodified;

        private int total;

        public int getAdded() {
            return added;
        }

        public void setAdded(int added) {
            this.added = added;
        }

        public int getModified() {
            return modified;
        }

        public void setModified(int modified) {
            this.modified = modified;
        }

        public int getDeleted() {
            return deleted;
        }

        public void setDeleted(int deleted) {
            this.deleted = deleted;
        }

        public int getUnmodified() {
            return unmodified;
        }

        public void setUnmodified(int unmodified) {
            this.unmodified = unmodified;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }
    }
}
