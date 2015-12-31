package com.siemens.cto.aem.service.app.impl;

import com.siemens.cto.aem.common.request.app.UploadWebArchiveRequest;
import com.siemens.cto.aem.common.exception.BadRequestException;
import com.siemens.cto.aem.common.domain.model.app.Application;
import com.siemens.cto.aem.common.domain.model.event.Event;
import com.siemens.cto.aem.common.domain.model.fault.AemFaultType;
import com.siemens.cto.aem.persistence.service.ApplicationPersistenceService;
import com.siemens.cto.aem.service.app.PrivateApplicationService;
import com.siemens.cto.toc.files.RepositoryFileInformation;
import com.siemens.cto.toc.files.WebArchiveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

public class PrivateApplicationServiceImpl implements PrivateApplicationService {

    private final static Logger LOGGER = LoggerFactory.getLogger(PrivateApplicationServiceImpl.class);

    @Autowired
    private ApplicationPersistenceService applicationPersistenceService;

    @Autowired
    private WebArchiveManager webArchiveManager;

    /**
     * Helper method - non-transactional upload.
     */
    @Override
    public RepositoryFileInformation uploadWebArchiveData(final UploadWebArchiveRequest uploadWebArchiveRequest) {
        RepositoryFileInformation result = null;

        try {
            result = webArchiveManager.store(uploadWebArchiveRequest);
            LOGGER.info("Archive Upload: " + result);
        } catch (IOException e) {
            //This is logged here instead of included in the BadRequestException because it's a potential CSRF vector
            LOGGER.warn("IOException occurred while trying to upload Web Archive Data", e);
            throw new BadRequestException(AemFaultType.BAD_STREAM,
                                          "Error storing data");
        }

        Long bytes = result.getLength();

        if(uploadWebArchiveRequest == null || uploadWebArchiveRequest.getLength() != -1 && bytes != null && !bytes.equals(uploadWebArchiveRequest.getLength())) {
            throw new BadRequestException(AemFaultType.BAD_STREAM, "Post-condition file length check failed");
        }

        return result;
    }

    /**
     * Helper method - transactional update of database.
     */
    @Override
    @Transactional
    public Application uploadWebArchiveUpdateDB(final UploadWebArchiveRequest uploadWebArchiveRequest, final RepositoryFileInformation result) {

        return applicationPersistenceService.updateWARPath(uploadWebArchiveRequest, result.getPath().toAbsolutePath().toString());

    }

}
