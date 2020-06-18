/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.captcha;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;

public class CaptchaPanel extends BasePanel<Void> {

    private static final long serialVersionUID = 1L;

    /**
     * The text provided by the user.
     */
    @SuppressWarnings("unused")
    private String captchaText;

    private final CaptchaImageResource captchaImageResource;

    /**
     * Constructor.
     *
     * @param id The component id
     */
    public CaptchaPanel(String id, PageBase pageBase) {
        super(id);

        FeedbackAlerts feedback = new FeedbackAlerts("feedback");
        feedback.setFilter(new ContainerFeedbackMessageFilter(CaptchaPanel.this));
        add(feedback);

        captchaImageResource = createCaptchaImageResource();
        final Image captchaImage = new Image("image", captchaImageResource);
        captchaImage.setOutputMarkupId(true);
        add(captchaImage);

        AjaxLink<Void> changeCaptchaLink = new AjaxLink<Void>("changeLink") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                captchaImageResource.invalidate();
                target.add(captchaImage);
            }
        };
        changeCaptchaLink.add(new Label("changeLinkLabel",
                pageBase.createStringResource("CaptchaPanel.changeLinkLabel")));
        add(changeCaptchaLink);

        add(new Label("textDescriptionLabel",
                pageBase.createStringResource("CaptchaPanel.textDescriptionLabel")));

        add(new RequiredTextField<String>("text",
                new PropertyModel<>(CaptchaPanel.this, "captchaText"), String.class) {
            private static final long serialVersionUID = 1L;

            @Override
            protected final void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                // clear the field after each render
                tag.put("value", "");
            }
        });
    }

    protected CaptchaImageResource createCaptchaImageResource() {
        return new CaptchaImageResource(randomString(), 48, 30) {
            private static final long serialVersionUID = 1L;

            @Override
            protected byte[] render() {
                String randomText = randomString();
                getChallengeIdModel().setObject(randomText);
                return super.render();
            }
        };
    }

    static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    static String randomString() {
        return String.valueOf(randomInt(1000, 9999));
    }

    public String getCaptchaText() {
        return captchaText;
    }

    public String getRandomText() {
        return captchaImageResource.getChallengeId();
    }
}
