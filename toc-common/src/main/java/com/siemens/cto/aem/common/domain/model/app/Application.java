package com.siemens.cto.aem.common.domain.model.app;

import com.siemens.cto.aem.common.domain.model.group.Group;
import com.siemens.cto.aem.common.domain.model.id.Identifier;
import com.siemens.cto.aem.common.domain.model.jvm.Jvm;

import java.util.List;

public class Application {
    
    private Identifier<Application> id;
    
    private Group group;
    
    private String webAppContext;

    private String name;

    private String warPath;

    private boolean secure;

    private boolean loadBalanceAcrossServers;

    private boolean unpackWar;

    private String warName;

    private List<Jvm> jvms;

    private Jvm parentJvm;

    public Application(Identifier<Application> anId,
                       String aName,
                       String aWarPath,
                       String aWebAppContext,
                       Group aGroup,
                       boolean secure,
                       boolean loadBalanceAcrossServers,
                       boolean unpackWar, String warName) {
        group = aGroup;
        id = anId;
        webAppContext = aWebAppContext;
        warPath = aWarPath;
        name = aName;
        this.secure = secure;
        this.loadBalanceAcrossServers = loadBalanceAcrossServers;
        this.unpackWar = unpackWar;
        this.warName = warName;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getWebAppContext() {
        return webAppContext;
    }

    public void setWebAppContext(String webAppContext) {
        this.webAppContext = webAppContext;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWarPath() {
        return warPath;
    }

    public void setWarPath(String warPath) {
        this.warPath = warPath;
    }

    public Identifier<Application> getId() {
        return id;
    }

    public void setId(Identifier<Application> id) {
        this.id = id;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isLoadBalanceAcrossServers() {
        return loadBalanceAcrossServers;
    }

    public void setLoadBalanceAcrossServers(boolean loadBalanceAcrossServers) {
        this.loadBalanceAcrossServers = loadBalanceAcrossServers;
    }

    public String getWarName() {
        return warName;
    }

    public void setWarName(String warName) {
        this.warName = warName;
    }

    public boolean isUnpackWar() {
        return unpackWar;
    }

    public void setUnpackWar(boolean unpackWar) {
        this.unpackWar = unpackWar;
    }

    public List<Jvm> getJvms() {
        return jvms;
    }

    public void setJvms(List<Jvm> jvms) {
        this.jvms = jvms;
    }

    public Jvm getParentJvm() {
        return parentJvm;
    }

    public void setParentJvm(Jvm parentJvm) {
        this.parentJvm = parentJvm;
    }
}
