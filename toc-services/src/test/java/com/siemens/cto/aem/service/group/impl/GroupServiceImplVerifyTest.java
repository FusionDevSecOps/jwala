package com.siemens.cto.aem.service.group.impl;

import com.siemens.cto.aem.common.exception.BadRequestException;
import com.siemens.cto.aem.common.request.group.*;
import com.siemens.cto.aem.common.domain.model.group.*;
import com.siemens.cto.aem.common.domain.model.id.Identifier;
import com.siemens.cto.aem.common.domain.model.jvm.Jvm;
import com.siemens.cto.aem.common.domain.model.user.User;
import com.siemens.cto.aem.common.domain.model.webserver.WebServer;
import com.siemens.cto.aem.persistence.service.GroupPersistenceService;
import com.siemens.cto.aem.service.VerificationBehaviorSupport;
import com.siemens.cto.aem.service.state.GroupStateService;
import com.siemens.cto.aem.service.state.StateNotificationWorker;
import com.siemens.cto.aem.service.webserver.WebServerService;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class GroupServiceImplVerifyTest extends VerificationBehaviorSupport {

    private GroupServiceImpl groupService;
    private GroupPersistenceService groupPersistenceService;
    private GroupStateService.API groupStateService;
    private StateNotificationWorker stateNotificationWorker;
    private WebServerService webServerService;
    private User user;

    @Before
    public void setUp() {
        groupPersistenceService = mock(GroupPersistenceService.class);
        stateNotificationWorker = mock(StateNotificationWorker.class);
        groupStateService = mock(GroupStateService.API.class);
        stateNotificationWorker = mock(StateNotificationWorker.class);
        webServerService = mock(WebServerService.class);

        groupService = new GroupServiceImpl(groupPersistenceService,
                                    webServerService,
                                    groupStateService,
                                    stateNotificationWorker);
        user = new User("unused");
    }

    @Test
    public void testCreateGroup() {

        final CreateGroupRequest command = mock(CreateGroupRequest.class);

        groupService.createGroup(command,
                user);

        verify(command, times(1)).validate();
        verify(groupPersistenceService, times(1)).createGroup(command);
    }

    @Test
    public void testGetGroup() {

        final Identifier<Group> id = new Identifier<>(-123456L);

        groupService.getGroup(id);

        verify(groupPersistenceService, times(1)).getGroup(eq(id));
    }

    @Test
    public void testGetGroups() {

        groupService.getGroups();

        verify(groupPersistenceService, times(1)).getGroups();
    }

    @Test
    public void testFindGroups() {

        final String fragment = "unused";

        groupService.findGroups(fragment);

        verify(groupPersistenceService, times(1)).findGroups(eq(fragment));
    }

    @Test(expected = BadRequestException.class)
    public void testFindGroupsWithBadName() {

        final String badFragment = "";

        groupService.findGroups(badFragment);
    }

    @Test
    public void testUpdateGroup() throws InterruptedException {
        final UpdateGroupRequest updateGroupRequest = mock(UpdateGroupRequest.class);
        groupService.updateGroup(updateGroupRequest, user);

        verify(updateGroupRequest).validate();
        verify(groupPersistenceService).updateGroup(updateGroupRequest);

        // TODO: Remove if this is no londer needed.
        // verify(stateNotificationWorker).refreshState(eq(groupStateService), any(Group.class));
    }

    @Test
    public void testRemoveGroup() {

        final Identifier<Group> id = new Identifier<>(-123456L);

        groupService.removeGroup(id);

        verify(groupPersistenceService, times(1)).removeGroup(eq(id));
    }

    @Test
    public void testAddJvmToGroup() {

        final AddJvmToGroupRequest addJvmToGroupRequest = mock(AddJvmToGroupRequest.class);

        groupService.addJvmToGroup(addJvmToGroupRequest, user);

        verify(addJvmToGroupRequest, times(1)).validate();
        verify(groupPersistenceService, times(1)).addJvmToGroup(addJvmToGroupRequest);

        // TODO: Remove if this is no londer needed.
        // verify(stateNotificationWorker).refreshState(eq(groupStateService), any(Group.class));
    }

    @Test
    public void testAddJvmsToGroup() {

        final AddJvmsToGroupRequest addJvmsToGroupRequest = mock(AddJvmsToGroupRequest.class);

        final Set<AddJvmToGroupRequest> addJvmToGroupRequests = createMockedAddRequests(5);
        when(addJvmsToGroupRequest.toRequests()).thenReturn(addJvmToGroupRequests);

        groupService.addJvmsToGroup(addJvmsToGroupRequest, user);

        verify(addJvmsToGroupRequest, times(1)).validate();
        for (final AddJvmToGroupRequest addJvmToGroupRequest: addJvmToGroupRequests) {
            verify(addJvmToGroupRequest, times(1)).validate();
            verify(groupPersistenceService, times(1)).addJvmToGroup(addJvmToGroupRequest);
        }
    }

    @Test
    public void testRemoveJvmFromGroup() {

        final RemoveJvmFromGroupRequest removeJvmFromGroupRequest = mock(RemoveJvmFromGroupRequest.class);

        groupService.removeJvmFromGroup(removeJvmFromGroupRequest, user);

        verify(removeJvmFromGroupRequest, times(1)).validate();
        verify(groupPersistenceService, times(1)).removeJvmFromGroup(removeJvmFromGroupRequest);

        // TODO: Remove if this is no londer needed.
        // verify(stateNotificationWorker).refreshState(eq(groupStateService), any(Group.class));
    }

    @Test
    public void testGetOtherGroupingDetailsOfJvms() {
        final Set<Group> groupSet = new HashSet<>();
        groupSet.add(new Group(new Identifier<Group>("1"), "Group1"));
        groupSet.add(new Group(new Identifier<Group>("2"), "Group2"));
        groupSet.add(new Group(new Identifier<Group>("3"), "Group3"));

        final Set<Jvm> jvmSet = new HashSet<>();
        jvmSet.add(new Jvm(new Identifier<Jvm>("1"), "Jvm1", groupSet));

        final Group group = new Group(new Identifier<Group>("1"), "Group1" , jvmSet);

        when(groupPersistenceService.getGroup(any(Identifier.class), eq(false))).thenReturn(group);

        final List<Jvm> otherGroupingDetailsOfJvm = groupService.getOtherGroupingDetailsOfJvms(new Identifier<Group>("1"));

        assertTrue(otherGroupingDetailsOfJvm.size() == 1);
        assertEquals(otherGroupingDetailsOfJvm.get(0).getGroups().size(), 2);

        String groupNames = "";
        for (Group grp: otherGroupingDetailsOfJvm.get(0).getGroups()) {
            groupNames += grp.getName();
        }

        assertTrue("Group3Group2".equalsIgnoreCase(groupNames) || "Group2Group3".equalsIgnoreCase(groupNames));
    }

    @Test
    public void testGetOtherGroupingDetailsOfWebServers() {
        final List<Group> groupSet = new ArrayList<>();
        groupSet.add(new Group(new Identifier<Group>("2"), "Group2"));
        groupSet.add(new Group(new Identifier<Group>("3"), "Group3"));

        final Set<WebServer> webServerSet = new HashSet<>();
        webServerSet.add(new WebServer(new Identifier<WebServer>("1"),
                                       groupSet,
                                       "WebServer1",
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null,
                                       null));

        groupSet.add(new Group(new Identifier<Group>("1"), "Group1", new HashSet<Jvm>(), webServerSet, null, null));

        when(groupPersistenceService.getGroup(any(Identifier.class), eq(true))).thenReturn(groupSet.get(2));

        final List<WebServer> otherGroupingDetailsOfWebServer =
                groupService.getOtherGroupingDetailsOfWebServers(new Identifier<Group>("1"));
        assertTrue(otherGroupingDetailsOfWebServer.size() == 1);

        String groupNames = "";
        for (Group grp: otherGroupingDetailsOfWebServer.get(0).getGroups()) {
            groupNames += grp.getName();
        }

        assertTrue("Group3Group2".equalsIgnoreCase(groupNames) || "Group2Group3".equalsIgnoreCase(groupNames));
    }

}
