package com.siemens.cto.aem.service.app;

import com.siemens.cto.aem.common.domain.model.app.Application;
import com.siemens.cto.aem.common.request.app.UploadWebArchiveRequest;
import com.siemens.cto.toc.files.RepositoryFileInformation;

/**
 * Not to be used as entry points, these APIs are called indirectly from 
 * {@link com.siemens.cto.aem.service.app.impl.ApplicationServiceImpl}
 */
public interface PrivateApplicationService {

    RepositoryFileInformation uploadWebArchiveData(UploadWebArchiveRequest uploadWebArchiveRequest);

    Application uploadWebArchiveUpdateDB(UploadWebArchiveRequest uploadWebArchiveRequest, RepositoryFileInformation result);
}
