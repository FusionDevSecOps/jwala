package com.siemens.cto.aem.service.resource.impl.handler;

import com.siemens.cto.aem.common.domain.model.app.Application;
import com.siemens.cto.aem.common.domain.model.group.Group;
import com.siemens.cto.aem.common.domain.model.jvm.Jvm;
import com.siemens.cto.aem.common.domain.model.resource.ContentType;
import com.siemens.cto.aem.common.domain.model.resource.ResourceIdentifier;
import com.siemens.cto.aem.common.domain.model.resource.ResourceTemplateMetaData;
import com.siemens.cto.aem.common.request.app.UploadAppTemplateRequest;
import com.siemens.cto.aem.persistence.jpa.domain.JpaJvm;
import com.siemens.cto.aem.persistence.jpa.domain.resource.config.template.ConfigTemplate;
import com.siemens.cto.aem.persistence.service.ApplicationPersistenceService;
import com.siemens.cto.aem.persistence.service.GroupPersistenceService;
import com.siemens.cto.aem.persistence.service.JvmPersistenceService;
import com.siemens.cto.aem.persistence.service.ResourceDao;
import com.siemens.cto.aem.service.exception.ResourceServiceException;
import com.siemens.cto.aem.service.resource.ResourceHandler;
import com.siemens.cto.aem.service.resource.impl.CreateResourceResponseWrapper;
import com.siemens.cto.aem.service.resource.impl.handler.exception.ResourceHandlerException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Handler for a group level application resource identified by a "resource identifier" {@link ResourceIdentifier}
 *
 * Created by JC043760 on 7/21/2016
 */
public class GroupLevelAppResourceHandler extends ResourceHandler {

    private static final String WAR_FILE_EXTENSION = ".war";
    private static final String MSG_ERR_CONVERTING_DATA_INPUTSTREAM_TO_STR = "Error converting data input stream to string!";
    private static final String MSG_CAN_ONLY_HAVE_ONE_WAR = "A web application can only have 1 war file. To change it, delete the war file first before uploading a new one.";

    private final GroupPersistenceService groupPersistenceService;
    private final JvmPersistenceService jvmPersistenceService;
    private final ApplicationPersistenceService applicationPersistenceService;

    public GroupLevelAppResourceHandler(final ResourceDao resourceDao,
                                        final GroupPersistenceService groupPersistenceService,
                                        final JvmPersistenceService jvmPersistenceService,
                                        final ApplicationPersistenceService applicationPersistenceService,
                                        final ResourceHandler successor) {
        this.resourceDao = resourceDao;
        this.groupPersistenceService = groupPersistenceService;
        this.jvmPersistenceService = jvmPersistenceService;
        this.applicationPersistenceService = applicationPersistenceService;
        this.successor = successor;
    }

    @Override
    public ConfigTemplate fetchResource(final ResourceIdentifier resourceIdentifier) {
        ConfigTemplate configTemplate = null;
        if (canHandle(resourceIdentifier)) {
            configTemplate = resourceDao.getGroupLevelAppResource(resourceIdentifier.resourceName, resourceIdentifier.webAppName,
                    resourceIdentifier.groupName);
        } else if (successor != null) {
            configTemplate = successor.fetchResource(resourceIdentifier);
        }
        return configTemplate;
    }

    @Override
    public CreateResourceResponseWrapper createResource(final ResourceIdentifier resourceIdentifier,
                                                        final ResourceTemplateMetaData metaData,
                                                        final InputStream data) {
        CreateResourceResponseWrapper createResourceResponseWrapper = null;
        if (canHandle(resourceIdentifier)) {
            final String groupName = metaData.getEntity().getGroup();
            final Group group = groupPersistenceService.getGroup(groupName);
            final ConfigTemplate createdConfigTemplate;
            final String resourceContent;

            try {
                resourceContent = IOUtils.toString(data);
            } catch (final IOException ioe) {
                throw new ResourceHandlerException(MSG_ERR_CONVERTING_DATA_INPUTSTREAM_TO_STR, ioe);
            }

            if (metaData.getContentType().equals(ContentType.APPLICATION_BINARY.contentTypeStr) &&
                    resourceContent.toLowerCase().endsWith(WAR_FILE_EXTENSION)) {
                final Application app = applicationPersistenceService.getApplication(resourceIdentifier.webAppName);
                if (StringUtils.isEmpty(app.getWarName())) {
                    applicationPersistenceService.updateWarInfo(resourceIdentifier.webAppName, metaData.getTemplateName(),
                            resourceContent);
                } else {
                    throw new ResourceServiceException(MSG_CAN_ONLY_HAVE_ONE_WAR);
                }
            }

            createdConfigTemplate = groupPersistenceService.populateGroupAppTemplate(groupName, resourceIdentifier.webAppName,
                    metaData.getDeployFileName(), convertResourceTemplateMetaDataToJson(metaData), resourceContent);

            // Can't we just get the application using the group name and target app name instead of getting all the applications
            // then iterating it to compare with the target app name ???
            // If we can do that then TODO: Refactor this to return only one application and remove the iteration!
            final List<Application> applications = applicationPersistenceService.findApplicationsBelongingTo(groupName);

            for (final Application application : applications) {
                if (metaData.getEntity().getDeployToJvms() && application.getName().equals(resourceIdentifier.webAppName)) {
                    final byte[] bytes = resourceContent.getBytes();
                    for (final Jvm jvm : group.getJvms()) {
                        UploadAppTemplateRequest uploadAppTemplateRequest = new UploadAppTemplateRequest(application, metaData.getTemplateName(),
                                metaData.getDeployFileName(), jvm.getJvmName(), convertResourceTemplateMetaDataToJson(metaData), new ByteArrayInputStream(bytes)
                        );
                        JpaJvm jpaJvm = jvmPersistenceService.getJpaJvm(jvm.getId(), false);
                        applicationPersistenceService.uploadAppTemplate(uploadAppTemplateRequest, jpaJvm);
                    }
                }
            }

            createResourceResponseWrapper = new CreateResourceResponseWrapper(createdConfigTemplate);
        } else if (successor != null) {
            createResourceResponseWrapper = successor.createResource(resourceIdentifier, metaData, data);
        }
        return createResourceResponseWrapper;
    }

    @Override
    public void deleteResource(final ResourceIdentifier resourceIdentifier) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean canHandle(final ResourceIdentifier resourceIdentifier) {
        return StringUtils.isNotEmpty(resourceIdentifier.resourceName) &&
               StringUtils.isNotEmpty(resourceIdentifier.webAppName) &&
               StringUtils.isNotEmpty(resourceIdentifier.groupName) &&
               StringUtils.isEmpty(resourceIdentifier.webServerName) &&
               StringUtils.isEmpty(resourceIdentifier.jvmName);
    }
}
