package com.evolveum.midpoint.web.component.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;

/**
 * Created by Kate on 07.04.2016.
 */
public class HelpInfoPanel extends Panel {
    private static final String ID_HELP = "helpLabel";
    private static final String ID_BUTTON_OK = "okButton";
    private static final String ID_CONTENT = "content";

    public HelpInfoPanel(String id){
        this(id, null);
    }

    public HelpInfoPanel(String id, String messageKey){
        super (id);
        initLayout(messageKey);
    }

    public void initLayout(final String messageKey){
        WebMarkupContainer content = new WebMarkupContainer(ID_CONTENT);
        add(content);

        Label helpLabel = new Label(ID_HELP, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getString(messageKey);
            }
        });
        helpLabel.setEscapeModelStrings(false);
        content.add(helpLabel);

        AjaxLink ok = new AjaxLink(ID_BUTTON_OK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                closePerformed(target);
            }
        };
        content.add(ok);
    }

    protected void closePerformed(AjaxRequestTarget target){
    }

}
