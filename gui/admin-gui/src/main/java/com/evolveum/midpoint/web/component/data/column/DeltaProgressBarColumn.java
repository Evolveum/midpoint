package com.evolveum.midpoint.web.component.data.column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;

public abstract class DeltaProgressBarColumn<R extends Serializable, S extends Serializable> extends ProgressBarColumn<R, S> {

    private static final String KEY_ADDED = "ProcessedObjectsPanel.added";
    private static final String KEY_MODIFIED = "ProcessedObjectsPanel.modified";
    private static final String KEY_DELETED = "ProcessedObjectsPanel.deleted";

    public DeltaProgressBarColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    protected abstract @NotNull IModel<ObjectDeltaType> createObjectDeltaModel(IModel<R> rowModel);

    @Override
    protected @NotNull IModel<List<ProgressBar>> createProgressBarModel(IModel<R> rowModel) {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<ProgressBar> load() {
                ObjectDeltaType delta = createObjectDeltaModel(rowModel).getObject();
                if (delta == null) {
                    return Collections.emptyList();
                }

                switch (delta.getChangeType()) {
                    case ADD:
                        int size = getSizeForAddDelta(delta);

                        return Collections.singletonList(new ProgressBar(100, ProgressBar.State.SUCCESS,
                                new SingleLocalizableMessage(KEY_ADDED, new Object[] { size }, KEY_ADDED)));
                    case DELETE:
                        return Collections.singletonList(new ProgressBar(100, ProgressBar.State.DANGER));
                    default:
                }

                List<ItemDeltaType> deltas = delta.getItemDelta();
                int total = deltas.size();
                int add = 0;
                int modify = 0;
                int delete = 0;

                for (ItemDeltaType id : deltas) {
                    switch (id.getModificationType()) {
                        case ADD:
                            add++;
                            break;
                        case REPLACE:
                            modify++;
                            break;
                        case DELETE:
                            delete++;
                            break;
                    }
                }

                List<ProgressBar> bars = new ArrayList<>();
                addProgressBar(bars, ProgressBar.State.SUCCESS, add, total, KEY_ADDED);
                addProgressBar(bars, ProgressBar.State.INFO, modify, total, KEY_MODIFIED);
                addProgressBar(bars, ProgressBar.State.DANGER, delete, total, KEY_DELETED);

                return bars;
            }

            private void addProgressBar(List<ProgressBar> bars, ProgressBar.State state, int size, int total, String key) {
                if (size == 0) {
                    return;
                }

                double value = size * 100 / (double) total;

                bars.add(new ProgressBar(value, state,
                        new SingleLocalizableMessage(key, new Object[] { size }, key)));
            }
        };
    }

    private int getSizeForAddDelta(ObjectDeltaType delta) {
        ObjectType obj = delta.getObjectToAdd();
        if (obj == null || obj.asPrismObject().isEmpty()) {
            return 0;
        }

        return delta.getObjectToAdd().asPrismObject().getValue().getItems().size();
    }

    @Override
    protected @NotNull IModel<String> createTextModel(IModel<R> rowModel, IModel<List<ProgressBar>> model) {
        return new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                List<ProgressBar> bars = model.getObject();
                Object[] texts = bars.stream()
                        .filter(bar -> bar.getText() != null)
                        .map(bar -> WebComponentUtil.translateMessage(bar.getText()))
                        .filter(StringUtils::isNotEmpty)
                        .toArray();

                String msg = StringUtils.joinWith(" / ", texts);
                if (StringUtils.isEmpty(msg)) {
                    return null;
                }

                ObjectDeltaType delta = createObjectDeltaModel(rowModel).getObject();
                if (delta == null) {
                    return msg;
                }

                int count;
                switch (delta.getChangeType()) {
                    case ADD:
                        count = getSizeForAddDelta(delta);
                        break;
                    case DELETE:
                    default:
                        count = delta.getItemDelta().size();
                }

                return LocalizationUtil.translate("ProcessedObjectsPanel.progressMessage", new Object[] { msg, count });
            }
        };
    }
}
