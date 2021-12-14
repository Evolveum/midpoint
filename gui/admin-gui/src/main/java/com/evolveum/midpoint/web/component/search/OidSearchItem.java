package com.evolveum.midpoint.web.component.search;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class OidSearchItem extends SpecialSearchItem<OidSearchItemDefinition, String> {

    public OidSearchItem(Search search, IModel<String> valueModel) {
        super(search, valueModel);
    }

    public OidSearchPanel createSearchItemPanel(String id) {
        return new OidSearchPanel(id, getValueModel());
    }

    public Class<OidSearchPanel> getSearchItemPanelClass() {
        return OidSearchPanel.class;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        String oid = getValueModel().getObject();
        if (StringUtils.isEmpty(oid)) {
            return null;
        }
        ObjectQuery query = pageBase.getPrismContext().queryFor(ObjectType.class)
                .id(oid)
                .build();
        return query.getFilter();
    }
}
