/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.api.transports;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import java.util.List;

/**
 * @author mederly
 */
public class Message implements DebugDumpable {

    private List<String> to;
    private List<String> cc;
    private List<String> bcc;
    private String subject;
    private String body;         // todo
    private String contentType;
    private String from;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }
    
    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }
    
    public List<String> getBcc() {
        return bcc;
    }

    public void setBcc(List<String> bcc) {
        this.bcc = bcc;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    @Override
    public String toString() {
    	String fromString = from!= null ? ", from='" + from + "' " : "";
    	String ccString = cc!= null ? ", cc='" + cc + "' " : "";
    	String bccString = bcc!= null ? ", bcc='" + bcc + "' " : "";
        return "Message{" +
        		"to='" + to + '\'' +
        		fromString +
                ccString +
                bccString +
                ", subject='" + subject + '\'' +
                ", contentType='" + contentType + '\'' +
                ", body='" + body + '\'' +
                '}';
    }

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder rv = new StringBuilder();
		rv.append("\n");
		DebugUtil.debugDumpLabel(rv, "Message", indent);
		rv.append("\n");

		if (from != null){
			DebugUtil.debugDumpWithLabel(rv, "From", from, indent+1);
			rv.append("\n");
		}
		
		DebugUtil.debugDumpWithLabel(rv, "To", to, indent+1);
		rv.append("\n");

		if (cc != null){
			DebugUtil.debugDumpWithLabel(rv, "Cc", cc, indent+1);
			rv.append("\n");
		}
		if (bcc != null){
			DebugUtil.debugDumpWithLabel(rv, "Bcc", bcc, indent+1);
			rv.append("\n");
		}
		
		DebugUtil.debugDumpWithLabel(rv, "Subject", subject, indent+1);
		rv.append("\n");

		DebugUtil.debugDumpWithLabel(rv, "Body", body, indent+1);
		return rv.toString();
	}
}
