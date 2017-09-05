/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import java.io.Serializable;

import com.evolveum.midpoint.web.component.util.Choiceable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailServerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailTransportSecurityType;

/**
 *  @author shood
 * */
public class MailServerConfigurationTypeDto implements Serializable, Choiceable {

    public static final String F_HOST = "host";
    public static final String F_PORT = "port";
    public static final String F_USERNAME = "username";
    public static final String F_PASSWORD = "password";
    public static final String F_MAIL_TRANSPORT_SECURITY_TYPE = "mailTransportSecurityType";

    private MailServerConfigurationType oldConfig;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private MailTransportSecurityType mailTransportSecurityType;

    public MailServerConfigurationTypeDto(){
        oldConfig = new MailServerConfigurationType();
    }

    public MailServerConfigurationTypeDto(MailServerConfigurationType config){

        oldConfig = config;
        host = config.getHost();
        port = config.getPort();
        username = config.getUsername();

        if(config.getPassword() != null){
            password = "";
        } else {
            password = null;
        }

        mailTransportSecurityType = config.getTransportSecurity();
    }

    @Override
    public String getName() {
    	return host;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if(password == null || password.isEmpty()){
            return;
        }

        this.password = password;
    }

    public MailTransportSecurityType getMailTransportSecurityType() {
        return mailTransportSecurityType;
    }

    public void setMailTransportSecurityType(MailTransportSecurityType mailTransportSecurityType) {
        this.mailTransportSecurityType = mailTransportSecurityType;
    }

    public MailServerConfigurationType getOldConfig() {
        return oldConfig;
    }

    public void setOldConfig(MailServerConfigurationType oldConfig) {
        this.oldConfig = oldConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MailServerConfigurationTypeDto)) return false;

        MailServerConfigurationTypeDto that = (MailServerConfigurationTypeDto) o;

        if (host != null ? !host.equals(that.host) : that.host != null) return false;
        if (mailTransportSecurityType != that.mailTransportSecurityType) return false;
        if (oldConfig != null ? !oldConfig.equals(that.oldConfig) : that.oldConfig != null) return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (port != null ? !port.equals(that.port) : that.port != null) return false;
        if (username != null ? !username.equals(that.username) : that.username != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = oldConfig != null ? oldConfig.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (port != null ? port.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (mailTransportSecurityType != null ? mailTransportSecurityType.hashCode() : 0);
        return result;
    }
}
