/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.data;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.data.DataViewBase;
import org.apache.wicket.model.AbstractReadOnlyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.session.UserProfileStorage;

/**
 * @author Viliam Repan (lazyman)
 */
public class BoxedTablePanel<T> extends BasePanel implements Table {

	private static final String ID_HEADER = "header";
	private static final String ID_FOOTER = "footer";
	private static final String ID_TABLE = "table";

	private static final String ID_PAGING_FOOTER = "pagingFooter";
	private static final String ID_PAGING = "paging";
	private static final String ID_COUNT = "count";
	private static final String ID_MENU = "menu";
	private static final String ID_FOOTER_CONTAINER = "footerContainer";

	private UserProfileStorage.TableId tableId;
	private boolean showPaging;

	public BoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns) {
		this(id, provider, columns, null, Integer.MAX_VALUE);
	}

	public BoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
			UserProfileStorage.TableId tableId) {
		this(id, provider, columns, tableId, UserProfileStorage.DEFAULT_PAGING_SIZE);
	}

	public BoxedTablePanel(String id, ISortableDataProvider provider, List<IColumn<T, String>> columns,
			UserProfileStorage.TableId tableId, int pageSize) {
		super(id);
		this.tableId = tableId;

		initLayout(columns, provider, pageSize);
	}

	private void initLayout(List<IColumn<T, String>> columns, ISortableDataProvider provider, int pageSize) {
		DataTable<T, String> table = new SelectableDataTable<>(ID_TABLE, columns, provider, pageSize);
		table.setOutputMarkupId(true);
		add(table);

		TableHeadersToolbar headersTop = new TableHeadersToolbar(table, provider);
		headersTop.setOutputMarkupId(true);
		table.addTopToolbar(headersTop);

		add(createHeader(ID_HEADER));
		add(createFooter(ID_FOOTER));
	}

	@Override
	public DataTable getDataTable() {
		return (DataTable) get(ID_TABLE);
	}

	@Override
	public UserProfileStorage.TableId getTableId() {
		return tableId;
	}

	@Override
	public void setItemsPerPage(int size) {
		getDataTable().setItemsPerPage(size);
	}

	@Override
	public int getItemsPerPage() {
		return (int) getDataTable().getItemsPerPage();
	}

	@Override
	public void setShowPaging(boolean show) {
		// todo make use of this [lazyman]
		this.showPaging = show;

		if (!show) {
			setItemsPerPage(Integer.MAX_VALUE);
		} else {
			setItemsPerPage(10);
		}
	}

	public WebMarkupContainer getHeader() {
		return (WebMarkupContainer) get(ID_HEADER);
	}

	public WebMarkupContainer getFooter() {
		return (WebMarkupContainer) get(ID_FOOTER);
	}

	protected WebMarkupContainer createHeader(String headerId) {
		WebMarkupContainer header = new WebMarkupContainer(headerId);
		header.setVisible(false);
		return header;
	}

	protected WebMarkupContainer createFooter(String footerId) {
		return new PagingFooter(footerId, ID_PAGING_FOOTER, this, this);
	}

	public Component getFooterMenu() {
		return ((PagingFooter)getFooter()).getFooterMenu();
	}


	public Component getFooterCountLabel() {
		return ((PagingFooter)getFooter()).getFooterCountLabel();
	}

	public Component getFooterPaging() {
		return ((PagingFooter)getFooter()).getFooterPaging();
    }

	@Override
	public void setCurrentPage(ObjectPaging paging) {
		WebComponentUtil.setCurrentPage(this, paging);
	}

	@Override
	public void setCurrentPage(long page) {
		getDataTable().setCurrentPage(page);
	}

	private static class PagingFooter extends Fragment {

		public PagingFooter(String id, String markupId, MarkupContainer markupProvider, Table table) {
			super(id, markupId, markupProvider);
			setOutputMarkupId(true);

			initLayout(table);
		}

		private void initLayout(final Table table) {
			final DataTable dataTable = table.getDataTable();
            WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
            footerContainer.setOutputMarkupId(true);

			final Label count = new Label(ID_COUNT, new AbstractReadOnlyModel<String>() {

				@Override
				public String getObject() {
					return createCountString(dataTable);
				}
			});
			count.setOutputMarkupId(true);
			add(count);

			BoxedPagingPanel nb2 = new BoxedPagingPanel(ID_PAGING, dataTable, true) {

				@Override
				protected void onPageChanged(AjaxRequestTarget target, long page) {
					target.add(count);
				}
			};
            footerContainer.add(nb2);

			TableConfigurationPanel menu = new TableConfigurationPanel(ID_MENU) {

				@Override
				protected void pageSizeChanged(AjaxRequestTarget target) {
					Table table = findParent(Table.class);
					UserProfileStorage.TableId tableId = table.getTableId();

					PageBase page = (PageBase) getPage();
					Integer pageSize = page.getSessionStorage().getUserProfile().getPagingSize(tableId);

					table.setItemsPerPage(pageSize);
					target.add(findParent(PagingFooter.class));
					target.add((Component) table);
				}
			};
            footerContainer.add(menu);
            add(footerContainer);
		}

        public Component getFooterMenu(){
            return get(ID_FOOTER_CONTAINER).get(ID_MENU);
        }

        public Component getFooterCountLabel(){
            return get(ID_COUNT);
        }

        public Component getFooterPaging(){
            return get(ID_FOOTER_CONTAINER).get(ID_PAGING);
        }

		private String createCountString(IPageable pageable) {
			long from = 0;
			long to = 0;
			long count = 0;

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
				if (count == Integer.MAX_VALUE) {
					return PageBase.createStringResourceStatic(PagingFooter.this, "CountToolbar.lable",
							new Object[] { from, to }).getString();
				}
				
				return PageBase.createStringResourceStatic(PagingFooter.this, "CountToolbar.label",
						new Object[] { from, to, count }).getString();

				// new StringResourceModel("CountToolbar.label",
				// PagingFooter.this, null,
				// new Object[]{from, to, count}).getString();
			}

			return PageBase.createStringResourceStatic(PagingFooter.this, "CountToolbar.noFound", new Object[] {})
					.getString();
		}
	}
}
