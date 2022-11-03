package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AdvancedQueryWrapper;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.search.SearchItem;
import com.evolveum.midpoint.web.page.admin.configuration.PageRepositoryQuery;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.io.Serializable;

public class XmlSearchPanel extends BasePanel<AdvancedQueryWrapper> {

    private static final String ID_DEBUG = "debug";
    private static final String ID_ADVANCED_GROUP = "advancedGroup";
    private static final String ID_ADVANCED_AREA = "advancedArea";
//    private static final String ID_AXIOM_QUERY_FIELD = "axiomQueryField";
    private static final String ID_ADVANCED_ERROR = "advancedError";

    public XmlSearchPanel(String id, IModel<AdvancedQueryWrapper> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initAdvancedSearchLayout();
    }

    private <S extends SearchItem, T extends Serializable> void initAdvancedSearchLayout() {
        AjaxButton debug = new AjaxButton(ID_DEBUG, createStringResource("SearchPanel.debug")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                debugPerformed();
            }
        };
//        debug.add(new VisibleBehaviour(() -> isQueryPlaygroundAccessible()));
        add(debug);

//        WebMarkupContainer advancedGroup = new WebMarkupContainer(ID_ADVANCED_GROUP);
//        advancedGroup.add(createVisibleBehaviour(SearchBoxModeType.ADVANCED, SearchBoxModeType.AXIOM_QUERY));
//        advancedGroup.setOutputMarkupId(true);
//        add(advancedGroup);

        TextArea<?> advancedArea = new TextArea<>(ID_ADVANCED_AREA, new PropertyModel<>(getModel(), com.evolveum.midpoint.web.component.search.Search.F_ADVANCED_QUERY));
        advancedArea.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        advancedArea.add(AttributeAppender.append("placeholder", getPageBase().createStringResource("SearchPanel.insertFilterXml")));
//        advancedArea.add(createVisibleBehaviour(SearchBoxModeType.ADVANCED));
//        advancedArea.add(AttributeAppender.append("class", createValidityStyle()));
        add(advancedArea);



//        Label advancedError = new Label(ID_ADVANCED_ERROR,
//                new PropertyModel<String>(getModel(), com.evolveum.midpoint.web.component.search.Search.F_ADVANCED_ERROR));
//        advancedError.setOutputMarkupId(true);
//        advancedError.add(AttributeAppender.append("class",
//                () -> StringUtils.isEmpty(getModelObject().getAdvancedError()) ? "valid-feedback" : "invalid-feedback"));
//        advancedError.add(new VisibleBehaviour(() -> {
//            Search search = getModelObject();
//
//            if (!isAdvancedMode()) {
//                return false;
//            }
//
//            return StringUtils.isNotEmpty(search.getAdvancedError());
//        }));
//        add(advancedError);
    }

//    private boolean isAdvancedMode() {
//        return SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchMode())
//                || SearchBoxModeType.AXIOM_QUERY.equals(getModelObject().getSearchMode());
//    }
//    private boolean isQueryPlaygroundAccessible() {
//        return SecurityUtils.isPageAuthorized(PageRepositoryQuery.class) && SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchMode())
//                && ObjectType.class.isAssignableFrom(getModelObject().getTypeClass());
//    }

    private void debugPerformed() {
        Search search = null;//getModelObject();
        PageRepositoryQuery pageQuery;
        if (search != null) {
            ObjectTypes type = search.getTypeClass() != null ? ObjectTypes.getObjectType(search.getTypeClass().getSimpleName()) : null;
            QName typeName = type != null ? type.getTypeQName() : null;
            String inner = search.getAdvancedQuery();
            if (StringUtils.isNotBlank(inner)) {
                inner = "\n" + inner + "\n";
            } else if (inner == null) {
                inner = "";
            }
            pageQuery = new PageRepositoryQuery(typeName, "<query>" + inner + "</query>");
        } else {
            pageQuery = new PageRepositoryQuery();
        }
//        SearchPanel.this.setResponsePage(pageQuery);
    }

//    private IModel<String> createValidityStyle() {
//        return () -> StringUtils.isEmpty(getModelObject().getAdvancedError()) ? "is-valid" : "is-invalid";
//    }



}
