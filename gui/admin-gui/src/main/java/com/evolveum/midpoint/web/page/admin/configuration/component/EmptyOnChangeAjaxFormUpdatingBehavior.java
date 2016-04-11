package com.evolveum.midpoint.web.page.admin.configuration.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;

public class EmptyOnChangeAjaxFormUpdatingBehavior extends AjaxFormComponentUpdatingBehavior {

    public EmptyOnChangeAjaxFormUpdatingBehavior(){
        super("change");
    }

    @Override
    protected void onUpdate(AjaxRequestTarget target){

    }
}
