package com.siemens.cto.aem.persistence.service.app;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.siemens.cto.aem.common.exception.BadRequestException;
import com.siemens.cto.aem.domain.model.app.Application;
import com.siemens.cto.aem.domain.model.app.CreateApplicationCommand;
import com.siemens.cto.aem.domain.model.app.RemoveWebArchiveCommand;
import com.siemens.cto.aem.domain.model.app.UpdateApplicationCommand;
import com.siemens.cto.aem.domain.model.app.UploadWebArchiveCommand;
import com.siemens.cto.aem.domain.model.audit.AuditEvent;
import com.siemens.cto.aem.domain.model.event.Event;
import com.siemens.cto.aem.domain.model.group.CreateGroupCommand;
import com.siemens.cto.aem.domain.model.group.Group;
import com.siemens.cto.aem.domain.model.id.Identifier;
import com.siemens.cto.aem.domain.model.temporary.User;
import com.siemens.cto.aem.persistence.service.group.GroupPersistenceService;

import static org.junit.Assert.*;

@Transactional
public abstract class AbstractApplicationPersistenceServiceTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractApplicationPersistenceServiceTest.class); 

    @Autowired
    private ApplicationPersistenceService applicationPersistenceService;

    @Autowired
    private GroupPersistenceService groupPersistenceService;

    private String aUser;
    
    private String alphaLower = "abcdefghijklmnopqrstuvwxyz";
    private String alpha = alphaLower + alphaLower.toUpperCase();
    private String alphaNum = alpha + "0123456789,.-/_$ ";
    private String alphaUnsafe = alphaNum + "\\\t\r\n";
    
    private String textContext = "/" + RandomStringUtils.random(25,alphaUnsafe.toCharArray());
    private String textWarPath = RandomStringUtils.random(25,alphaUnsafe.toCharArray()) + ".war";
    private String textName    = RandomStringUtils.random(25,alphaUnsafe.toCharArray());
    private String textGroup   = RandomStringUtils.random(25,alphaUnsafe.toCharArray());

    private String textUpdatedContext = "/updated" + RandomStringUtils.random(25,alphaUnsafe.toCharArray());
    private String textUpdatedWarPath = RandomStringUtils.random(25,alphaUnsafe.toCharArray()) + "-updated.war";
    private String textUpdatedName    = textName + "-updated";
    private String textUpdatedGroup   = textGroup+ "-updated";

    private Identifier<Group> expGroupId;
    private Identifier<Group> expUpdatedGroupId;
    private Identifier<Application> updateAppId;
    private Identifier<Application> deleteAppId;

    private User userObj;
    
    @Before
    public void setup() {
        aUser = "TestUserId";
        userObj = new User(aUser);
        Group group = groupPersistenceService.createGroup(new Event<CreateGroupCommand>(new CreateGroupCommand(textGroup), AuditEvent.now(userObj)));        
        Group updGroup = groupPersistenceService.createGroup(new Event<CreateGroupCommand>(new CreateGroupCommand(textUpdatedGroup), AuditEvent.now(userObj)));        
        expGroupId = group.getId();
        expUpdatedGroupId = updGroup.getId();
        
        deleteAppId = null;
        updateAppId = null;
    }
    
    @After
    public void tearDown() {
        if(updateAppId != null) { 
            try { applicationPersistenceService.removeApplication(updateAppId); } catch (Exception x) { LOGGER.trace("Test tearDown", x); }
        }
        try { groupPersistenceService.removeGroup(expUpdatedGroupId); } catch (Exception x) { LOGGER.trace("Test tearDown", x); }
        try { groupPersistenceService.removeGroup(expGroupId); } catch (Exception x) { LOGGER.trace("Test tearDown", x); }
    }
    
    @Test
    public void testCreateApp() {
        CreateApplicationCommand cmd = new CreateApplicationCommand(expGroupId,  textName, textContext, true, true);
        Event<CreateApplicationCommand> anAppToCreate = new Event<>(cmd, AuditEvent.now(new User(aUser)));
        Application created = applicationPersistenceService.createApplication(anAppToCreate);
        assertNotNull(created.getGroup());
        assertEquals(expGroupId, created.getGroup().getId());
        assertEquals(textName, created.getName());
        assertEquals(textContext, created.getWebAppContext());
        assertTrue(created.isSecure());
        updateAppId = created.getId(); 
        deleteAppId = created.getId();
    }

    @Test
    public void testCreateNonSecureApp() {
        CreateApplicationCommand cmd = new CreateApplicationCommand(expGroupId,  textName, textContext, false, true);
        Event<CreateApplicationCommand> anAppToCreate = new Event<>(cmd, AuditEvent.now(new User(aUser)));
        Application created = applicationPersistenceService.createApplication(anAppToCreate);
        assertNotNull(created.getGroup());
        assertEquals(expGroupId, created.getGroup().getId());
        assertEquals(textName, created.getName());
        assertEquals(textContext, created.getWebAppContext());
        assertTrue(!created.isSecure());
        updateAppId = created.getId();
        deleteAppId = created.getId();
    }
    
    @Test
    public void testUpdateApp() {
        if(updateAppId == null) {
            testCreateApp();
        }
        
        UpdateApplicationCommand cmd = new UpdateApplicationCommand(updateAppId, expUpdatedGroupId,  textUpdatedContext, textUpdatedName, true, true);
        Event<UpdateApplicationCommand> anAppToCreate = new Event<>(cmd, AuditEvent.now(new User(aUser)));
        Application created = applicationPersistenceService.updateApplication(anAppToCreate);
        assertEquals(updateAppId, created.getId());
        assertNotNull(created.getGroup());
        assertEquals(expUpdatedGroupId, created.getGroup().getId());
        assertEquals(textUpdatedName, created.getName());
        assertEquals(textUpdatedContext, created.getWebAppContext());

    }
    
    @Test
    public void testRemoveApp() {
        testCreateApp();
        
        applicationPersistenceService.removeApplication(deleteAppId);
    }

    @Test(expected = BadRequestException.class)
    public void testRemoveAppAndFailUpdate() {
        testRemoveApp();
        testUpdateApp();
    }    
    
    @Test
    public void testUpdateWARPath() { 
        CreateApplicationCommand cmd = new CreateApplicationCommand(expGroupId,  textName, textContext, true, true);
        Event<CreateApplicationCommand> anAppToCreate = new Event<>(cmd, AuditEvent.now(new User(aUser)));
        Application created = applicationPersistenceService.createApplication(anAppToCreate);
        
        UploadWebArchiveCommand uploadCmd = new UploadWebArchiveCommand(created, "filename-uuid.war", 0L, null);
        Event<UploadWebArchiveCommand> uploadEvent = new Event<>(uploadCmd, AuditEvent.now(new User(aUser)));

        Application uploaded = applicationPersistenceService.updateWARPath(uploadEvent, "D:\\APACHE\\TOMCAT\\WEBAPPS\\filename-uuid.war");
        assertEquals(uploaded.getWarPath(), "D:\\APACHE\\TOMCAT\\WEBAPPS\\filename-uuid.war");                
    }
    
    @Test
    public void testRemoveWARPath() {        
        CreateApplicationCommand cmd = new CreateApplicationCommand(expGroupId,  textName, textContext, true, true);
        Event<CreateApplicationCommand> anAppToCreate = new Event<>(cmd, AuditEvent.now(new User(aUser)));
        Application created = applicationPersistenceService.createApplication(anAppToCreate);
        
        UploadWebArchiveCommand uploadCmd = new UploadWebArchiveCommand(created, "filename-uuid.war", 0L, null);
        Event<UploadWebArchiveCommand> uploadEvent = new Event<>(uploadCmd, AuditEvent.now(new User(aUser)));

        Application uploaded = applicationPersistenceService.updateWARPath(uploadEvent, "D:\\APACHE\\TOMCAT\\WEBAPPS\\filename-uuid.war");
        assertEquals(uploaded.getWarPath(), "D:\\APACHE\\TOMCAT\\WEBAPPS\\filename-uuid.war");                        

        RemoveWebArchiveCommand removeCmd = new RemoveWebArchiveCommand(created);
        Event<RemoveWebArchiveCommand> removeEvent = new Event<>(removeCmd, AuditEvent.now(new User(aUser)));
        
        Application noWarApp = applicationPersistenceService.removeWARPath(removeEvent);
        assertNull(noWarApp.getWarPath());
    }

    @Test
    public void testUpdateSecureFlag() {
        CreateApplicationCommand cmd = new CreateApplicationCommand(expGroupId,  textName, textContext, true, true);
        Event<CreateApplicationCommand> anAppToCreate = new Event<>(cmd, AuditEvent.now(new User(aUser)));
        Application created = applicationPersistenceService.createApplication(anAppToCreate);
        assertTrue(created.isSecure());

        final Event<UpdateApplicationCommand> updateEvent =
                new Event<>(new UpdateApplicationCommand(created.getId(),
                                                         created.getGroup().getId(),
                                                         created.getWebAppContext(),
                                                         created.getName(), false, true), AuditEvent.now(new User(aUser)));
        Application updatedApplication = applicationPersistenceService.updateApplication(updateEvent);
        assertTrue(!updatedApplication.isSecure());
    }
}