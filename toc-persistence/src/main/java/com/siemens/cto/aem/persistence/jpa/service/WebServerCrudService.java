package com.siemens.cto.aem.persistence.jpa.service;

import com.siemens.cto.aem.common.exception.NotFoundException;
import com.siemens.cto.aem.common.domain.model.app.Application;
import com.siemens.cto.aem.common.domain.model.event.Event;
import com.siemens.cto.aem.common.domain.model.group.Group;
import com.siemens.cto.aem.common.domain.model.id.Identifier;
import com.siemens.cto.aem.common.domain.model.jvm.Jvm;
import com.siemens.cto.aem.common.domain.model.user.User;
import com.siemens.cto.aem.persistence.jpa.domain.JpaWebServer;
import com.siemens.cto.aem.common.request.webserver.CreateWebServerRequest;
import com.siemens.cto.aem.common.request.webserver.UpdateWebServerRequest;
import com.siemens.cto.aem.common.domain.model.webserver.WebServer;
import com.siemens.cto.aem.common.request.webserver.UploadWebServerTemplateRequest;
import com.siemens.cto.aem.persistence.jpa.domain.JpaWebServerConfigTemplate;

import java.util.List;

public interface WebServerCrudService extends CrudService<JpaWebServer> {

	WebServer createWebServer(WebServer webServer, String createdBy);

	WebServer updateWebServer(WebServer webServer, String createdBy);

	WebServer getWebServer(final Identifier<WebServer> aWebServerId) throws NotFoundException;

	List<WebServer> getWebServers();

	List<WebServer> findWebServers(final String aWebServerNameFragment);

	void removeWebServer(final Identifier<WebServer> aWebServerId);

	List<WebServer> findWebServersBelongingTo(Identifier<Group> aGroupId);

    List<Application> findApplications(final String aWebServerName);

	void removeWebServersBelongingTo(final Identifier<Group> aGroupId);

    WebServer findWebServerByName(final String aWebServerName);

    List<Jvm> findJvms(final String aWebServerName);

    List<String> getResourceTemplateNames(final String webServerName);

    String getResourceTemplate(final String webServerName, final String resourceTemplateName);

	void populateWebServerConfig(List<UploadWebServerTemplateRequest> uploadWSTemplateCommands, User user, boolean overwriteExisting);

	JpaWebServerConfigTemplate uploadWebserverConfigTemplate(Event<UploadWebServerTemplateRequest> event);

    void updateResourceTemplate(final String wsName, final String resourceTemplateName, final String template);
}
