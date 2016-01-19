package com.evolveum.midpoint.web.page.admin.configuration.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;

public class EmptyOnBlurAjaxFormUpdatingBehaviour  extends AjaxFormComponentUpdatingBehavior {

    public EmptyOnBlurAjaxFormUpdatingBehaviour() {
        super("Blur");
    }

    @Override
    protected void onUpdate(AjaxRequestTarget target) {
    }
}
