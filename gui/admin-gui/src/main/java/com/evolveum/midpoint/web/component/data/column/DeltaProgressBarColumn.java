package com.evolveum.midpoint.web.component.data.column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.SingleLocalizableMessage;

public abstract class DeltaProgressBarColumn<R extends Serializable, S extends Serializable> extends ProgressBarColumn<R, S> {

    private static final String KEY_ADDED = "ProcessedObjectsPanel.added";
    private static final String KEY_MODIFIED = "ProcessedObjectsPanel.modified";
    private static final String KEY_DELETED = "ProcessedObjectsPanel.deleted";

    public DeltaProgressBarColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    protected abstract @NotNull IModel<ObjectDelta<?>> createObjectDeltaModel(IModel<R> rowModel);

    @Override
    protected @NotNull IModel<List<ProgressBar>> createProgressBarModel(IModel<R> rowModel) {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<ProgressBar> load() {
                ObjectDelta<?> delta = createObjectDeltaModel(rowModel).getObject();

                if (delta == null) {
                    return Collections.emptyList();
                }

                switch (delta.getChangeType()) {
                    case ADD:
                        long size = getSizeForAddDelta(delta);

                        return Collections.singletonList(new ProgressBar(100, ProgressBar.State.SUCCESS,
                                new SingleLocalizableMessage(KEY_ADDED, new Object[] { size }, KEY_ADDED)));
                    case DELETE:
                        return Collections.singletonList(new ProgressBar(100, ProgressBar.State.DANGER));
                    default:
                }

                Collection<? extends ItemDelta<?, ?>> deltas = delta.getModifications();
                long total = deltas.stream().filter(i -> !i.isOperational()).count();
                int add = 0;
                int modify = 0;
                int delete = 0;

                for (ItemDelta<?, ?> id : deltas) {
                    if (id.isOperational()) {
                        continue;
                    }

                    if (id.isAdd()) {
                        add++;
                    } else if (id.isReplace()) {
                        modify++;
                    } else if (id.isDelete()) {
                        delete++;
                    }
                }

                List<ProgressBar> bars = new ArrayList<>();
                addProgressBar(bars, ProgressBar.State.SUCCESS, add, total, KEY_ADDED);
                addProgressBar(bars, ProgressBar.State.INFO, modify, total, KEY_MODIFIED);
                addProgressBar(bars, ProgressBar.State.DANGER, delete, total, KEY_DELETED);

                return bars;
            }

            private void addProgressBar(List<ProgressBar> bars, ProgressBar.State state, int size, long total, String key) {
                if (size == 0) {
                    return;
                }

                double value = size * 100 / (double) total;

                bars.add(new ProgressBar(value, state,
                        new SingleLocalizableMessage(key, new Object[] { size }, key)));
            }
        };
    }

    private long getSizeForAddDelta(ObjectDelta<?> delta) {
        PrismObject<?> obj = delta.getObjectToAdd();
        if (obj == null || obj.isEmpty()) {
            return 0;
        }

        return obj.getValue().getItems().stream().filter(i -> !i.isOperational()).count();
    }

    @Override
    protected @NotNull IModel<String> createTextModel(IModel<R> rowModel, IModel<List<ProgressBar>> model) {
        return new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                List<ProgressBar> bars = model.getObject();
                Object[] texts = bars.stream()
                        .filter(bar -> bar.getText() != null)
                        .map(bar -> LocalizationUtil.translateMessage(bar.getText()))
                        .filter(StringUtils::isNotEmpty)
                        .toArray();

                if (texts.length == 0) {
                    return LocalizationUtil.translate("DeltaProgressBarColumn.noChanges");
                }

                String msg = StringUtils.joinWith(" / ", texts);

                ObjectDelta<?> delta = createObjectDeltaModel(rowModel).getObject();
                if (delta == null) {
                    return msg;
                }

                long count;
                switch (delta.getChangeType()) {
                    case ADD:
                        count = getSizeForAddDelta(delta);
                        break;
                    case DELETE:
                    default:
                        count = delta.getModifications().stream().filter(i -> !i.isOperational()).count();
                }

                return LocalizationUtil.translate("ProcessedObjectsPanel.progressMessage", new Object[] { msg, count });
            }
        };
    }
}
