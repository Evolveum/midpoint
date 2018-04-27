package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author acope on 9/14/17.
 */

public abstract class PageCases extends PageAdminCases {

    private static final Trace LOGGER = TraceManager.getTrace(PageCases.class);

    private static final String DOT_CLASS = PageCases.class.getName() + ".";

    private static final long serialVersionUID = 1L;

    public static final String ID_MAIN_FORM = "mainForm";
    public static final String ID_CASES_TABLE = "table";

    private boolean all;

    public PageCases(boolean all) {
        this.all = all;

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        LOGGER.trace("Creating casePanel");
        MainObjectListPanel<CaseType> casePanel = new MainObjectListPanel<CaseType>(
                ID_CASES_TABLE,
                CaseType.class,
                UserProfileStorage.TableId.TABLE_CASES,
                null,
                this) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, CaseType caseInstance) {
                PageCases.this.caseDetailsPerformed(target, caseInstance);
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target) {
                navigateToNext(PageCase.class);
            }

            @Override
            protected List<IColumn<SelectableBean<CaseType>, String>> createColumns() {
                return PageCases.this.initColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return new ArrayList<>();
            }

            @Override
            protected IColumn<SelectableBean<CaseType>, String> createActionsColumn() {
                return null;
            }
        };
        casePanel.setOutputMarkupId(true);
        mainForm.add(casePanel);

    }

    private void caseDetailsPerformed(AjaxRequestTarget target, CaseType caseInstance) {
        LOGGER.trace("caseDetailsPerformed()");

        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OnePageParameterEncoder.PARAMETER, caseInstance.getOid());
        navigateToNext(PageCase.class, pageParameters);
    }

    private List<IColumn<SelectableBean<CaseType>, String>> initColumns() {
        LOGGER.trace("initColumns()");

        List<IColumn<SelectableBean<CaseType>, String>> columns = new ArrayList<IColumn<SelectableBean<CaseType>, String>>();

        IColumn column = new PropertyColumn(createStringResource("pageCases.table.description"), "value.description");
        columns.add(column);

        column = new AbstractColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.objectRef"), "objectRef"){
            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<CaseType>>> item, String componentId, IModel<SelectableBean<CaseType>> rowModel) {
                item.add(new Label(componentId, new AbstractReadOnlyModel<String>() {
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
                item.add(new Label(componentId, new AbstractReadOnlyModel<String>() {
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
                                        actorsList.add(actor.getTargetName() != null ? actor.getTargetName().getOrig() : actor.getOid());
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
                cellItem.add(new Label(componentId, new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        return WebComponentUtil.getLocalizedDate(created, DateLabelComponent.LONG_MEDIUM_STYLE);
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
                cellItem.add(new Label(componentId, new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        return WebComponentUtil.getLocalizedDate(closed, DateLabelComponent.LONG_MEDIUM_STYLE);
                    }
                }));
            }
        };
        columns.add(column);

        column = new PropertyColumn<SelectableBean<CaseType>, String>(createStringResource("pageCases.table.state"), CaseType.F_STATE.getLocalPart(), "value.state");
        columns.add(column);

        return columns;
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
