package com.siemens.cto.aem.ws.rest.v1.service.group.impl;

import com.siemens.cto.aem.common.configuration.TestExecutionProfile;
import com.siemens.cto.aem.common.domain.model.app.Application;
import com.siemens.cto.aem.common.domain.model.fault.AemFaultType;
import com.siemens.cto.aem.common.domain.model.group.Group;
import com.siemens.cto.aem.common.domain.model.group.GroupState;
import com.siemens.cto.aem.common.domain.model.id.Identifier;
import com.siemens.cto.aem.common.domain.model.jvm.Jvm;
import com.siemens.cto.aem.common.domain.model.resource.ResourceType;
import com.siemens.cto.aem.common.domain.model.user.User;
import com.siemens.cto.aem.common.domain.model.webserver.WebServer;
import com.siemens.cto.aem.common.exception.InternalErrorException;
import com.siemens.cto.aem.common.exec.CommandOutput;
import com.siemens.cto.aem.common.exec.ExecReturnCode;
import com.siemens.cto.aem.common.properties.ApplicationProperties;
import com.siemens.cto.aem.common.request.group.CreateGroupRequest;
import com.siemens.cto.aem.common.request.jvm.ControlJvmRequest;
import com.siemens.cto.aem.exception.CommandFailureException;
import com.siemens.cto.aem.service.app.ApplicationService;
import com.siemens.cto.aem.service.group.GroupControlService;
import com.siemens.cto.aem.service.group.GroupJvmControlService;
import com.siemens.cto.aem.service.group.GroupService;
import com.siemens.cto.aem.service.group.GroupWebServerControlService;
import com.siemens.cto.aem.service.jvm.JvmControlService;
import com.siemens.cto.aem.service.jvm.JvmService;
import com.siemens.cto.aem.service.resource.ResourceService;
import com.siemens.cto.aem.service.spring.component.GrpStateComputationAndNotificationSvc;
import com.siemens.cto.aem.service.state.StateService;
import com.siemens.cto.aem.service.webserver.WebServerCommandService;
import com.siemens.cto.aem.service.webserver.WebServerControlService;
import com.siemens.cto.aem.service.webserver.WebServerService;
import com.siemens.cto.aem.ws.rest.v1.provider.AuthenticatedUser;
import com.siemens.cto.aem.ws.rest.v1.service.app.ApplicationServiceRest;
import com.siemens.cto.aem.ws.rest.v1.service.app.impl.ApplicationServiceRestImpl;
import com.siemens.cto.aem.ws.rest.v1.service.group.GroupServiceRest;
import com.siemens.cto.aem.ws.rest.v1.service.jvm.JvmServiceRest;
import com.siemens.cto.aem.ws.rest.v1.service.jvm.impl.JvmServiceRestImpl;
import com.siemens.cto.aem.ws.rest.v1.service.webserver.WebServerServiceRest;
import com.siemens.cto.aem.ws.rest.v1.service.webserver.impl.WebServerServiceRestImpl;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@IfProfileValue(name = TestExecutionProfile.RUN_TEST_TYPES, value = TestExecutionProfile.INTEGRATION)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class,
        classes = {GroupServiceImplDeployTest.Config.class
        })
public class GroupServiceImplDeployTest {

    @Autowired
    GroupServiceRest groupServiceRest;

    @Autowired
    JvmServiceRest jvmServiceRest;

    @Autowired
    WebServerServiceRest webServerServiceRest;

    @Autowired
    ApplicationServiceRest applicationServiceRest;

    static final GroupService mockGroupService = mock(GroupService.class);
    static final ResourceService mockResourceService = mock(ResourceService.class);
    static final GroupControlService mockGroupControlService = mock(GroupControlService.class);
    static final GroupJvmControlService mockGroupJvmControlService = mock(GroupJvmControlService.class);
    static final GroupWebServerControlService mockGroupWebServerControlService = mock(GroupWebServerControlService.class);
    static final StateService<Group, GroupState> mockStateService = mock(StateService.class);
    static final JvmService mockJvmService = mock(JvmService.class);
    static final JvmControlService mockJvmControlService = mock(JvmControlService.class);
    static final WebServerService mockWebServerService = mock(WebServerService.class);
    static final WebServerControlService mockWebServerControlService = mock(WebServerControlService.class);
    static final ApplicationService mockApplicationService = mock(ApplicationService.class);
    static final GrpStateComputationAndNotificationSvc mockGrpStateComputationAndNotificationSvc = mock(GrpStateComputationAndNotificationSvc.class);

    private AuthenticatedUser mockAuthUser = mock(AuthenticatedUser.class);
    private User mockUser = mock(User.class);
    private String httpdConfDirPath;

    @Before
    public void setUp() {
        when(mockAuthUser.getUser()).thenReturn(mockUser);

        System.setProperty(ApplicationProperties.PROPERTIES_ROOT_PATH, "./src/test/resources");
        httpdConfDirPath = ApplicationProperties.get("paths.httpd.conf");
        assertTrue(new File(httpdConfDirPath).mkdirs());
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(new File(httpdConfDirPath));
        System.clearProperty(ApplicationProperties.PROPERTIES_ROOT_PATH);
    }

    @Test
    public void testCreateGroup() {
        Group mockGroup = mock(Group.class);
        when(mockGroupService.createGroup(any(CreateGroupRequest.class), any(User.class))).thenReturn(mockGroup);
        when(mockResourceService.getResourceTypes()).thenReturn(new ArrayList<ResourceType>());

        groupServiceRest.createGroup("testGroup", mockAuthUser);

        verify(mockGroupService, times(1)).createGroup(any(CreateGroupRequest.class), any(User.class));
        verify(mockResourceService, times(2)).getResourceTypes();
    }

    @Test
    public void testGroupJvmDeploy() throws CommandFailureException {
        Group mockGroup = mock(Group.class);
        Jvm mockJvm = mock(Jvm.class);
        Response mockResponse = mock(Response.class);
        ResourceType mockResourceType = mock(ResourceType.class);

        Set<Jvm> jvmSet = new HashSet<>();
        jvmSet.add(mockJvm);
        Collection<ResourceType> resourcesList = new ArrayList<>();
        resourcesList.add(mockResourceType);

        when(mockGroup.getJvms()).thenReturn(jvmSet);
        when(mockJvm.getJvmName()).thenReturn("testJvm");
        when(mockJvm.getId()).thenReturn(new Identifier<Jvm>(99L));
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockResourceType.getRelativeDir()).thenReturn("./");
        when(mockResourceType.getEntityType()).thenReturn("jvm");
        when(mockResourceType.getConfigFileName()).thenReturn("server.xml");
        when(mockGroupService.getGroup(anyString())).thenReturn(mockGroup);
        when(mockGroupService.getGroupJvmResourceTemplate(anyString(),anyString(),anyBoolean())).thenReturn("new server.xml content");
        when(mockJvmService.updateResourceTemplate(anyString(), anyString(), anyString())).thenReturn("new server.xml content");
        when(mockJvmService.getJvm(anyString())).thenReturn(mockJvm);
        when(mockJvmService.isJvmStarted(any(Jvm.class))).thenReturn(false);
        when(mockJvmService.generateConfigFile(anyString(), anyString())).thenReturn("new server.xml content");
        when(mockResourceService.getResourceTypes()).thenReturn(resourcesList);
        when(mockJvmControlService.secureCopyFileWithBackup(any(ControlJvmRequest.class), anyString(), anyString())).thenReturn(new CommandOutput(new ExecReturnCode(0), "SUCCESS", ""));

        Response returnedResponse = groupServiceRest.generateAndDeployGroupJvmFile("testGroup", "server.xml", mockAuthUser);
        assertEquals(200, returnedResponse.getStatusInfo().getStatusCode());

        try {
            FileUtils.forceDelete(new File("./testJvm"));
        } catch (IOException e) {
            assertTrue("This should not fail :: " + e, false);
        }
    }

    @Test
    public void testGroupWebServerDeploy() throws CommandFailureException {
        Group mockGroup = mock(Group.class);
        WebServer mockWebServer = mock(WebServer.class);
        Response mockResponse = mock(Response.class);
        ResourceType mockResourceType = mock(ResourceType.class);

        Set<WebServer> webServerSet = new HashSet<>();
        webServerSet.add(mockWebServer);
        Collection<ResourceType> resourcesList = new ArrayList<>();
        resourcesList.add(mockResourceType);

        when(mockGroup.getWebServers()).thenReturn(webServerSet);
        when(mockWebServer.getName()).thenReturn("testWebServer");
        when(mockWebServer.getId()).thenReturn(new Identifier<WebServer>(99L));
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockResourceType.getRelativeDir()).thenReturn("./");
        when(mockResourceType.getEntityType()).thenReturn("webServer");
        when(mockResourceType.getConfigFileName()).thenReturn("httpd.conf");
        when(mockGroupService.getGroup(anyString())).thenReturn(mockGroup);
        when(mockGroupService.getGroupWithWebServers(any(Identifier.class))).thenReturn(mockGroup);
        when(mockGroupService.getGroupWebServerResourceTemplate(anyString(), anyString(), anyBoolean())).thenReturn("new httpd.conf context");
        when(mockResourceService.getResourceTypes()).thenReturn(resourcesList);
        when(mockWebServerService.updateResourceTemplate(anyString(), anyString(), anyString())).thenReturn("new httpd.conf context");
        when(mockWebServerService.generateHttpdConfig(anyString(),anyBoolean())).thenReturn("new httpd.conf context");
        when(mockWebServerControlService.secureCopyHttpdConf(anyString(), anyString(), anyString())).thenReturn(new CommandOutput(new ExecReturnCode(0), "SUCCESS", ""));

        Response returnedResponse = groupServiceRest.generateAndDeployGroupWebServersFile("testGroup", mockAuthUser);
        assertEquals(200, returnedResponse.getStatusInfo().getStatusCode());

        when(mockWebServerControlService.secureCopyHttpdConf(anyString(), anyString(), anyString())).thenReturn(new CommandOutput(new ExecReturnCode(1), "", "NOT OK"));
        try{
            groupServiceRest.generateAndDeployGroupWebServersFile("testGroup", mockAuthUser);
        } catch (InternalErrorException ie){
            assertEquals(AemFaultType.REMOTE_COMMAND_FAILURE, ie.getMessageResponseStatus());
        }
    }

    @Test
    public void testGroupAppDeploy() throws CommandFailureException {
        Group mockGroup = mock(Group.class);
        Jvm mockJvm = mock(Jvm.class);
        Application mockApp = mock(Application.class);
        Response mockResponse = mock(Response.class);
        ResourceType mockResourceType = mock(ResourceType.class);

        Set<Jvm> jvmSet = new HashSet<>();
        jvmSet.add(mockJvm);
        Set<Application> appSet = new HashSet<>();
        appSet.add(mockApp);
        Collection<ResourceType> resourcesList = new ArrayList<>();
        resourcesList.add(mockResourceType);

        when(mockGroup.getJvms()).thenReturn(jvmSet);
        when(mockJvm.getJvmName()).thenReturn("testJvm");
        when(mockJvm.getId()).thenReturn(new Identifier<Jvm>(99L));
        when(mockResponse.getStatus()).thenReturn(200);
        when(mockResourceType.getRelativeDir()).thenReturn("./");
        when(mockResourceType.getEntityType()).thenReturn("webApp");
        when(mockResourceType.getConfigFileName()).thenReturn("hct.xml");
        when(mockGroupService.getGroup(anyString())).thenReturn(mockGroup);
        when(mockGroupService.getGroupAppResourceTemplate(anyString(), anyString(), anyBoolean())).thenReturn("new hct.xml content");
        when(mockGroupService.getAppNameFromResourceTemplate(anyString())).thenReturn("testApp");
        when(mockJvmService.getJvm(anyString())).thenReturn(mockJvm);
        when(mockJvmService.isJvmStarted(any(Jvm.class))).thenReturn(false);
        when(mockResourceService.getResourceTypes()).thenReturn(resourcesList);
        when(mockApplicationService.updateResourceTemplate(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn("new hct.xml content");
        when(mockApplicationService.deployConf(anyString(), anyString(), anyString(), anyString(), anyBoolean(), any(User.class))).thenReturn(new CommandOutput(new ExecReturnCode(0), "SUCCESS", ""));
        Response returnResponse = groupServiceRest.generateAndDeployGroupAppFile("testGroup", "hct.xml", mockAuthUser);
        assertEquals(200, returnResponse.getStatus());

        when(mockApplicationService.deployConf(anyString(), anyString(), anyString(), anyString(), anyBoolean(), any(User.class))).thenReturn(new CommandOutput(new ExecReturnCode(1), "", "NOT OK"));
        try{
            groupServiceRest.generateAndDeployGroupAppFile("testGroup", "hct.xml", mockAuthUser);
        } catch (InternalErrorException ie){
            assertEquals(AemFaultType.REMOTE_COMMAND_FAILURE, ie.getMessageResponseStatus());
        }
    }

    @Configuration
    static class Config {

        @Bean
        public GroupServiceRest getGroupServiceRest() {
            return new GroupServiceRestImpl(mockGroupService, mockResourceService);
        }

        @Bean
        public JvmServiceRest getJvmServiceRest() {
            return new JvmServiceRestImpl(mockJvmService, mockJvmControlService, mock(StateService.class), mockResourceService, mock(ExecutorService.class), new HashMap<String, ReentrantReadWriteLock>(), mockGrpStateComputationAndNotificationSvc);
        }

        @Bean
        WebServerServiceRest getWebServerServiceRest() {
            return new WebServerServiceRestImpl(mockWebServerService, mockWebServerControlService, mock(WebServerCommandService.class), mock(StateService.class), new HashMap<String, ReentrantReadWriteLock>(), mockResourceService);
        }

        @Bean
        ApplicationServiceRest getApplicationServiceRest(){
            return new ApplicationServiceRestImpl(mockApplicationService);
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

        @Bean(name = "groupStateService")
        public StateService<Group, GroupState> getStateService() {
            return mockStateService;
        }


    }
}
