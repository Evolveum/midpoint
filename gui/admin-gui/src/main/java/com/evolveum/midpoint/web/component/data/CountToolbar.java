package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.repeater.data.DataViewBase;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author lazyman
 */
public class CountToolbar extends AbstractToolbar {

    private static final String ID_TD = "td";
    private static final String ID_COUNT = "count";
    private static final String ID_PAGE_SIZE = "pageSize";

    public CountToolbar(DataTable<?, ?> table) {
        super(table);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer td = new WebMarkupContainer(ID_TD);
        td.add(AttributeModifier.replace("colspan", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return String.valueOf(getTable().getColumns().size());
            }
        }));
        add(td);

        Label count = new Label(ID_COUNT, createModel());
        count.setRenderBodyOnly(true);
        td.add(count);

        PageSizePopover popover = new PageSizePopover(ID_PAGE_SIZE) {

            @Override
            protected void pageSizeChanged(AjaxRequestTarget target) {
                CountToolbar.this.pageSizeChanged(target);
            }
        };
        popover.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return CountToolbar.this.isPageSizePopupVisible();
            }
        });
        td.add(popover);
    }

    private IModel<String> createModel() {
        return new LoadableModel<String>() {

            @Override
            protected String load() {
                long from = 0;
                long to = 0;
                long count = 0;

                IPageable pageable = getTable();
                if (pageable instanceof DataViewBase) {
                    DataViewBase view = (DataViewBase) pageable;

                    from = view.getFirstItemOffset() + 1;
                    to = from + view.getItemsPerPage() - 1;
                    long itemCount = view.getItemCount();
                    if (to > itemCount) {
                        to = itemCount;
                    }
                    count = itemCount;
                } else if (pageable instanceof DataTable) {
                    DataTable table = (DataTable) pageable;

                    from = table.getCurrentPage() * table.getItemsPerPage() + 1;
                    to = from + table.getItemsPerPage() - 1;
                    long itemCount = table.getItemCount();
                    if (to > itemCount) {
                        to = itemCount;
                    }
                    count = itemCount;
                }

                if (count > 0) {
                    return new StringResourceModel("CountToolbar.label", CountToolbar.this, null,
                            new Object[]{from, to, count}).getString();
                }

                return new StringResourceModel("CountToolbar.noFound", CountToolbar.this, null).getString();
            }
        };
    }

    protected void pageSizeChanged(AjaxRequestTarget target) {
    }

    protected boolean isPageSizePopupVisible() {
        return true;
    }
}
