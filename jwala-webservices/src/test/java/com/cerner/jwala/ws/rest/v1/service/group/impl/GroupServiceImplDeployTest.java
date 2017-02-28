package com.cerner.jwala.ws.rest.v1.service.group.impl;

import com.cerner.jwala.common.domain.model.app.Application;
import com.cerner.jwala.common.domain.model.fault.FaultType;
import com.cerner.jwala.common.domain.model.group.Group;
import com.cerner.jwala.common.domain.model.id.Identifier;
import com.cerner.jwala.common.domain.model.jvm.Jvm;
import com.cerner.jwala.common.domain.model.jvm.JvmState;
import com.cerner.jwala.common.domain.model.resource.Entity;
import com.cerner.jwala.common.domain.model.resource.ResourceGroup;
import com.cerner.jwala.common.domain.model.resource.ResourceTemplateMetaData;
import com.cerner.jwala.common.domain.model.user.User;
import com.cerner.jwala.common.domain.model.webserver.WebServer;
import com.cerner.jwala.common.domain.model.webserver.WebServerReachableState;
import com.cerner.jwala.common.exception.InternalErrorException;
import com.cerner.jwala.common.exec.CommandOutput;
import com.cerner.jwala.common.exec.ExecReturnCode;
import com.cerner.jwala.common.properties.ApplicationProperties;
import com.cerner.jwala.common.request.group.CreateGroupRequest;
import com.cerner.jwala.common.request.jvm.ControlJvmRequest;
import com.cerner.jwala.exception.CommandFailureException;
import com.cerner.jwala.persistence.jpa.service.exception.ResourceTemplateUpdateException;
import com.cerner.jwala.service.HistoryFacadeService;
import com.cerner.jwala.service.app.ApplicationService;
import com.cerner.jwala.service.binarydistribution.BinaryDistributionControlService;
import com.cerner.jwala.service.binarydistribution.BinaryDistributionLockManager;
import com.cerner.jwala.service.binarydistribution.BinaryDistributionService;
import com.cerner.jwala.service.binarydistribution.impl.BinaryDistributionLockManagerImpl;
import com.cerner.jwala.service.group.GroupControlService;
import com.cerner.jwala.service.group.GroupJvmControlService;
import com.cerner.jwala.service.group.GroupService;
import com.cerner.jwala.service.group.GroupWebServerControlService;
import com.cerner.jwala.service.jvm.JvmControlService;
import com.cerner.jwala.service.jvm.JvmService;
import com.cerner.jwala.service.resource.ResourceService;
import com.cerner.jwala.service.webserver.WebServerCommandService;
import com.cerner.jwala.service.webserver.WebServerControlService;
import com.cerner.jwala.service.webserver.WebServerService;
import com.cerner.jwala.ws.rest.v1.provider.AuthenticatedUser;
import com.cerner.jwala.ws.rest.v1.response.ApplicationResponse;
import com.cerner.jwala.ws.rest.v1.service.app.ApplicationServiceRest;
import com.cerner.jwala.ws.rest.v1.service.app.impl.ApplicationServiceRestImpl;
import com.cerner.jwala.ws.rest.v1.service.group.GroupServiceRest;
import com.cerner.jwala.ws.rest.v1.service.jvm.JvmServiceRest;
import com.cerner.jwala.ws.rest.v1.service.jvm.impl.JvmServiceRestImpl;
import com.cerner.jwala.ws.rest.v1.service.webserver.WebServerServiceRest;
import com.cerner.jwala.ws.rest.v1.service.webserver.impl.WebServerServiceRestImpl;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class,
        classes = {GroupServiceImplDeployTest.Config.class
        })
@Ignore
public class GroupServiceImplDeployTest {

    @Autowired
    GroupServiceRest groupServiceRest;

    @Autowired
    JvmServiceRest jvmServiceRest;

    @Autowired
    WebServerServiceRest webServerServiceRest;

    @Autowired
    ApplicationServiceRest applicationServiceRest;

    static final BinaryDistributionService binaryDistributionService = mock(BinaryDistributionService.class);
    static final BinaryDistributionControlService binaryDistributionControlService = mock(BinaryDistributionControlService.class);
    static final GroupService mockGroupService = mock(GroupService.class);
    static final ResourceService mockResourceService = mock(ResourceService.class);
    static final GroupControlService mockGroupControlService = mock(GroupControlService.class);
    static final GroupJvmControlService mockGroupJvmControlService = mock(GroupJvmControlService.class);
    static final GroupWebServerControlService mockGroupWebServerControlService = mock(GroupWebServerControlService.class);
    static final JvmService mockJvmService = mock(JvmService.class);
    static final JvmControlService mockJvmControlService = mock(JvmControlService.class);
    static final WebServerService mockWebServerService = mock(WebServerService.class);
    static final WebServerControlService mockWebServerControlService = mock(WebServerControlService.class);
    static final ApplicationService mockApplicationService = mock(ApplicationService.class);
    static final ApplicationServiceRest mockApplicationServiceRest = mock(ApplicationServiceRest.class);
    static final WebServerServiceRest mockWebServerServiceRest = mock(WebServerServiceRest.class);
    static final HistoryFacadeService mockHistoryService = mock(HistoryFacadeService.class);
    static final BinaryDistributionLockManager mockBinaryDistributionLockManager = mock(BinaryDistributionLockManager.class);


    private AuthenticatedUser mockAuthUser = mock(AuthenticatedUser.class);
    private User mockUser = mock(User.class);
    private String httpdConfDirPath;

    public GroupServiceImplDeployTest() {
        System.setProperty(ApplicationProperties.PROPERTIES_ROOT_PATH, "./src/test/resources");
    }

    @Before
    public void setUp() {
        when(mockAuthUser.getUser()).thenReturn(mockUser);

        System.setProperty(ApplicationProperties.PROPERTIES_ROOT_PATH, "./src/test/resources");
        httpdConfDirPath = ApplicationProperties.get("remote.paths.httpd.conf");
        // assertTrue(new File(httpdConfDirPath).mkdirs());
        new File(httpdConfDirPath).mkdirs();
        // assertTrue(new File(generatedResourceDir).mkdirs());
        new File(httpdConfDirPath).mkdirs();
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File(httpdConfDirPath));
        System.clearProperty(ApplicationProperties.PROPERTIES_ROOT_PATH);
    }

    @Test
    public void testCreateGroup() {
        reset(mockResourceService);

        Group mockGroup = mock(Group.class);
        when(mockGroupService.createGroup(any(CreateGroupRequest.class), any(User.class))).thenReturn(mockGroup);

        groupServiceRest.createGroup("testGroup", mockAuthUser);

        verify(mockGroupService, times(1)).createGroup(any(CreateGroupRequest.class), any(User.class));
    }

    @Test
    public void testGroupJvmDeploy() {
        final Response response = groupServiceRest.generateAndDeployGroupJvmFile("testGroup", "server.xml", mockAuthUser);
        verify(mockGroupService).generateAndDeployGroupJvmFile(eq("testGroup"), eq("server.xml"), any(User.class));
        assertEquals(200, response.getStatus());
    }

    @Test
    @Ignore
    // TODO: Fix this.
    public void testGroupWebServerDeploy() throws CommandFailureException, IOException {
        Group mockGroup = mock(Group.class);
        WebServer mockWebServer = mock(WebServer.class);
        Response mockResponse = mock(Response.class);

        Set<WebServer> webServerSet = new HashSet<>();
        webServerSet.add(mockWebServer);

        when(mockGroup.getWebServers()).thenReturn(webServerSet);
        when(mockWebServer.getName()).thenReturn("testWebServer");
        when(mockWebServer.getId()).thenReturn(new Identifier<WebServer>(99L));
        when(mockWebServer.getState()).thenReturn(WebServerReachableState.WS_UNREACHABLE);
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockGroupService.getGroup(anyString())).thenReturn(mockGroup);
        when(mockGroupService.getGroupWithWebServers(any(Identifier.class))).thenReturn(mockGroup);
        when(mockGroupService.getGroupWebServerResourceTemplate(anyString(), anyString(), anyBoolean(), any(ResourceGroup.class))).thenReturn("new httpd.conf context");
        when(mockResourceService.generateResourceGroup()).thenReturn(new ResourceGroup());
        when(mockWebServerService.getResourceTemplateMetaData(anyString(), anyString())).thenReturn("{\"contentType\":\"text/plain\",\"deployPath\":\"./anyPath\"}");
        when(mockWebServerService.updateResourceTemplate(anyString(), anyString(), anyString())).thenReturn("new httpd.conf context");
        when(binaryDistributionControlService.secureCopyFile(anyString(), anyString(), anyString())).thenReturn(new CommandOutput(new ExecReturnCode(0), "SUCCESS", ""));
        Response returnedResponse = groupServiceRest.generateAndDeployGroupWebServersFile("testGroup", "httpd.conf", mockAuthUser);
        assertEquals(200, returnedResponse.getStatusInfo().getStatusCode());

//        when(mockWebServerControlService.secureCopyFile(anyString(), anyString(), anyString(), anyString())).thenReturn(new CommandOutput(new ExecReturnCode(1), "", "NOT OK"));
        try {
            groupServiceRest.generateAndDeployGroupWebServersFile("testGroup", "httpd.conf", mockAuthUser);
        } catch (InternalErrorException ie) {
            assertEquals(FaultType.REMOTE_COMMAND_FAILURE, ie.getMessageResponseStatus());
        }
    }

    @Test(expected = InternalErrorException.class)
    public void testGenerateAndDeployGroupWebServerFileWithWebServerStarted() {
        Group mockGroup = mock(Group.class);
        WebServer mockWebServer = mock(WebServer.class);
        Set<WebServer> webServerSet = new HashSet<>();
        webServerSet.add(mockWebServer);
        when(mockGroupService.getGroupWithWebServers(any(Identifier.class))).thenReturn(mockGroup);
        when(mockGroupService.getGroup(anyString())).thenReturn(mockGroup);
        when(mockGroupService.getGroupWebServerResourceTemplate(anyString(), anyString(), anyBoolean(), any(ResourceGroup.class))).thenReturn("Httpd.conf template content");
        when(mockGroup.getWebServers()).thenReturn(webServerSet);
        when(mockWebServerService.isStarted(any(WebServer.class))).thenReturn(true);
        groupServiceRest.generateAndDeployGroupWebServersFile("groupName", "httpd.conf", mockAuthUser);
    }

    @Test
    public void testGroupAppDeploy() throws CommandFailureException, IOException {
        Group mockGroup = mock(Group.class);
        Jvm mockJvm = mock(Jvm.class);
        Application mockApp = mock(Application.class);
        Response mockResponse = mock(Response.class);
        String hostName = "testHost";

        Set<Jvm> jvmSet = new HashSet<>();
        jvmSet.add(mockJvm);
        Set<Application> appSet = new HashSet<>();
        appSet.add(mockApp);

        when(mockApp.getName()).thenReturn("testApp");
        when(mockGroup.getJvms()).thenReturn(jvmSet);
        when(mockJvm.getJvmName()).thenReturn("testJvm");
        when(mockJvm.getId()).thenReturn(new Identifier<Jvm>(99L));
        when(mockJvm.getState()).thenReturn(JvmState.JVM_STOPPED);
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockGroupService.getGroup(anyString())).thenReturn(mockGroup);
        when(mockGroupService.getGroupAppResourceTemplate(anyString(), anyString(), anyString(), anyBoolean(), any(ResourceGroup.class))).thenReturn("new hct.xml content");
        when(mockGroupService.getGroupAppResourceTemplateMetaData(anyString(), anyString())).thenReturn("{\"entity\":{\"target\": \"testApp\"}}");
        when(mockJvmService.getJvm(anyString())).thenReturn(mockJvm);
        when(mockApplicationService.updateResourceTemplate(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("new hct.xml content");
        when(mockApplicationService.deployConf(anyString(), anyString(), anyString(), anyString(), any(ResourceGroup.class), any(User.class))).thenReturn(new CommandOutput(new ExecReturnCode(0), "SUCCESS", ""));
        when(mockApplicationServiceRest.deployConf(anyString(), anyString(), anyString(), anyString(), any(AuthenticatedUser.class))).thenReturn(mockResponse);

        ResourceTemplateMetaData mockMetaData = mock(ResourceTemplateMetaData.class);
        Entity mockEntity = mock(Entity.class);
        when(mockMetaData.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getDeployToJvms()).thenReturn(true);
        when(mockResourceService.getMetaData(anyString())).thenReturn(mockMetaData);

        Response returnResponse = groupServiceRest.generateAndDeployGroupAppFile("testGroup", "hct.xml", "testApp", mockAuthUser, null);
        assertEquals(200, returnResponse.getStatus());

        when(mockJvm.getHostName()).thenReturn("TestHost");
        returnResponse = groupServiceRest.generateAndDeployGroupAppFile("testGroup", "hct.xml", "testApp", mockAuthUser, hostName);
        assertEquals(200, returnResponse.getStatus());

        when(mockJvm.getHostName()).thenReturn("otherhostname");
        returnResponse = groupServiceRest.generateAndDeployGroupAppFile("testGroup", "hct.xml", "testApp", mockAuthUser, hostName);
        assertEquals(200, returnResponse.getStatus());

        when(mockGroupService.getGroupAppResourceTemplateMetaData(anyString(), anyString())).thenReturn("{\"entity\":{\"target\": \"testApp\", \"deployToJvms\": false}}");
        when(mockJvm.getHostName()).thenReturn("TestHost");
        when(mockGroupService.deployGroupAppTemplate(anyString(), anyString(), any(Application.class), anyString())).thenReturn(
                new CommandOutput(new ExecReturnCode(0), "SUCCESS", ""));
        returnResponse = groupServiceRest.generateAndDeployGroupAppFile("testGroup", "hct.xml", "testApp", mockAuthUser, hostName);
        assertEquals(200, returnResponse.getStatus());

        when(mockApplicationService.deployConf(anyString(), anyString(), anyString(), anyString(), any(ResourceGroup.class), any(User.class))).thenReturn(new CommandOutput(new ExecReturnCode(1), "", "NOT OK"));
        try {
            groupServiceRest.generateAndDeployGroupAppFile("testGroup", "hct.xml", "testApp", mockAuthUser, null);
        } catch (InternalErrorException ie) {
            assertEquals(FaultType.REMOTE_COMMAND_FAILURE, ie.getMessageResponseStatus());
        }

        boolean internalErrorException = false;
        when(mockJvm.getState()).thenReturn(JvmState.JVM_STARTED);
        try {
            groupServiceRest.generateAndDeployGroupAppFile("testGroup", "hct.xml", "testApp", mockAuthUser, null);
        } catch (InternalErrorException ie) {
            internalErrorException = true;
            assertTrue(ie.getMessage().contains("All JVMs in the group must be stopped"));
        }
        assertTrue(internalErrorException);
    }

    @Test
    public void testGroupAppDeployNotToJvms() throws IOException {
        Group mockGroup = mock(Group.class);
        Jvm mockJvm = mock(Jvm.class);
        Application mockApp = mock(Application.class);
        Response mockResponse = mock(Response.class);

        Set<Jvm> jvmSet = new HashSet<>();
        jvmSet.add(mockJvm);
        Set<Application> appSet = new HashSet<>();
        appSet.add(mockApp);

        reset(mockResourceService);
        when(mockApp.getName()).thenReturn("testApp");
        when(mockGroup.getJvms()).thenReturn(jvmSet);
        when(mockJvm.getJvmName()).thenReturn("testJvm");
        when(mockJvm.getId()).thenReturn(new Identifier<Jvm>(99L));
        when(mockJvm.getState()).thenReturn(JvmState.JVM_STOPPED);
        when(mockJvm.getHostName()).thenReturn("mockHost");
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockGroupService.getGroup(anyString())).thenReturn(mockGroup);
        when(mockGroupService.getGroupAppResourceTemplate(anyString(), anyString(), anyString(), anyBoolean(), any(ResourceGroup.class))).thenReturn("new hct.xml content");
        when(mockGroupService.getGroupAppResourceTemplateMetaData(anyString(), anyString())).thenReturn("{\"entity\":{\"target\": \"testApp\", \"deployToJvms\":false}}");
        when(mockGroupService.deployGroupAppTemplate(anyString(), anyString(), any(Application.class), any(Jvm.class))).thenReturn(new CommandOutput(new ExecReturnCode(0), "SUCCESS", ""));
        when(mockJvmService.getJvm(anyString())).thenReturn(mockJvm);
        when(mockApplicationService.updateResourceTemplate(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("new hct.xml content");
        when(mockResourceService.generateResourceGroup()).thenReturn(new ResourceGroup());
        ResourceTemplateMetaData mockMetaData = mock(ResourceTemplateMetaData.class);
        Entity mockEntity = mock(Entity.class);
        when(mockMetaData.getEntity()).thenReturn(mockEntity);
        when(mockEntity.getDeployToJvms()).thenReturn(false);
        when(mockEntity.getTarget()).thenReturn("testApp");
        when(mockResourceService.getMetaData(anyString())).thenReturn(mockMetaData);
        Response returnResponse = groupServiceRest.generateAndDeployGroupAppFile("testGroup", "hct.xml", "testApp", mockAuthUser, null);
        assertEquals(200, returnResponse.getStatus());

        when(mockJvm.getState()).thenReturn(JvmState.JVM_STARTED);
        boolean internalErrorExceptionThrown = false;
        try {
            groupServiceRest.generateAndDeployGroupAppFile("testGroup", "hct.xml", "testApp", mockAuthUser, null);
        } catch (InternalErrorException e) {
            internalErrorExceptionThrown = true;
        }
        assertTrue(internalErrorExceptionThrown);
    }

    @Test (expected = InternalErrorException.class)
    public void testGroupAppDeployToHostsFailedForStartedJvm() throws IOException {
        Group mockGroup = mock(Group.class);
        Jvm mockJvm = mock(Jvm.class);
        when(mockJvm.getHostName()).thenReturn("test-host-name");
        when(mockJvm.getState()).thenReturn(JvmState.JVM_STARTED);
        when(mockJvm.getJvmName()).thenReturn("test-jvm-name");
        when(mockGroup.getJvms()).thenReturn(Collections.singleton(mockJvm));

        when(mockGroupService.getGroup(anyString())).thenReturn(mockGroup);
        when(mockGroupService.getGroupAppResourceTemplateMetaData(anyString(), anyString())).thenReturn("{\"entity\":{\"target\": \"testApp\", \"deployToJvms\":false}}");

        ResourceTemplateMetaData mockMetaData = mock(ResourceTemplateMetaData.class);
        Entity mockMetaDataEntity = mock(Entity.class);
        when(mockMetaData.getEntity()).thenReturn(mockMetaDataEntity);
        when(mockMetaDataEntity.getDeployToJvms()).thenReturn(false);
        when(mockResourceService.getMetaData(anyString())).thenReturn(mockMetaData);

        groupServiceRest.generateAndDeployGroupAppFile("test-group-name", "test.properties", "test-app-name", mockAuthUser, "test-host-name");
    }

    @Test (expected = InternalErrorException.class)
    public void testGenerateGroupWebServersWithWebServerStarted() {
        Group mockGroup = mock(Group.class);
        Set<WebServer> webServersSet = new HashSet<>();
        WebServer mockWebServer = mock(WebServer.class);
        webServersSet.add(mockWebServer);
        when(mockGroup.getWebServers()).thenReturn(webServersSet);
        when(mockWebServerService.isStarted(any(WebServer.class))).thenReturn(true);
        when(mockGroupService.getGroupWithWebServers(any(Identifier.class))).thenReturn(mockGroup);
        groupServiceRest.generateGroupWebservers(new Identifier<Group>(111L), mockAuthUser);
    }

    @Test
    public void testGenerateGroupWebServersWithNoWebServers() {
        Group mockGroup = mock(Group.class);
        Set<WebServer> webServersSet = new HashSet<>();
        when(mockGroup.getWebServers()).thenReturn(webServersSet);
        when(mockGroupService.getGroupWithWebServers(any(Identifier.class))).thenReturn(mockGroup);
        Response response = groupServiceRest.generateGroupWebservers(new Identifier<Group>(111L), mockAuthUser);
        assertTrue(response.getStatus() > 199 && response.getStatus() < 300);
    }

    @Test
    public void testGenerateAndDeployJvms() throws CommandFailureException {
        reset(mockGroupService);
        reset(mockJvmService);
        reset(mockJvmControlService);
        reset(mockResourceService);

        Group mockGroup = mock(Group.class);
        Jvm mockJvm = mock(Jvm.class);
        CommandOutput successCommandOutput = new CommandOutput(new ExecReturnCode(0), "SUCCESS", "");
        Set<Jvm> jvmsSet = new HashSet<>();
        jvmsSet.add(mockJvm);

        when(mockGroup.getJvms()).thenReturn(jvmsSet);
        when(mockJvm.getJvmName()).thenReturn("jvmName");
        when(mockJvm.getState()).thenReturn(JvmState.JVM_STOPPED);
        when(mockJvm.getId()).thenReturn(new Identifier<Jvm>(1111L));
        when(mockGroupService.getGroup(any(Identifier.class))).thenReturn(mockGroup);
        when(mockJvmService.getJvm(anyString())).thenReturn(mockJvm);
        when(mockJvmControlService.controlJvm(any(ControlJvmRequest.class), any(User.class))).thenReturn(successCommandOutput);
        when(mockJvmControlService.executeCreateDirectoryCommand(any(Jvm.class), anyString())).thenReturn(successCommandOutput);
        when(mockJvmControlService.executeChangeFileModeCommand(any(Jvm.class), anyString(), anyString(), anyString())).thenReturn(successCommandOutput);
        when(mockJvmControlService.secureCopyFile(any(ControlJvmRequest.class), anyString(), anyString(), anyString(), anyBoolean())).thenReturn(successCommandOutput);

        Response response = groupServiceRest.generateGroupJvms(new Identifier<Group>(111L), mockAuthUser);
        assertNotNull(response);
    }

    @Test
    public void testGenerateAndDeployJvmsNoJvms() {
        Group mockGroup = mock(Group.class);
        when(mockGroup.getJvms()).thenReturn(new HashSet<Jvm>());
        when(mockGroupService.getGroup(any(Identifier.class))).thenReturn(mockGroup);
        Response response = groupServiceRest.generateGroupJvms(new Identifier<Group>(11212L), mockAuthUser);
        assertTrue(response.getStatus() > 199 && response.getStatus() < 300);

    }

    @Test (expected = InternalErrorException.class)
    public void testGenerateGroupJvmsWithJvmStarted() {
        Group mockGroup = mock(Group.class);
        Set<Jvm> jvmSet = new HashSet<>();
        Jvm mockJvm = mock(Jvm.class);
        jvmSet.add(mockJvm);
        when(mockJvm.getState()).thenReturn(JvmState.JVM_STARTED);
        when(mockGroup.getJvms()).thenReturn(jvmSet);
        when(mockGroupService.getGroup(any(Identifier.class))).thenReturn(mockGroup);
        groupServiceRest.generateGroupJvms(new Identifier<Group>(111L), mockAuthUser);
    }

    @Test
    public void testUpdateGroupAppTemplate() {
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(200);

        Group mockGroupWithJvms = mock(Group.class);
        Jvm mockJvm = mock(Jvm.class);
        Set<Jvm> mockJvms = new HashSet<>();
        mockJvms.add(mockJvm);
        reset(mockResourceService);
        reset(mockGroupService);
        when(mockJvm.getJvmName()).thenReturn("mockJvmName");
        when(mockGroupWithJvms.getJvms()).thenReturn(mockJvms);
        when(mockGroupService.updateGroupAppResourceTemplate(anyString(), anyString(), anyString(), anyString())).thenReturn("new hct.xml content");
        when(mockGroupService.getGroup(anyString())).thenReturn(mockGroupWithJvms);
        when(mockGroupService.getGroupAppResourceTemplateMetaData(anyString(), anyString())).thenReturn("{\"entity\":{\"target\": \"testApp\"}}");
        when(mockApplicationServiceRest.updateResourceTemplate(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(mockResponse);
        when(mockApplicationService.updateResourceTemplate(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("new hct.xml content");

        Response response = groupServiceRest.updateGroupAppResourceTemplate("testGroup", "testAppName", "hct.xml", "new hct.xml context");
        verify(mockGroupService).updateGroupAppResourceTemplate(anyString(), anyString(), anyString(), anyString());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        when(mockGroupService.updateGroupAppResourceTemplate(anyString(), anyString(), anyString(), anyString())).thenThrow(new ResourceTemplateUpdateException("testApp", "hct.xml"));
        response = groupServiceRest.updateGroupAppResourceTemplate("testGroup", "testAppName", "hct.xml", "newer hct.xml content");
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        reset(mockGroupService);
        when(mockGroupService.updateGroupAppResourceTemplate(anyString(), anyString(), anyString(), anyString())).thenReturn("new hct.xml content");
        when(mockGroupService.getGroupAppResourceTemplateMetaData(anyString(), anyString())).thenReturn("{\"entity\":{\"target\": \"testApp\"}}");
        when(mockGroupService.getGroup(anyString())).thenReturn(mockGroupWithJvms);
        when(mockGroupWithJvms.getJvms()).thenReturn(null);
        response = groupServiceRest.updateGroupAppResourceTemplate("testGroup", "testAppName", "hct.xml", "newer hct.xml content");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetStartedAndStoppedWebserversAndJvmsCount() {
        List<Group> groupList = new ArrayList<>();
        Group mockGroup = mock(Group.class);
        when(mockGroup.getName()).thenReturn("test-group-name");
        groupList.add(mockGroup);

        when(mockJvmService.getJvmStartedCount(anyString())).thenReturn(0L);
        when(mockJvmService.getJvmStoppedCount(anyString())).thenReturn(0L);
        when(mockJvmService.getJvmForciblyStoppedCount(anyString())).thenReturn(0L);
        when(mockJvmService.getJvmCount(anyString())).thenReturn(0L);
        when(mockWebServerService.getWebServerStartedCount(anyString())).thenReturn(0L);
        when(mockWebServerService.getWebServerStoppedCount(anyString())).thenReturn(0L);
        when(mockWebServerService.getWebServerCount(anyString())).thenReturn(0L);
        when(mockGroupService.getGroups()).thenReturn(groupList);

        Response response = groupServiceRest.getStartedAndStoppedWebServersAndJvmsCount();
        assertEquals(200, response.getStatus());
        final GroupServerInfo groupServerInfoResponse = (GroupServerInfo) ((ArrayList)(((ApplicationResponse) response.getEntity()).getApplicationResponseContent())).get(0);
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getJvmStartedCount());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getJvmStoppedCount());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getJvmForciblyStoppedCount());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getJvmCount());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getWebServerCount());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getWebServerStartedCount());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getWebServerStoppedCount());
    }

    @Test
    public void testgetStartedAndStoppedWebServersAndJvmsCount() {
        when(mockJvmService.getJvmStartedCount(anyString())).thenReturn(0L);
        when(mockJvmService.getJvmStoppedCount(anyString())).thenReturn(0L);
        when(mockJvmService.getJvmForciblyStoppedCount(anyString())).thenReturn(0L);
        when(mockJvmService.getJvmCount(anyString())).thenReturn(0L);
        when(mockWebServerService.getWebServerStartedCount(anyString())).thenReturn(0L);
        when(mockWebServerService.getWebServerStoppedCount(anyString())).thenReturn(0L);
        when(mockWebServerService.getWebServerCount(anyString())).thenReturn(0L);

        Response response = groupServiceRest.getStartedAndStoppedWebServersAndJvmsCount("test-group-name");
        assertEquals(200, response.getStatus());
        final GroupServerInfo groupServerInfoResponse = (GroupServerInfo) (((ApplicationResponse) response.getEntity()).getApplicationResponseContent());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getJvmStartedCount());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getJvmStoppedCount());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getJvmForciblyStoppedCount());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getJvmCount());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getWebServerCount());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getWebServerStartedCount());
        assertEquals(Long.valueOf(0), groupServerInfoResponse.getWebServerStoppedCount());
    }

    @Configuration
    static class Config {

        private final static ExecutorService EXECUTOR_SERVICE = mock(ExecutorService.class);

        final static JvmService JVM_SERVICE = mock(JvmService.class);

        @Bean
        public GroupServiceRest getGroupServiceRest() {
            return new GroupServiceRestImpl(mockGroupService, mockResourceService, mockGroupControlService, mockGroupJvmControlService,
                    mockGroupWebServerControlService, mockJvmService, mockWebServerService, mockApplicationService,
                    mockApplicationServiceRest, mockWebServerServiceRest);
        }

        @Bean
        public JvmServiceRest getJvmServiceRest() {
            return new JvmServiceRestImpl(mockJvmService, mockJvmControlService);
        }

        @Bean
        WebServerServiceRest getWebServerServiceRest() {
            WebServerServiceRestImpl webServerServiceRest = new WebServerServiceRestImpl(mockWebServerService, mockWebServerControlService, mock(WebServerCommandService.class), mockResourceService, mockGroupService, binaryDistributionService, mockHistoryService);
            webServerServiceRest.setLockManager(new BinaryDistributionLockManagerImpl());
            return webServerServiceRest;
        }

        @Bean
        ApplicationServiceRest getApplicationServiceRest() {
            return new ApplicationServiceRestImpl(mockApplicationService, mock(ResourceService.class), mockGroupService);
        }

        @Bean
        BinaryDistributionLockManager getBinaryDistributionLockManager() {
            return mockBinaryDistributionLockManager;
        }

        @Bean
        public GroupControlService getGroupControlService() {
            return mockGroupControlService;
        }

        @Bean
        public GroupJvmControlService getGroupJvmControlService() {
            return mockGroupJvmControlService;
        }

        @Bean
        public GroupWebServerControlService getGroupWebServerControlService() {
            return mockGroupWebServerControlService;
        }

        @Bean
        public static ExecutorService getExecutorService() {
            return EXECUTOR_SERVICE;
        }

        @Bean
        public static JvmService getJvmService() {
            return JVM_SERVICE;
        }
    }
}
