package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextFormGroup;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

/**
 * Created honchar
 */
public class RichHyperlinkConfigPanel extends Panel implements Popupable {
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_LABEL = "label";
    private static final String ID_TARGET_URL = "targetUrl";
    private static final String ID_COLOR = "color";
    private static final String ID_AUTHORIZATION = "authorization";
    private static final String ID_ICON = "icon";
    private static final String ID_BUTTON_SAVE = "saveButton";
    private static final String ID_BUTTON_CANCEL = "cancelButton";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";
    private static final String ID_MULTIPLE_INPUT_SIZE = "col-md-6";

//    private boolean initialized;
    private boolean isMenuItem;
    private IModel<RichHyperlinkType> model;

    public RichHyperlinkConfigPanel(String id, final RichHyperlinkType link, boolean isMenuItem){
        super(id);

        this.isMenuItem = isMenuItem;
        model = new LoadableModel<RichHyperlinkType>(false) {

            @Override
            protected RichHyperlinkType load() {
                return loadModel(link);
            }
        };

        setOutputMarkupId(true);
        initLayout();
    }

    public IModel<RichHyperlinkType> getModel(){
        return model;
    }

    private RichHyperlinkType loadModel(RichHyperlinkType link){
        return link == null ? new RichHyperlinkType() : link;
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return PageBase.createStringResourceStatic(this, resourceKey, objects);
    }

    public void updateModel(AjaxRequestTarget target, RichHyperlinkType link){
        if(link == null){
            warn("RichHyperlinkConfigDialog.message.badUpdate");
            target.add(getPageBase().getFeedbackPanel());
        }

        model.setObject(link);
        target.add(getMainForm());
    }

    private PageBase getPageBase() {
        return (PageBase) getPage();
    }

    private void initLayout(){
        Form form = new com.evolveum.midpoint.web.component.form.Form(ID_MAIN_FORM);
        form.setOutputMarkupId(true);
        add(form);

        TextFormGroup name = new TextFormGroup(ID_LABEL,
            new PropertyModel<>(model, RichHyperlinkType.F_LABEL.getLocalPart()),
                createStringResource("RichHyperlinkConfigDialog.label"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        form.add(name);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION,
            new PropertyModel<>(model, RichHyperlinkType.F_DESCRIPTION.getLocalPart()),
                createStringResource("RichHyperlinkConfigDialog.description"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        description.setVisible(!isMenuItem);
        form.add(description);

        TextFormGroup targetUrl = new TextFormGroup(ID_TARGET_URL,
            new PropertyModel<>(model, RichHyperlinkType.F_TARGET_URL.getLocalPart()),
                createStringResource("RichHyperlinkConfigDialog.targetUrl"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(targetUrl);

        TextFormGroup color = new TextFormGroup(ID_COLOR,
            new PropertyModel<>(model, RichHyperlinkType.F_COLOR.getLocalPart()),
                createStringResource("RichHyperlinkConfigDialog.color"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        color.setVisible(!isMenuItem);
        form.add(color);

        MultiValueTextFormGroup authorizations = new MultiValueTextFormGroup(ID_AUTHORIZATION,
                new PropertyModel<List<String>>(model, RichHyperlinkType.F_AUTHORIZATION.getLocalPart()),
                createStringResource("RichHyperlinkConfigDialog.authorization"), ID_LABEL_SIZE, ID_MULTIPLE_INPUT_SIZE, false);
        authorizations.setVisible(!isMenuItem);
        form.add(authorizations);

        TextFormGroup icon = new TextFormGroup(ID_ICON,
            new PropertyModel<>(model, RichHyperlinkType.F_ICON.getLocalPart() + "." + IconType.F_CSS_CLASS.getLocalPart()),
                createStringResource("RichHyperlinkConfigDialog.icon"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(icon);

        initButtons(form);
    }

    private void initButtons(Form mainForm){
        AjaxSubmitButton cancel = new AjaxSubmitButton(ID_BUTTON_CANCEL,
                createStringResource("PageBase.button.cancel")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                cancelPerformed(target);
            }
        };
        mainForm.add(cancel);

        AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE,
                createStringResource("PageBase.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }
        };
        mainForm.add(save);
    }

    protected void cancelPerformed(AjaxRequestTarget target){
        getPageBase().hideMainPopup(target);
    }

    /**
     *  Override to provide call-back to situation when save button is clicked
     * */
    protected void savePerformed(AjaxRequestTarget target){}

    private Component getMainForm(){
        return get(ID_MAIN_FORM);
    }

	@Override
	public int getWidth() {
		return 600;
	}

	@Override
	public int getHeight() {
		return 900;
	}

	@Override
	public StringResourceModel getTitle() {
		return null;
	}

	@Override
	public Component getComponent() {
		return this;
	}
}
