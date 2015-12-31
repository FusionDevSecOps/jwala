package com.siemens.cto.toc.files;

import com.siemens.cto.aem.common.request.app.RemoveWebArchiveRequest;
import com.siemens.cto.aem.common.request.app.UploadWebArchiveRequest;
import com.siemens.cto.aem.common.domain.model.event.Event;

import java.io.IOException;

public interface WebArchiveManager {

    RepositoryFileInformation store(UploadWebArchiveRequest uploadWebArchiveRequest) throws IOException;

    RepositoryFileInformation remove(RemoveWebArchiveRequest removeWebArchiveRequest) throws IOException;

}
