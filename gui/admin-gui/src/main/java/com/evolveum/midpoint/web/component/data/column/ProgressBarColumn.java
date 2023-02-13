package com.evolveum.midpoint.web.component.data.column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;

public class ProgressBarColumn<R extends Serializable, S extends Serializable> extends AbstractColumn<R, S> {

    private static final long serialVersionUID = 1L;

    public ProgressBarColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    @Override
    public void populateItem(Item<ICellPopulator<R>> item, String id, IModel<R> rowModel) {
        item.add(new ProgressBarPanel(id, createProgressBarModel(rowModel)));
    }

    protected @NotNull IModel<List<ProgressBar>> createProgressBarModel(IModel<R> rowModel) {
        return () -> new ArrayList<>();
    }
}
