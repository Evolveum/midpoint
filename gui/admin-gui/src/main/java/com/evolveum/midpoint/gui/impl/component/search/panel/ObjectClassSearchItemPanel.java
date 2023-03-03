package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ObjectClassSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ObjectClassSearchItemPanel extends PropertySearchItemPanel {

    public ObjectClassSearchItemPanel(String id, IModel<ObjectClassSearchItemWrapper> model) {
        super(id, model);
    }

    protected boolean isFieldEnabled (){
        return !isResourceRefEmpty();
    }

    private boolean isResourceRefEmpty() {
        return getResourceRef() == null || StringUtils.isEmpty(getResourceRef().getOid());
    }

    private ObjectReferenceType getResourceRef() {
        SearchPanel<? extends Containerable> searchPanel = findParent(SearchPanel.class);
        PropertySearchItemWrapper<ObjectReferenceType> resourceRef = searchPanel.getModelObject().findPropertyItemByPath(ShadowType.F_RESOURCE_REF);
        return resourceRef == null ? null : resourceRef.getValue().getValue();
    }

    @Override
    protected Component initSearchItemField(String id) {
        return WebComponentUtil.createDropDownChoices(
                id, new PropertyModel(getModel(), ObjectClassSearchItemWrapper.F_DISPLAYABLE_VALUE),
                Model.ofList(getAvailableObjectClassList()), true);
    }

    private List<DisplayableValue<QName>> getAvailableObjectClassList() {
        List<DisplayableValue<QName>> list = new ArrayList<>();
        ObjectReferenceType resourceRef = getResourceRef();
        Task task = getPageBase().createSimpleTask("load resource");
        if (resourceRef != null && StringUtils.isNotBlank(resourceRef.getOid())) {
            PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(resourceRef, getPageBase(), task, task.getResult());
            if (resource != null) {
                Collection<QName> objectClasses = WebComponentUtil.loadResourceObjectClassValues(resource.asObjectable(), getPageBase());
                for (QName objectClass : objectClasses) {
                    list.add(new SearchValue(objectClass, getPageBase().createStringResource(objectClass.getLocalPart()).getString()));
                }
            }
        }
        return list;
    }


}
