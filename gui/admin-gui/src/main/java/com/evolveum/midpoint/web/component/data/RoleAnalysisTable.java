/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applyTableScaleScript;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.RoleAnalysisTablePageable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCandidateRoleType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleAnalysisTable<T> extends BasePanel<T> implements Table {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_HEADER_FOOTER = "headerFooter";
    private static final String ID_HEADER_PAGING = "pagingFooterHeader";
    private static final String ID_HEADER = "header";
    private static final String ID_FOOTER = "footer";
    private static final String ID_TABLE = "table";
    private static final String ID_TABLE_CONTAINER = "tableContainer";

    private static final String ID_PAGING_FOOTER = "pagingFooter";
    private static final String ID_PAGING = "paging";
    private static final String ID_COUNT = "count";
    private static final String ID_PAGE_SIZE = "pageSize";
    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_BUTTON_TOOLBAR = "buttonToolbar";
    private static final String ID_FORM = "form";
    private final boolean showAsCard = true;
    private final UserProfileStorage.TableId tableId;
    private String additionalBoxCssClasses = null;
    int columnCount;
    static boolean isRoleMining = false;

    LoadableDetachableModel<DisplayValueOption> displayValueOptionModel;

    public RoleAnalysisTable(String id, ISortableDataProvider<T, ?> provider, List<IColumn<T, String>> columns,
            UserProfileStorage.TableId tableId, boolean isRoleMining, int columnCount,
            LoadableDetachableModel<DisplayValueOption> displayValueOptionModel) {
        super(id);
        this.tableId = tableId;
        RoleAnalysisTable.isRoleMining = isRoleMining;
        this.columnCount = columnCount;
        this.displayValueOptionModel = displayValueOptionModel;

        initLayout(columns, provider, columnCount);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(OnDomReadyHeaderItem
                .forScript("MidPointTheme.initResponsiveTable(); MidPointTheme.initScaleResize('#tableScaleContainer');"));
    }

    private void initLayout(List<IColumn<T, String>> columns, ISortableDataProvider<T, ?> provider, int colSize) {
        setOutputMarkupId(true);
        add(AttributeAppender.prepend("class", () -> showAsCard ? "card" : ""));
        add(AttributeAppender.append("class", this::getAdditionalBoxCssClasses));

        WebMarkupContainer tableContainer = new WebMarkupContainer(ID_TABLE_CONTAINER);
        tableContainer.setOutputMarkupId(true);

        int pageSize = getItemsPerPage(tableId);
        DataTable<T, String> table = new SelectableDataTable<>(ID_TABLE, columns, provider, pageSize) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected Item<T> newRowItem(String id, int index, IModel<T> rowModel) {
                Item<T> item = super.newRowItem(id, index, rowModel);
                return customizeNewRowItem(item);
            }
        };
        table.setOutputMarkupId(true);
        tableContainer.add(table);
        add(tableContainer);

        if (!isRoleMining) {
            TableHeadersToolbar<?> headersTop = new TableHeadersToolbar<>(table, provider) {

                @Override
                protected void refreshTable(AjaxRequestTarget target) {
                    super.refreshTable(target);
                    target.add(getFooter());
                }
            };

            headersTop.setOutputMarkupId(true);
            table.addTopToolbar(headersTop);
        } else {
            RoleAnalysisTableHeadersToolbar<?> headersTop = new RoleAnalysisTableHeadersToolbar<>(table, provider) {

                @Override
                protected void refreshTable(AjaxRequestTarget target) {
                    super.refreshTable(target);
                    target.add(getFooter());
                }
            };

            headersTop.setOutputMarkupId(true);
            table.addTopToolbar(headersTop);

        }
        add(createHeader(ID_HEADER));
        WebMarkupContainer footer = createFooter();
        footer.add(new VisibleBehaviour(() -> !hideFooterIfSinglePage() || provider.size() > pageSize));

        WebMarkupContainer footer2 = createHeaderPaging();
        footer2.add(new VisibleBehaviour(() -> !hideFooterIfSinglePage() || colSize > pageSize));
        add(footer2);
        add(footer);
    }

    public String getAdditionalBoxCssClasses() {
        return additionalBoxCssClasses;
    }

    public void setAdditionalBoxCssClasses(String boxCssClasses) {
        this.additionalBoxCssClasses = boxCssClasses;
    }

    protected Item<T> customizeNewRowItem(Item<T> item) {
        return item;
    }

    protected boolean hideFooterIfSinglePage() {
        return false;
    }

    @Override
    public DataTable<?, ?> getDataTable() {
        return (DataTable<?, ?>) get(ID_TABLE_CONTAINER).get(ID_TABLE);
    }

    public Component getHeaderFooter() {
        return get("headerFooter");
    }

    @Override
    public UserProfileStorage.TableId getTableId() {
        return tableId;
    }

    @Override
    public boolean enableSavePageSize() {
        return true;
    }

    @Override
    public void setItemsPerPage(int size) {
        getDataTable().setItemsPerPage(size);
    }

    @Override
    public int getItemsPerPage() {
        return (int) getDataTable().getItemsPerPage();
    }

    private int getItemsPerPage(UserProfileStorage.TableId tableId) {
        if (tableId == null) {
            return UserProfileStorage.DEFAULT_PAGING_SIZE;
        }
        MidPointAuthWebSession session = getSession();
        UserProfileStorage userProfile = session.getSessionStorage().getUserProfile();
        return userProfile.getPagingSize(tableId);
    }

    @Override
    public void setShowPaging(boolean show) {
        if (!show) {
            setItemsPerPage(Integer.MAX_VALUE);
        } else {
            setItemsPerPage(UserProfileStorage.DEFAULT_PAGING_SIZE);
            if (isRoleMining) {
                setItemsPerPage(100);
            }
        }
    }

    public WebMarkupContainer getHeader() {
        return (WebMarkupContainer) get(ID_HEADER);
    }

    public WebMarkupContainer getFooter() {
        return (WebMarkupContainer) get(ID_FOOTER);
    }

    protected Component createHeader(String headerId) {
        WebMarkupContainer header = new WebMarkupContainer(headerId);
        header.setVisible(false);
        header.setOutputMarkupId(true);
        return header;
    }

    protected WebMarkupContainer createFooter() {
        return new PagingFooter(RoleAnalysisTable.ID_FOOTER, ID_PAGING_FOOTER, this, this) {

            @Override
            protected String getPaginationCssClass() {
                return RoleAnalysisTable.this.getPaginationCssClass();
            }

            @Override
            protected boolean isPagingVisible() {
                return RoleAnalysisTable.this.isPagingVisible();
            }
        };
    }

    protected WebMarkupContainer createHeaderPaging() {
        return new PagingFooterColumn(RoleAnalysisTable.ID_HEADER_FOOTER, ID_HEADER_PAGING, this) {

            @Override
            protected String getPaginationCssClass() {
                return RoleAnalysisTable.this.getPaginationCssClass();
            }

            @Override
            protected boolean isPagingVisible() {
                return RoleAnalysisTable.this.isPagingVisible();
            }
        };
    }

    protected boolean isPagingVisible() {
        return true;
    }

    protected String getPaginationCssClass() {
        return "pagination-sm";
    }

    @Override
    public void setCurrentPage(ObjectPaging paging) {
        WebComponentUtil.setCurrentPage(this, paging);
    }

    @Override
    public void setCurrentPage(long page) {
        getDataTable().setCurrentPage(page);
    }

    protected WebMarkupContainer createButtonToolbar(String id) {
        return new WebMarkupContainer(id);
    }

    private static class PagingFooter extends Fragment {

        public PagingFooter(String id, String markupId, RoleAnalysisTable markupProvider, Table table) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);

            initLayout(markupProvider, table);
        }

        private void initLayout(final RoleAnalysisTable<?> boxedTablePanel, final Table table) {
            WebMarkupContainer buttonToolbar = boxedTablePanel.createButtonToolbar(ID_BUTTON_TOOLBAR);
            add(buttonToolbar);

            final DataTable<?, ?> dataTable = table.getDataTable();
            WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
            footerContainer.setOutputMarkupId(true);
            footerContainer.add(new VisibleBehaviour(this::isPagingVisible));

            final Label count = new Label(ID_COUNT, () -> CountToolbar.createCountString(dataTable));
            count.setOutputMarkupId(true);
            footerContainer.add(count);

            NavigatorPanel nb2 = new NavigatorPanel(ID_PAGING, dataTable, true) {

                @Override
                protected void onPageChanged(AjaxRequestTarget target, long page) {
                    target.add(count);
                    target.appendJavaScript(applyTableScaleScript());

                }

                @Override
                protected boolean isCountingDisabled() {
                    if (dataTable.getDataProvider() instanceof SelectableBeanContainerDataProvider) {
                        return !((SelectableBeanContainerDataProvider<?>) dataTable.getDataProvider()).isUseObjectCounting();
                    }
                    return super.isCountingDisabled();
                }

                @Override
                protected String getPaginationCssClass() {
                    return RoleAnalysisTable.PagingFooter.this.getPaginationCssClass();
                }
            };
            footerContainer.add(nb2);

            Form<?> form = new MidpointForm<>(ID_FORM);
            footerContainer.add(form);
            PagingSizePanel menu = new PagingSizePanel(ID_PAGE_SIZE) {

                @Override
                protected List<Integer> getPagingSizes() {

                    if (isRoleMining) {
                        return List.of(new Integer[] { 50, 100, 150, 200 });
                    }
                    return super.getPagingSizes();
                }

                @Override
                protected void onPageSizeChangePerformed(Integer newValue, AjaxRequestTarget target) {
                    Table table = findParent(Table.class);
                    UserProfileStorage.TableId tableId = table.getTableId();

                    if (tableId != null && table.enableSavePageSize()) {
//                        int pageSize = (int) getPageBase().getItemsPerPage(tableId);

                        table.setItemsPerPage(newValue);
                    }
                    target.appendJavaScript(applyTableScaleScript());
                    target.add(findParent(RoleAnalysisTable.PagingFooter.class));
                    target.add((Component) table);
                }
            };
            // todo nasty hack, we should decide whether paging should be normal or "small"
            menu.setSmall(getPaginationCssClass() != null);
            form.add(menu);
            add(footerContainer);
        }

        protected String getPaginationCssClass() {
            return "pagination-sm";
        }

        protected boolean isPagingVisible() {
            return true;
        }
    }

    private class PagingFooterColumn extends Fragment {

        public PagingFooterColumn(String id, String markupId, RoleAnalysisTable markupProvider) {
            super(id, markupId, markupProvider);
            setOutputMarkupId(true);

            initLayout(markupProvider);
        }

        int pagingSize = getColumnPageCount();
        long pages = 0;

        private void initLayout(final RoleAnalysisTable<?> boxedTablePanel) {


            WebMarkupContainer footerContainer = new WebMarkupContainer(ID_FOOTER_CONTAINER);
            footerContainer.setOutputMarkupId(true);
            footerContainer.add(new VisibleBehaviour(this::isPagingVisible));

            Form<?> form = new MidpointForm<>(ID_FORM);
            footerContainer.add(form);

            Form<?> formBsProcess = new MidpointForm<>("form_bs_process");
            footerContainer.add(formBsProcess);

            CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_PLUS_CIRCLE,
                    LayeredIconCssStyle.IN_ROW_STYLE);

            AjaxCompositedIconSubmitButton editButton = new AjaxCompositedIconSubmitButton("process_selections_id",
                    iconBuilder.build(), new LoadableModel<>() {
                @Override
                protected String load() {
                    @Nullable Set<RoleAnalysisCandidateRoleType> candidateRoleContainers = getCandidateRoleContainer();
                    if (candidateRoleContainers != null) {
                        List<RoleAnalysisCandidateRoleType> candidateRoleTypes = new ArrayList<>(candidateRoleContainers);
                        if (candidateRoleTypes.size() == 1) {
                            PolyStringType targetName = candidateRoleTypes.get(0).getCandidateRoleRef().getTargetName();
                            return createStringResource("RoleMining.button.title.edit.candidate",
                                    targetName).getString();
                        } else {
                            return createStringResource("RoleMining.button.title.edit.candidate").getString();
                        }
                    } else {
                        return createStringResource("RoleMining.button.title.candidate").getString();
                    }
                }
            }) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    onSubmitEditButton(target);
                }

                @Override
                protected void onError(AjaxRequestTarget target) {
                    target.add(((PageBase) getPage()).getFeedbackPanel());
                }
            };
            editButton.add(new AttributeModifier("style", "min-width: 150px;"));
            editButton.add(new VisibleBehaviour(RoleAnalysisTable.this::getMigrationButtonVisibility));
            editButton.titleAsLabel(true);
            editButton.setOutputMarkupId(true);
            editButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));

            formBsProcess.add(editButton);

            List<Integer> integers = List.of(new Integer[] { 100, 200, 400 });
            DropDownChoice<Integer> colPerPage = new DropDownChoice<>("colCountOnPage",
                    new Model<>(getColumnPageCount()), integers);
            colPerPage.add(new AjaxFormComponentUpdatingBehavior("change") {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    onChangeSize(colPerPage.getModelObject(), target);
                }
            });
            colPerPage.setOutputMarkupId(true);
            form.add(colPerPage);

            Label colPerPageLabel = new Label("label_dropdown", Model.of("Cols per page"));
            colPerPageLabel.setOutputMarkupId(true);
            footerContainer.add(colPerPageLabel);

            int from = 1;
            int to = getColumnPageCount();
            pagingSize = getColumnPageCount();
            String separator = " - ";
            List<String> navigation = new ArrayList<>();

            if (columnCount <= to) {
                navigation.add(from + separator + columnCount);
            } else {
                while (columnCount > to) {
                    navigation.add(from + separator + to);
                    from += pagingSize;
                    to += pagingSize;
                }
                navigation.add(from + separator + columnCount);
            }

            String[] rangeParts = getColumnPagingTitle().split(" - ");
            String title = (Integer.parseInt(rangeParts[0]) + 1) + " to "
                    + Integer.parseInt(rangeParts[1])
                    + " of "
                    + columnCount;

            Label count = new Label(ID_COUNT, Model.of(title));
            count.setOutputMarkupId(true);
            footerContainer.add(count);

            RoleAnalysisTablePageable<?> roleAnalysisTablePageable = new RoleAnalysisTablePageable<>(navigation.size(),
                    getCurrentPage());

            NavigatorPanel colNavigator = new NavigatorPanel(ID_PAGING, roleAnalysisTablePageable, true) {

                @Override
                protected boolean isComponent() {
                    return false;
                }

                @Override
                protected void onPageChanged(AjaxRequestTarget target, long page) {
                    pages = page;
                    String newPageRange = navigation.get((int) page);
                    target.add(this);
                    onChange(newPageRange, target, (int) page);
                }

                @Override
                protected boolean isCountingDisabled() {
                    return super.isCountingDisabled();
                }

                @Override
                protected String getPaginationCssClass() {
                    return RoleAnalysisTable.PagingFooterColumn.this.getPaginationCssClass();
                }
            };
            footerContainer.add(colNavigator);

            add(footerContainer);
        }

        protected String getPaginationCssClass() {
            return "pagination-sm";
        }

        protected boolean isPagingVisible() {
            return true;
        }
    }

    public void onChange(String value, AjaxRequestTarget target, int currentPage) {
    }

    protected void onChangeSize(int value, AjaxRequestTarget target) {
    }

    protected String getColumnPagingTitle() {
        if (columnCount < getColumnPageCount()) {
            return "0 - " + columnCount;
        }
        return "0 - " + getColumnPageCount();
    }

    protected int getCurrentPage() {
        return 0;
    }

    protected int getColumnPageCount() {
        return 100;
    }

    protected void onSubmitEditButton(AjaxRequestTarget target) {

    }

    protected @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRoleContainer() {
        return null;
    }

    protected boolean getMigrationButtonVisibility() {
        return true;
    }

}
