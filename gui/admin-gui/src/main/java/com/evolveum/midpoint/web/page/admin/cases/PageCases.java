package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author acope on 9/14/17.
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/casesAll", matchUrlForSecurity = "/admin/casesAll")
        }, action = {
        @AuthorizationAction(actionUri = PageAdminCases.AUTH_CASES_ALL_LABEL,
                label = PageAdminCases.AUTH_CASES_ALL_LABEL,
                description = PageAdminCases.AUTH_CASES_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_ALL_URL,
                label = "PageCases.auth.casesAll.label",
                description = "PageCases.auth.casesAll.description")
})
public class PageCases extends PageAdminObjectList<CaseType> {

    private static final Trace LOGGER = TraceManager.getTrace(PageCases.class);

    private static final String DOT_CLASS = PageCases.class.getName() + ".";
    private static final String OPERATION_LOAD_REFERENCE_DISPLAY_NAME = DOT_CLASS + "loadReferenceDisplayName";

    private static final long serialVersionUID = 1L;

    public static final String ID_MAIN_FORM = "mainForm";
    public static final String ID_CASES_TABLE = "table";

    public PageCases() {
        super();
    }

//    private void initLayout() {
//        Form mainForm = new Form(ID_MAIN_FORM);
//        add(mainForm);
//
//        LOGGER.trace("Creating casePanel");
//        MainObjectListPanel<CaseType, CompiledObjectCollectionView> casePanel =
//                new MainObjectListPanel<CaseType, CompiledObjectCollectionView>(
//                ID_CASES_TABLE,
//                CaseType.class,
//                UserProfileStorage.TableId.TABLE_CASES,
//                null,
//                this) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected void objectDetailsPerformed(AjaxRequestTarget target, CaseType caseInstance) {
//                PageCases.this.caseDetailsPerformed(target, caseInstance);
//            }
//
//            @Override
//            protected void newObjectPerformed(AjaxRequestTarget target, CompiledObjectCollectionView collectionView) {
//                navigateToNext(PageCase.class);
//            }
//
//            @Override
//            protected List<IColumn<SelectableBean<CaseType>, String>> createColumns() {
//                return PageCases.this.initColumns();
//            }
//
//            @Override
//            protected List<InlineMenuItem> createInlineMenu() {
//                return new ArrayList<>();
//            }
//
//        };
//        casePanel.setOutputMarkupId(true);
//        mainForm.add(casePanel);
//
//    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, CaseType caseInstance) {
        LOGGER.trace("caseDetailsPerformed()");

        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OnePageParameterEncoder.PARAMETER, caseInstance.getOid());
        navigateToNext(PageCase.class, pageParameters);
    }

    @Override
    protected List<IColumn<SelectableBean<CaseType>, String>> initColumns() {
        LOGGER.trace("initColumns()");

        List<IColumn<SelectableBean<CaseType>, String>> columns = new ArrayList<IColumn<SelectableBean<CaseType>, String>>();

        IColumn column = new PropertyColumn(createStringResource("pageCases.table.description"), "value.description");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.objectRef"), "objectRef"){
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> item, String componentId, IModel<SelectableBean<CaseType>> rowModel) {
                item.add(new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        return getObjectRef(rowModel);
                    }
                }));
            }
        };
        columns.add(column);

        column = new AbstractColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.actors")){
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> item, String componentId, IModel<SelectableBean<CaseType>> rowModel) {
                item.add(new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        String actors = null;
                        SelectableBean<CaseType> caseModel = rowModel.getObject();
                        if (caseModel != null) {
                            CaseType caseIntance = caseModel.getValue();
                            if (caseIntance != null) {
                                List<CaseWorkItemType> caseWorkItemTypes = caseIntance.getWorkItem();
                                List<String> actorsList = new ArrayList<String>();
                                for (CaseWorkItemType caseWorkItem : caseWorkItemTypes) {
                                    List<ObjectReferenceType> assignees = caseWorkItem.getAssigneeRef();
                                    for (ObjectReferenceType actor : assignees) {
                                        actorsList.add(WebComponentUtil.getEffectiveName(actor, AbstractRoleType.F_DISPLAY_NAME, PageCases.this,
                                                OPERATION_LOAD_REFERENCE_DISPLAY_NAME));
                                    }
                                }
                                actors = String.join(", ", actorsList);
                            }
                        }
                        return actors;
                    }
                }));
            }
        };
        columns.add(column);

        column = new AbstractColumn<SelectableBean<CaseType>, String>(
                createStringResource("pageCases.table.openTimestamp"),
                MetadataType.F_CREATE_TIMESTAMP.getLocalPart()) {

            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> cellItem,
                                     String componentId, final IModel<SelectableBean<CaseType>> rowModel) {
                CaseType object = rowModel.getObject().getValue();
                MetadataType metadata = object != null ? object.getMetadata() : null;
                XMLGregorianCalendar createdCal = metadata.getCreateTimestamp();
                final Date created;
                if (createdCal != null) {
                    created = createdCal.toGregorianCalendar().getTime();
//                    cellItem.add(AttributeModifier.replace("title", WebComponentUtil.getLocalizedDate(created, DateLabelComponent.LONG_MEDIUM_STYLE)));
//                    cellItem.add(new TooltipBehavior());
                } else {
                    created = null;
                }
                cellItem.add(new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        return WebComponentUtil.getShortDateTimeFormattedValue(created, PageCases.this);
                    }
                }));
            }
        };
        columns.add(column);

        column = new PropertyColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.closeTimestamp"), CaseType.F_CLOSE_TIMESTAMP.getLocalPart(), "value.closeTimestamp") {
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> cellItem,
                                     String componentId, final IModel<SelectableBean<CaseType>> rowModel) {
                CaseType object = rowModel.getObject().getValue();
                XMLGregorianCalendar closedCal = object.getCloseTimestamp();
                final Date closed;
                if (closedCal != null) {
                    closed = closedCal.toGregorianCalendar().getTime();
//                    cellItem.add(AttributeModifier.replace("title", WebComponentUtil.getLocalizedDate(closed, DateLabelComponent.LONG_MEDIUM_STYLE)));
//                    cellItem.add(new TooltipBehavior());
                } else {
                    closed = null;
                }
                cellItem.add(new Label(componentId, new IModel<String>() {
                    @Override
                    public String getObject() {
                        return WebComponentUtil.getShortDateTimeFormattedValue(closed, PageCases.this);
                    }
                }));
            }
        };
        columns.add(column);

        column = new PropertyColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.state"), CaseType.F_STATE.getLocalPart(), "value.state");
        columns.add(column);

        column = new AbstractExportableColumn<SelectableBean<CaseType>, String>(
                createStringResource("pageCases.table.workitems")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> cellItem,
                                     String componentId, IModel<SelectableBean<CaseType>> model) {
                cellItem.add(new Label(componentId,
                        model.getObject().getValue() != null && model.getObject().getValue().getWorkItem() != null ?
                                model.getObject().getValue().getWorkItem().size() : null));
            }

            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<CaseType>> rowModel) {
                return Model.of(rowModel.getObject().getValue() != null && rowModel.getObject().getValue().getWorkItem() != null ?
                        Integer.toString(rowModel.getObject().getValue().getWorkItem().size()) : "");
            }


        };
        columns.add(column);
        return columns;
    }

    @Override
    protected Class getType(){
        return CaseType.class;
    }

    @Override
    protected UserProfileStorage.TableId getTableId(){
        return UserProfileStorage.TableId.TABLE_CASES;
    }

    @Override
    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();
        return menu;
    }

    private String getObjectRef(IModel<SelectableBean<CaseType>> caseModel) {
        CaseType caseModelObject = caseModel.getObject().getValue();
        if (caseModelObject.getObjectRef() == null) {
            return "";
        }
        if (caseModelObject.getObjectRef().getTargetName() != null && StringUtils.isNotEmpty(caseModelObject.getObjectRef().getTargetName().getOrig())) {
            return caseModelObject.getObjectRef().getTargetName().getOrig();
        } else {
            return caseModelObject.getObjectRef().getOid();
        }
    }
}
