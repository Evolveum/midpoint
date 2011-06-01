/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

/**
 * Enables messages to be rendered on different pages from which they were set.
 * 
 * After each phase where messages may be added, this moves the messages from
 * the page-scoped FacesContext to the session-scoped session map.
 * 
 * Before messages are rendered, this moves the messages from the session-scoped
 * session map back to the page-scoped FacesContext.
 * 
 * Only global messages, not associated with a particular component, are moved.
 * Component messages cannot be rendered on pages other than the one on which
 * they were added.
 * 
 * To enable multi-page messages support, add a <code>lifecycle</code> block to
 * your faces-config.xml file. That block should contain a single
 * <code>phase-listener</code> block containing the fully-qualified classname of
 * this file.
 * 
 * @author Jesse Wilson jesse[AT]odel.on.ca
 * @secondaryAuthor Lincoln Baxter III lincoln[AT]ocpsoft.com
 */
public class MultiPageMessagesSupport implements PhaseListener {

	private static final long serialVersionUID = 1250469273857785274L;
	private static final String sessionToken = "MULTI_PAGE_MESSAGES_SUPPORT";

	public PhaseId getPhaseId() {
		return PhaseId.ANY_PHASE;
	}

	/*
	 * Check to see if we are "naturally" in the RENDER_RESPONSE phase. If we
	 * have arrived here and the response is already complete, then the page is
	 * not going to show up: don't display messages yet.
	 */
	public void beforePhase(final PhaseEvent event) {
		FacesContext facesContext = event.getFacesContext();
		this.saveMessages(facesContext);

		if (PhaseId.RENDER_RESPONSE.equals(event.getPhaseId())) {
			if (!facesContext.getResponseComplete()) {
				this.restoreMessages(facesContext);
			}
		}
	}

	/*
	 * Save messages into the session after every phase.
	 */
	public void afterPhase(final PhaseEvent event) {
		if (!PhaseId.RENDER_RESPONSE.equals(event.getPhaseId())) {
			FacesContext facesContext = event.getFacesContext();
			this.saveMessages(facesContext);
		}
	}

	@SuppressWarnings("unchecked")
	private int saveMessages(final FacesContext facesContext) {
		List<FacesMessage> messages = new ArrayList<FacesMessage>();
		for (Iterator<FacesMessage> iter = facesContext.getMessages(null); iter.hasNext();) {
			messages.add(iter.next());
			iter.remove();
		}

		if (messages.size() == 0) {
			return 0;
		}

		Map<String, Object> sessionMap = facesContext.getExternalContext().getSessionMap();
		List<FacesMessage> existingMessages = (List<FacesMessage>) sessionMap.get(sessionToken);
		if (existingMessages != null) {
			existingMessages.addAll(messages);
		} else {
			sessionMap.put(sessionToken, messages);
		}
		return messages.size();
	}

	@SuppressWarnings("unchecked")
	private int restoreMessages(final FacesContext facesContext) {
		Map<String, Object> sessionMap = facesContext.getExternalContext().getSessionMap();
		List<FacesMessage> messages = (List<FacesMessage>) sessionMap.remove(sessionToken);

		if (messages == null) {
			return 0;
		}

		int restoredCount = messages.size();
		for (Object element : messages) {
			facesContext.addMessage(null, (FacesMessage) element);
		}
		return restoredCount;
	}
}
