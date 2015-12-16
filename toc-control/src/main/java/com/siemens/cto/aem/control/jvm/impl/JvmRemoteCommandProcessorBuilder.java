package com.siemens.cto.aem.control.jvm.impl;

import com.jcraft.jsch.JSch;
import com.siemens.cto.aem.commandprocessor.CommandProcessor;
import com.siemens.cto.aem.commandprocessor.CommandProcessorBuilder;
import com.siemens.cto.aem.commandprocessor.impl.jsch.JschCommandProcessorImpl;
import com.siemens.cto.aem.commandprocessor.impl.jsch.JschScpCommandProcessorImpl;
import com.siemens.cto.aem.common.domain.model.ssh.SshConfiguration;
import com.siemens.cto.aem.common.exec.ExecCommand;
import com.siemens.cto.aem.common.exec.RemoteExecCommand;
import com.siemens.cto.aem.common.exec.RemoteSystemConnection;
import com.siemens.cto.aem.exception.CommandFailureException;
import com.siemens.cto.aem.persistence.jpa.domain.JpaJvm;

public class JvmRemoteCommandProcessorBuilder implements CommandProcessorBuilder {

    private ExecCommand command;
    private JpaJvm jvm;
    private JSch jsch;
    private SshConfiguration sshConfig;

    public JvmRemoteCommandProcessorBuilder() {
    }

    public JvmRemoteCommandProcessorBuilder setCommand(final ExecCommand aCommand) {
        command = aCommand;
        return this;
    }

    public JvmRemoteCommandProcessorBuilder setJvm(final JpaJvm jvm) {
        this.jvm = jvm;
        return this;
    }

    public JvmRemoteCommandProcessorBuilder setJsch(final JSch aJsch) {
        jsch = aJsch;
        return this;
    }

    public JvmRemoteCommandProcessorBuilder setSshConfig(final SshConfiguration aConfig) {
        sshConfig = aConfig;
        return this;
    }

    @Override
    public CommandProcessor build() throws CommandFailureException {

        final RemoteExecCommand remoteCommand = new RemoteExecCommand(getRemoteSystemConnection(), command);
        if (command.getCommandFragments().get(0).contains("secure-copy")) {
            return new JschScpCommandProcessorImpl(jsch, remoteCommand);
        } else {
            return new JschCommandProcessorImpl(jsch, remoteCommand);
        }
    }

    protected RemoteSystemConnection getRemoteSystemConnection() {
        final RemoteSystemConnection connection = new RemoteSystemConnection(
                sshConfig.getUserName(),
                sshConfig.getPassword(),
                jvm.getHostName(),
                sshConfig.getPort());
        return connection;
    }
}
