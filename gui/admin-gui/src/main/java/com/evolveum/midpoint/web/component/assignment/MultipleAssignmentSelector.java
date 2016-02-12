package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UsersStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Honchar
 */
public class MultipleAssignmentSelector<F extends FocusType> extends BasePanel<List<AssignmentEditorDto>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(MultipleAssignmentSelector.class);

    private static final String ID_TABLE = "table";
    private static final String ID_BUTTON_RESET = "buttonReset";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH = "search";
    private static final int ITEMS_PER_PAGE = 10;
    private static final String ID_TABLE_HEADER = "tableHeader";
    private ISortableDataProvider<F, String> provider;
    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel;

    private IModel<Search> searchModel;
    public MultipleAssignmentSelector(String id, IModel<List<AssignmentEditorDto>> selectorModel, ISortableDataProvider provider) {
        super(id, selectorModel);
        this.provider = provider;
        executeOptionsModel = new LoadableModel<ExecuteChangeOptionsDto>(false) {

            @Override
            protected ExecuteChangeOptionsDto load() {
                return new ExecuteChangeOptionsDto();
            }
        };
        searchModel = new LoadableModel<Search>(false) {

            @Override
            public Search load() {
                Search search =  SearchFactory.createSearch(RoleType.class, getPageBase().getPrismContext(), false);
                return search;
            }
        };

        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);
        AjaxLink<String> buttonReset = new AjaxLink<String>(ID_BUTTON_RESET) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                reset();
                target.add(MultipleAssignmentSelector.this);
            }
        };
        buttonReset.setBody(createStringResource("MultipleAssignmentSelector.reset"));
        add(buttonReset);

        initSearchPanel();
        List<IColumn<SelectableBean<AssignmentEditorDto>, String>> columns = initColumns();

        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns,
                UserProfileStorage.TableId.TABLE_ROLES, ITEMS_PER_PAGE){
//            @Override
//            protected WebMarkupContainer createHeader(String headerId) {
//                return new SearchFragment(headerId, ID_TABLE_HEADER, MultipleAssignmentSelector.this, searchModel, executeOptionsModel);
//            }
        };
        updateBoxedTablePanelStyles(table);
        //hide footer menu
        table.getFooterMenu().setVisible(false);
        //hide footer count label
        table.getFooterCountLabel().setVisible(false);
        table.setOutputMarkupId(true);

        add(table);
    }

    private Component createRowLink(String id, final IModel<SelectableBean<AssignmentEditorDto>> rowModel) {
        AjaxLink<SelectableBean<AssignmentEditorDto>> button = new AjaxLink<SelectableBean<AssignmentEditorDto>>(id, rowModel) {

            @Override
            public IModel<?> getBody() {
                return new Model<String>(((AssignmentEditorDto) rowModel.getObject()).getNameForTargetObject());
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                LOGGER.trace("{} CLICK: {}", this, rowModel.getObject());
                toggleRow(rowModel);
                target.add(this);
            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                if (rowModel.getObject().isSelected()) {
                    tag.put("class", "list-group-item active");
                } else {
                    tag.put("class", "list-group-item");
                }
                String description = ((AssignmentEditorDto) rowModel.getObject()).getDescription();
                if (description != null) {
                    tag.put("title", description);
                }
            }
        };
        button.setOutputMarkupId(true);
        return button;
    }

    private void reset() {
        List<AssignmentEditorDto> assignmentsList = getModel().getObject();
        List<AssignmentEditorDto> listToBeRemoved = new ArrayList<>();
        for (AssignmentEditorDto dto : assignmentsList){
            if (dto.getStatus().equals(UserDtoStatus.ADD)) {
                listToBeRemoved.add(dto);
             } else if (dto.getStatus() == UserDtoStatus.DELETE) {
                dto.setStatus(UserDtoStatus.MODIFY);
            }
        }
        assignmentsList.removeAll(listToBeRemoved);
    }


    public  void setResetButtonVisibility(boolean isVisible){
        get(ID_BUTTON_RESET).setVisible(isVisible);
    }

    private List<IColumn<SelectableBean<AssignmentEditorDto>, String>> initColumns() {
        List<IColumn<SelectableBean<AssignmentEditorDto>, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<SelectableBean<AssignmentEditorDto>, String>(new Model()) {
            public void populateItem(Item<ICellPopulator<SelectableBean<AssignmentEditorDto>>> cellItem, String componentId,
                                     IModel<SelectableBean<AssignmentEditorDto>> rowModel) {
                cellItem.add(createRowLink(componentId, rowModel));
            }
        });

        return columns;
    }

    private void updateBoxedTablePanelStyles(BoxedTablePanel panel) {
        panel.getDataTable().add(new AttributeModifier("class", ""));
        panel.getDataTable().add(new AttributeAppender("style", "width: 100%;"));
        panel.getFooterPaging().getParent().add(new AttributeModifier("class", "col-md-10"));
    }

    public ISortableDataProvider<F, String> getProvider() {
        return provider;
    }

    private void toggleRow(IModel<SelectableBean<AssignmentEditorDto>> rowModel){
        rowModel.getObject().setSelected(!rowModel.getObject().isSelected());
        List<AssignmentEditorDto> providerList = ((BaseSortableDataProvider) getProvider()).getAvailableData();
        for (AssignmentEditorDto dto : providerList){
            if (dto.getTargetRef().getOid().equals(((AssignmentEditorDto) rowModel.getObject()).getTargetRef().getOid())){
                dto.setSelected(rowModel.getObject().isSelected());
                break;
            }
        }

    }

    private void initSearchPanel(){
        final Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);
        searchForm.setOutputMarkupId(true);

        SearchPanel search = new SearchPanel(ID_SEARCH, (IModel) searchModel) {

            @Override
            public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
                MultipleAssignmentSelector.this.searchPerformed(query, target);
//                PageUsers page = (PageUsers) getPage();
//                page.searchPerformed(query, target);
            }
        };
        searchForm.add(search);

    }

    private static class SearchFragment extends Fragment {

        public SearchFragment(String id, String markupId, MarkupContainer markupProvider,
                              IModel<Search> model, IModel<ExecuteChangeOptionsDto> executeOptionsModel) {
            super(id, markupId, markupProvider, model);

            initLayout(executeOptionsModel);
        }

        private void initLayout(IModel<ExecuteChangeOptionsDto> executeOptionsModel) {
            final Form searchForm = new Form(ID_SEARCH_FORM);
            add(searchForm);
            searchForm.setOutputMarkupId(true);

            SearchPanel search = new SearchPanel(ID_SEARCH, (IModel) getDefaultModel()) {

                @Override
                public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
                    Component component = this.findParent(MultipleAssignmentSelector.class);
                    if (component != null){
                        ((MultipleAssignmentSelector)component).searchPerformed(query, target);
                    }
                }
            };
            searchForm.add(search);

        }
    }

    private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
        BoxedTablePanel panel = getTable();
        DataTable table = panel.getDataTable();
        BaseSortableDataProvider provider = (BaseSortableDataProvider) table.getDataProvider();
        provider.setQuery(query);

        panel.setCurrentPage(null);

        target.add(panel);
    }


    private BoxedTablePanel getTable() {
        return (BoxedTablePanel) get(ID_TABLE);
    }
}
