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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MailConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailServerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 *  @author shood
 * */
public class NotificationConfigurationDto implements Serializable{

    public static final String F_DEFAULT_FROM = "defaultFrom";
    public static final String F_DEBUG = "debug";
    public static final String F_REDIRECT_TO_FILE = "redirectToFile";
    public static final String F_SERVERS = "servers";
    public static final String F_SELECTED_SERVER = "selectedServer";

    private String defaultFrom;
    private boolean debug;
    private String redirectToFile;
    private List<MailServerConfigurationTypeDto> servers;
    private MailServerConfigurationTypeDto selectedServer;


    public NotificationConfigurationDto(){}

    public NotificationConfigurationDto(NotificationConfigurationType config){

        if(config.getMail() != null){
            MailConfigurationType mailConfig = config.getMail();

            defaultFrom = mailConfig.getDefaultFrom();

            if(mailConfig.isDebug() != null){
                debug = mailConfig.isDebug();
            } else {
                debug = false;
            }

            redirectToFile = mailConfig.getRedirectToFile();

            for(MailServerConfigurationType serverConfig : mailConfig.getServer()){
                getServers().add(new MailServerConfigurationTypeDto(serverConfig));
            }
        }
    }

    public NotificationConfigurationType getNewObject(SystemConfigurationType systemConfig) {


		NotificationConfigurationType notificationConfig = (systemConfig.getNotificationConfiguration() != null) ? systemConfig.getNotificationConfiguration() : new NotificationConfigurationType();
		MailConfigurationType mailConfig = (notificationConfig.getMail() != null) ? notificationConfig.getMail() : new MailConfigurationType();

			mailConfig.setDebug(isDebug());
			mailConfig.setDefaultFrom(getDefaultFrom());
			mailConfig.setRedirectToFile(getRedirectToFile());
			mailConfig.getServer().clear();

			for (MailServerConfigurationTypeDto serverDto : getServers()) {
				MailServerConfigurationType newConfig = new MailServerConfigurationType();
				newConfig.setHost(serverDto.getHost());
				newConfig.setPort(serverDto.getPort());
				newConfig.setUsername(serverDto.getUsername());
				newConfig.setTransportSecurity(serverDto.getMailTransportSecurityType());

				if (serverDto.getPassword() != null && StringUtils.isNotEmpty(serverDto.getPassword())) {
					ProtectedStringType pass = new ProtectedStringType();
					pass.setClearValue(serverDto.getPassword());
					newConfig.setPassword(pass);
				} else {
					newConfig.setPassword(serverDto.getOldConfig().getPassword());
				}

				mailConfig.getServer().add(newConfig);
			}

			notificationConfig.setMail(mailConfig);

		return notificationConfig;
	}

    public String getDefaultFrom() {
        return defaultFrom;
    }

    public void setDefaultFrom(String defaultFrom) {
        this.defaultFrom = defaultFrom;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getRedirectToFile() {
        return redirectToFile;
    }

    public void setRedirectToFile(String redirectToFile) {
        this.redirectToFile = redirectToFile;
    }

    public List<MailServerConfigurationTypeDto> getServers() {
        if(servers == null){
            servers = new ArrayList<>();
        }

        return servers;
    }

    public void setServers(List<MailServerConfigurationTypeDto> servers) {
        this.servers = servers;
    }

    public MailServerConfigurationTypeDto getSelectedServer() {
        return selectedServer;
    }

    public void setSelectedServer(MailServerConfigurationTypeDto selectedServer) {
        this.selectedServer = selectedServer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotificationConfigurationDto that = (NotificationConfigurationDto) o;

        if (debug != that.debug) return false;
        if (defaultFrom != null ? !defaultFrom.equals(that.defaultFrom) : that.defaultFrom != null) return false;
        if (redirectToFile != null ? !redirectToFile.equals(that.redirectToFile) : that.redirectToFile != null)
            return false;
        if (servers != null ? !servers.equals(that.servers) : that.servers != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = defaultFrom != null ? defaultFrom.hashCode() : 0;
        result = 31 * result + (debug ? 1 : 0);
        result = 31 * result + (redirectToFile != null ? redirectToFile.hashCode() : 0);
        result = 31 * result + (servers != null ? servers.hashCode() : 0);
        return result;
    }
}
