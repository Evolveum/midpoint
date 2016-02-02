package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Honchar
 */
public class MultipleAssignmentSelector<F extends FocusType> extends BasePanel<List<SelectableBean<F>>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(MultipleAssignmentSelector.class);

    private static final String ID_TABLE = "table";
    private static final String ID_BUTTON_RESET = "buttonReset";
    private static final int ITEMS_PER_PAGE = 10;
    //    List<PrismObject<F>> choicesList;
    private PrismObject<F> prismObject;
    private ISortableDataProvider<F, String> provider;

    public MultipleAssignmentSelector(String id, IModel<List<SelectableBean<F>>> selectorModel, ISortableDataProvider provider) {
        super(id, selectorModel);
        this.provider = provider;

        initLayout();
    }

    public List<AssignmentType> getAssignmentTypeList() {
        return null;
    }

    public String getExcludeOid() {
        return null;
    }

    protected IModel<List<SelectableBean<F>>> getSelectorModel() {
        return getModel();
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
        buttonReset.setBody(createStringResource("SimpleRoleSelector.reset"));
        add(buttonReset);


//        ISortableDataProvider provider = new ListDataProvider(this, getModel());


//        if (provider != null) {
//            provider.setQuery(createQuery());
//        }

        List<IColumn<SelectableBean<F>, String>> columns = initColumns();

        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns,
                UserProfileStorage.TableId.TABLE_ROLES, ITEMS_PER_PAGE);
        updateBoxedTablePanelStyles(table);
        //hide footer menu
        table.getFooterMenu().setVisible(false);
        //hide footer count label
        table.getFooterCountLabel().setVisible(false);
        table.setOutputMarkupId(true);

        add(table);
    }

    private Component createRowLink(String id, final IModel<SelectableBean<F>> rowModel) {
        AjaxLink<SelectableBean<F>> button = new AjaxLink<SelectableBean<F>>(id, rowModel) {

            @Override
            public IModel<?> getBody() {
                return new Model<String>(getModel().getObject().getValue().asPrismObject().asObjectable().getName().getOrig());
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                LOGGER.trace("{} CLICK: {}", this, getModel().getObject());
//                toggleFocus(getModel().getObject());
                rowModel.getObject().setSelected(!rowModel.getObject().isSelected());
                getSelectorModel().getObject().add(rowModel.getObject());
                target.add(this);
            }

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
//                PrismObject<F> focus = getModel().getObject().getValue().asPrismObject();
                if (rowModel.getObject().isSelected()) {
                    tag.put("class", "list-group-item active");
                } else {
                    tag.put("class", "list-group-item");
                }
                String description = rowModel.getObject().getValue().getDescription();
                if (description != null) {
                    tag.put("title", description);
                }
            }
        };
        button.setOutputMarkupId(true);
        return button;
    }


    private boolean isSelected(PrismObject<F> prismObject) {

//        this.prismObject = prismObject;
//        for (AssignmentEditorDto dto: getSelectorModel().getObject()) {
//            if (willProcessAssignment(dto)) {
//                if (dto.getTargetRef() != null && prismObject.getOid().equals(dto.getTargetRef().getOid())) {
//                    if (dto.getStatus() != UserDtoStatus.DELETE) {
//                        return true;
//                    }
//                }
//            }
//        }
        return false;
    }

    private void toggleFocus(PrismObject<F> focus) {
//        Iterator<AssignmentEditorDto> iterator = getSelectorModel().getObject().iterator();
//        while (iterator.hasNext()) {
//            AssignmentEditorDto dto = iterator.next();
//            if (willProcessAssignment(dto)) {
//                if (dto.getTargetRef() != null && focus.getOid().equals(dto.getTargetRef().getOid())) {
//                    if (dto.getStatus() == UserDtoStatus.ADD) {
//                        iterator.remove();
//                    } else {
//                        dto.setStatus(UserDtoStatus.DELETE);
//                    }
//                    return;
//                }
//            }
//        }
//
//        AssignmentEditorDto dto = createAddAssignmentDto(focus, getPageBase());
//        getSelectorModel().getObject().add(dto);
    }

    protected AssignmentEditorDto createAddAssignmentDto(PrismObject<F> prismObject, PageBase pageBase) {
        AssignmentEditorDto dto = AssignmentEditorDto.createDtoAddFromSelectedObject(prismObject.asObjectable(), getPageBase());
        dto.setMinimized(true);
        return dto;
    }

    private void reset() {
//        Iterator<AssignmentEditorDto> iterator = getSelectorModel().getObject().iterator();
//        while (iterator.hasNext()) {
//            AssignmentEditorDto dto = iterator.next();
//            if (isManagedRole(dto) && willProcessAssignment(dto)) {
//                if (dto.getStatus() == UserDtoStatus.ADD) {
//                    iterator.remove();
//                } else if (dto.getStatus() == UserDtoStatus.DELETE) {
//                    dto.setStatus(UserDtoStatus.MODIFY);
//                }
//            }
//        }
    }

    protected boolean willProcessAssignment(AssignmentEditorDto dto) {
        return true;
    }

    protected boolean isManagedRole(AssignmentEditorDto dto) {
        if (dto.getTargetRef() == null || dto.getTargetRef().getOid() == null) {
            return false;
        }
        Iterator iterator = provider.iterator(0, provider.size());
        while (iterator.hasNext()) {
            PrismObject<F> choice = ((F)iterator.next()).asPrismObject();
            if (choice.getOid().equals(dto.getTargetRef().getOid())) {
                return true;
            }
        }
        return false;
    }

    public  void setResetButtonVisibility(boolean isVisible){
        get(ID_BUTTON_RESET).setVisible(isVisible);
    }

    private ObjectQuery createQuery() {
        return new ObjectQuery();
    }

    private List<IColumn<SelectableBean<F>, String>> initColumns() {
        List<IColumn<SelectableBean<F>, String>> columns = new ArrayList<>();
        columns.add(new AbstractColumn<SelectableBean<F>, String>(new Model()) {
            public void populateItem(Item<ICellPopulator<SelectableBean<F>>> cellItem, String componentId,
                                     IModel<SelectableBean<F>> rowModel) {
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

    public void deselectAll(){
//        List<AssignmentEditorDto> list = getModel().getObject();
//        for (AssignmentEditorDto dto : list){
//            dto.setStatus(UserDtoStatus.DELETE);
//        }
    }

    public ISortableDataProvider<F, String> getProvider() {
        return provider;
    }
}
