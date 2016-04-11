package com.evolveum.midpoint.web.page.admin.configuration.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;

public class EmptyOnBlurAjaxFormUpdatingBehaviour  extends AjaxFormComponentUpdatingBehavior {

    public EmptyOnBlurAjaxFormUpdatingBehaviour() {
        super("blur");
    }

    @Override
    protected void onUpdate(AjaxRequestTarget target) {
    }
}
