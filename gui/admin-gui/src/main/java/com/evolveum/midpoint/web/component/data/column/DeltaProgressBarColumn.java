package com.evolveum.midpoint.web.component.data.column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public abstract class DeltaProgressBarColumn<R extends Serializable, S extends Serializable> extends ProgressBarColumn<R, S> {

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
                        return Collections.singletonList(new ProgressBar(100, ProgressBar.State.SUCCESS));
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
                addProgressBar(bars, ProgressBar.State.SUCCESS, add, total, "ProcessedObjectsPanel.added");
                addProgressBar(bars, ProgressBar.State.INFO, modify, total, "ProcessedObjectsPanel.modified");
                addProgressBar(bars, ProgressBar.State.DANGER, delete, total, "ProcessedObjectsPanel.deleted");

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

                int count = 48; // todo implement
//                bars.stream()
//                        .map(bar -> bar.g)
                return LocalizationUtil.translate("ProcessedObjectsPanel.progressMessage", new Object[] { msg, count });
            }
        };
    }
}
