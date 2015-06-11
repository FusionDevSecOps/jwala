package com.siemens.cto.deploy.http

import groovy.json.JsonSlurper

public class RestException extends RuntimeException {

    RestException(String s) {
        super(s)
    }

    RestException(String msg, String errorResponse) {
        super(staticDecode(msg,errorResponse))
    }
    
    private static String staticDecode(msg,errorJsonString) {
        try { 
            JsonSlurper slurper = new JsonSlurper();
            def errorResponse = slurper.parseText(errorJsonString);
            return "${msg}: ${errorResponse.msgCode}(${errorResponse.message}) '${errorResponse.applicationResponseContent}'"            
        } catch(Exception e) {
            // if we cannot parse, just include the result in the error
            return "${msg}: ${errorJsonString}";
        }
    }
    
    // for your reference
    public enum HttpClientAemFaultTypes { 
    
    GROUP_NOT_FOUND("AEM1", "GroupNotFound"),
    
    INVALID_GROUP_NAME("AEM2", "InvalidGroupName"),
    
    INVALID_IDENTIFIER("AEM3", "InvalidIdentifier"),
    
    DUPLICATE_GROUP_NAME("AEM4", "DuplicateGroupName"),
    
    INVALID_JVM_NAME("AEM5", "InvalidJvmName"),
    
    INVALID_HOST_NAME("AEM6", "InvalidHostName"),
    
    GROUP_NOT_SPECIFIED("AEM7", "GroupNotSpecified"),
    
    JVM_NOT_FOUND("AEM8", "JvmNotFound"),
    
    JVM_NOT_SPECIFIED("AEM9", "JvmNotSpecified"),
    
    WEBSERVER_NOT_FOUND("AEM10", "WebServerNotFound"),
    
    INVALID_WEBSERVER_NAME("AEM11", "InvalidWebServerName"),
    
    INVALID_WEBSERVER_HOST("AEM12", "InvalidWebServerHostName"),
    
    INVALID_WEBSERVER_PORT("AEM13", "InvalidWebServerPortNumber"),
    
    WEBSERVER_NOT_SPECIFIED("AEM14", "WebServerNotSpecified"),
    
    JVM_ALREADY_BELONGS_TO_GROUP("AEM15", "JvmAlreadyBelongsToGroup"),
    
    APPLICATION_NOT_FOUND("AEM16", "ApplicationNotFound"),
    
    INVALID_APPLICATION_NAME("AEM17", "InvalidApplicationName"),
    
    INVALID_APPLICATION_CTX("AEM18", "InvalidApplicationContextPath"),
    
    INVALID_APPLICATION_WAR("AEM19", "InvalidApplicationWarPath"),
    
    APPLICATION_NOT_SPECIFIED("AEM20", "WebServerNotSpecified"),
    
    INVALID_JVM_OPERATION("AEM21", "InvalidJvmOperation"),
    
    JVM_CONTROL_HISTORY_NOT_FOUND("AEM22", "JvmControlHistoryNotFound"),
    
    REMOTE_COMMAND_FAILURE("AEM23", "RemoteCommandFailure"),
    
    CONTROL_OPERATION_UNSUCCESSFUL("AEM24", "ControlOperationUnsuccessful"),
    
    DUPLICATE_APPLICATION("AEM25", "DuplicateApplication"),
    
    PERSISTENCE_ERROR("AEM26","PersistenceError"),
    
    INVALID_WEBSERVER_OPERATION("AEM27", "InvalidWebServerOperation"),
    
    WEBSERVER_CONTROL_HISTORY_NOT_FOUND("AEM28", "WebServerControlHistoryNotFound"),
    
    BAD_STREAM("AEM29", "UploadStreamNotReadable"),
    
    INVALID_WEB_ARCHIVE_NAME("AEM30", "InvalidWebArchiveName"),
    
    INVALID_JVM_HTTP_PORT("AEM31", "InvalidJvmHttpPortNumber"),
    
    INVALID_JVM_HTTPS_PORT("AEM32", "InvalidJvmHttpsPortNumber"),
    
    INVALID_JVM_REDIRECT_PORT("AEM33", "InvalidJvmRedirectPortNumber"),
    
    INVALID_JVM_SHUTDOWN_PORT("AEM34", "InvalidJvmShutdownPortNumber"),
    
    INVALID_JVM_AJP_PORT("AEM35", "InvalidJvmAjpPortNumber"),
    
    INVALID_WEBSERVER_HTTPS_PORT("AEM36", "InvalidWebServerHttpsPortNumber"),
    
    JVM_STATE_NOT_SPECIFIED("AEM37", "JvmStateNotSpecified"),
    
    WEB_SERVER_HTTPD_CONF_TEMPLATE_NOT_FOUND("AEM38", "WebServerHttpdConfTemplateNotFound"),
    
    GROUP_CONTROL_HISTORY_NOT_FOUND("AEM39", "GroupControlHistoryNotFound"),
    
    GROUP_STATE_NOT_SPECIFIED("AEM40", "GroupStateNotSpecified"),
    
    WEB_SERVER_REACHABLE_STATE_NOT_SPECIFIED("AEM41", "WebServerReachableStateNotSpecified"),
    
    INVALID_GROUP_OPERATION("AEM42", "InvalidGroupOperation"),
    
    SSH_CONFIG_MISSING("AEM43", "SshConfigMissing"),
    
    USER_AUTHENTICATION_FAILED("AEM44", "UserAuthenticationFailed"),
    
    INVALID_STATUS_PATH("AEM45", "InvalidStatusPath"),
    
    INVALID_HTTP_CONFIG_FILE("AEM46", "InvalidHttpConfigFile"),
    
    TEMPLATE_NOT_FOUND("AEM47", "TemplateNotFound"),
    
    CANNOT_CONNECT("AEM48", "CannotConnect"),
    
    INVALID_PATH("AEM49", "InvalidPath");
    }
}
