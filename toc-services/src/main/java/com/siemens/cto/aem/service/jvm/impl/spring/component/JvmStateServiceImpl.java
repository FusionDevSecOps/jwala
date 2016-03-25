package com.siemens.cto.aem.service.jvm.impl.spring.component;

import com.siemens.cto.aem.common.domain.model.id.Identifier;
import com.siemens.cto.aem.common.domain.model.jvm.Jvm;
import com.siemens.cto.aem.common.domain.model.jvm.JvmState;
import com.siemens.cto.aem.common.domain.model.state.CurrentState;
import com.siemens.cto.aem.common.domain.model.state.StateType;
import com.siemens.cto.aem.service.MapWrapper;
import com.siemens.cto.aem.service.MessagingService;
import com.siemens.cto.aem.service.group.GroupStateNotificationService;
import com.siemens.cto.aem.service.jvm.JvmService;
import com.siemens.cto.aem.service.jvm.JvmStateService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * {@link JvmStateService} implementation.
 *
 * Created by JC043760 on 3/22/2016.
 */
@Service
public class JvmStateServiceImpl implements JvmStateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JvmStateServiceImpl.class);

    private final JvmService jvmService;
    private final MapWrapper<Identifier<Jvm>, CurrentState<Jvm, JvmState>> stateMapWrapper;
    private final JvmStateResolverWorker jvmStateResolverWorker;
    private final long jvmStateUpdateInterval;
    private final MessagingService messagingService;
    private final GroupStateNotificationService groupStateNotificationService;

    private static final Map<Identifier<Jvm>, Future<CurrentState<Identifier<Jvm>, JvmState>>> PING_FUTURE_MAP = new HashMap<>();

    @Autowired
    public JvmStateServiceImpl(final JvmService jvmService,
                               @Qualifier("jvmStateMapWrapper") final MapWrapper stateMapWrapper,
                               final JvmStateResolverWorker jvmStateResolverWorker,
                               final MessagingService messagingService,
                               final GroupStateNotificationService groupStateNotificationService,
                               @Value("${jvm.state.update.interval:60000}")
                               final long jvmStateUpdateInterval) {
        this.jvmService = jvmService;
        this.stateMapWrapper = stateMapWrapper;
        this.jvmStateResolverWorker = jvmStateResolverWorker;
        this.jvmStateUpdateInterval = jvmStateUpdateInterval;
        this.messagingService = messagingService;
        this.groupStateNotificationService = groupStateNotificationService;
    }

    @Override
    @Scheduled(fixedDelayString = "${ping.jvm.period.millis}")
    public void checkAndUpdateStates() {
        final List<Jvm> jvms = jvmService.getJvms();
        for (final Jvm jvm : jvms) {
            if ((!stateMapWrapper.getMap().containsKey(jvm.getId()) || (isStarted(jvm) && isStale(jvm))) &&
                (!PING_FUTURE_MAP.containsKey(jvm.getId()) || PING_FUTURE_MAP.get(jvm.getId()).isDone())) {
                    LOGGER.debug("Pinging JVM {} ...", jvm.getJvmName());
                    PING_FUTURE_MAP.put(jvm.getId(), jvmStateResolverWorker.pingJvm(jvm, this));
                    LOGGER.debug("Pinged JVM {}", jvm.getJvmName());
            }
        }
    }

    /**
     * Check if the JVM's state is stale by checking the state's time stamp.
     * @param jvm {@link Jvm}
     * @return true if the state is stale.
     */
    protected boolean isStale(final Jvm jvm) {
        final long interval = DateTime.now().getMillis() - stateMapWrapper.getMap().get(jvm.getId()).getAsOf().getMillis();
        if (interval > jvmStateUpdateInterval) {
            LOGGER.debug("JVM {} state is stale. Interval since last update = {} sec!", jvm.getJvmName(), interval/1000);
            return true;
        }
        return false;
    }

    /**
     * Checks if a JVM's state is set to started.
     * @param jvm {@link Jvm}
     * @return true if the state is started.
     */
    protected boolean isStarted(final Jvm jvm) {
        if (stateMapWrapper.getMap().get(jvm.getId()).getState().equals(JvmState.JVM_STARTED)) {
                return true;
        }
        return false;
    }

    @Override
    public void setState(final Jvm jvm, final JvmState state, final String errMsg) {
        // Check again before updating to make sure that nothing has change after pinging the JVM.
        if (!stateMapWrapper.getMap().containsKey(jvm.getId()) || (isStarted(jvm) && isStale(jvm))) {
            LOGGER.debug("Updating state of JVM {} ...", jvm.getJvmName());
            jvmService.updateState(jvm.getId(), state, errMsg);
            final CurrentState currentState = new CurrentState(jvm.getId(), state, DateTime.now(), StateType.JVM, errMsg);
            messagingService.send(currentState);
            stateMapWrapper.getMap().put(jvm.getId(), currentState);
            groupStateNotificationService.retrieveStateAndSendToATopic(jvm.getId(), Jvm.class);
            LOGGER.debug("Updated state of JVM {}!", jvm.getJvmName());
        }
    }

}
