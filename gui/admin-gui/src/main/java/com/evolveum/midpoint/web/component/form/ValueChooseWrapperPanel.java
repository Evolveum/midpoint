package com.evolveum.midpoint.web.component.form;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import java.util.Collection;
import java.util.List;

/**
 * Created by honchar.
 */
public class ValueChooseWrapperPanel<T, O extends ObjectType> extends ValueChoosePanel{
    private static final String ID_REMOVE = "remove";
    private static final String ID_BUTTON_GROUP = "buttonGroup";

    public ValueChooseWrapperPanel(String id, IModel<T> value, Collection<Class<? extends O>> types) {
        this(id, value, null, false, types);
    }

    public ValueChooseWrapperPanel(String id, IModel<T> value, List<PrismReferenceValue> values, boolean required,
                            Collection<Class<? extends O>> types) {
        super(id, value, values, required, types);
    }

    @Override
    protected void initButtons() {
        WebMarkupContainer buttonGroup = new WebMarkupContainer(ID_BUTTON_GROUP);
        buttonGroup.setOutputMarkupId(true);
        add(buttonGroup);

        AjaxLink remove = new AjaxLink(ID_REMOVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeValuePerformed(target);
            }
        };
        remove.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isRemoveButtonVisible();
            }
        });
        buttonGroup.add(remove);
    }

    protected void removeValuePerformed(AjaxRequestTarget target){
        getModel().setObject(null);
        target.add(getTextWrapperComponent());

    }

    protected boolean isRemoveButtonVisible(){
        return true;
    }


}
