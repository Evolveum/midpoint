/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.box;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.component.IRequestablePage;

/**
 * @author katkav
 * @author semancik
 */
public abstract class InfoBoxPanel extends Panel {
    private static final long serialVersionUID = 1L;

    private static final String ID_INFO_BOX = "infoBox";
    private static final String ID_INFO_BOX_ICON = "infoBoxIcon";
    private static final String ID_IMAGE_ID = "imageId";
    private static final String ID_MESSAGE = "message";
    private static final String ID_NUMBER = "number";

    private Class<? extends IRequestablePage> linkPage;

    public InfoBoxPanel(String id, IModel<InfoBoxType> model) {
        this(id, model, null);
    }

    public InfoBoxPanel(String id, IModel<InfoBoxType> model, Class<? extends IRequestablePage> linkPage) {
        super(id, model);
        add(AttributeModifier.append("class", "dashboard-info-box"));
        this.linkPage = linkPage;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout((IModel<InfoBoxType>)getDefaultModel(), linkPage);

    }

    private void initLayout(final IModel<InfoBoxType> model, final Class<? extends IRequestablePage> linkPage) {

        WebMarkupContainer infoBox = new WebMarkupContainer(ID_INFO_BOX);
        add(infoBox);
        infoBox.add(AttributeModifier.append("class", new PropertyModel<String>(model, InfoBoxType.BOX_BACKGROUND_COLOR)));

        WebMarkupContainer infoBoxIcon = new WebMarkupContainer(ID_INFO_BOX_ICON);
        infoBox.add(infoBoxIcon);
        infoBoxIcon.add(AttributeModifier.append("class", new PropertyModel<String>(model, InfoBoxType.ICON_BACKGROUND_COLOR)));


        WebMarkupContainer image = new WebMarkupContainer(ID_IMAGE_ID);
        image.add(AttributeModifier.append("class", new PropertyModel<String>(model, InfoBoxType.IMAGE_ID)));
        infoBoxIcon.add(image);

        Label message = new Label(ID_MESSAGE, new PropertyModel<String>(model, InfoBoxType.MESSAGE));
        infoBox.add(message);

        Label number = new Label(ID_NUMBER, new PropertyModel<String>(model, InfoBoxType.NUMBER));
        infoBox.add(number);

        customInitLayout(infoBox, model, linkPage);
    }

    protected abstract void customInitLayout(WebMarkupContainer parentInfoBox, final IModel<InfoBoxType> model, final Class<? extends IRequestablePage> linkPage);

}
