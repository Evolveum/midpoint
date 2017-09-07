package com.evolveum.midpoint.gui.api.component.captcha;

import java.util.Random;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.captcha.CaptchaImageResource;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;

public class CaptchaPanel extends BasePanel<Void> {

	private static final long serialVersionUID = 1L;

	/**
	 * The text provided by the user
	 */
	private String captchaText;

	private final CaptchaImageResource captchaImageResource;

	/**
	 * Constructor.
	 *
	 * @param id
	 *            The component id
	 */
	public CaptchaPanel(String id) {
		super(id);

		FeedbackAlerts feedback = new FeedbackAlerts("feedback");
		feedback.setFilter(new ContainerFeedbackMessageFilter(CaptchaPanel.this));
		add(feedback);
		
		captchaImageResource = createCaptchImageResource();
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
		add(changeCaptchaLink);

		add(new RequiredTextField<String>("text",
				new PropertyModel<String>(CaptchaPanel.this, "captchaText"), String.class) {
								private static final long serialVersionUID = 1L;

			@Override
			protected final void onComponentTag(final ComponentTag tag) {
				super.onComponentTag(tag);
				// clear the field after each render
				tag.put("value", "");
			}
		});
	}

	protected CaptchaImageResource createCaptchImageResource() {
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

	public void invalidateCaptcha() {
		captchaImageResource.invalidate();
	}

	static int randomInt(int min, int max)
    {
		return (int)(Math.random() * (max - min) + min);
    }

    static String randomString()
    {
    	return new Integer(randomInt(1000, 9999)).toString();
//    	for (int i = 0; i< length; i++){
//
//    	}
//
//        int num = randomInt(min, max);
//        byte b[] = new byte[num];
//        for (int i = 0; i < num; i++)
//            b[i] = (byte)randomInt('a', 'z');
//        return new String(b);
    }

    public String getCaptchaText() {
		return captchaText;
	}

    public String getRandomText() {
		return captchaImageResource.getChallengeId();
	}

}
