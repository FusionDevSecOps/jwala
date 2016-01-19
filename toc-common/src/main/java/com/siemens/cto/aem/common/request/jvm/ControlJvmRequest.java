package com.siemens.cto.aem.common.request.jvm;

import com.siemens.cto.aem.common.exception.BadRequestException;
import com.siemens.cto.aem.common.request.Request;
import com.siemens.cto.aem.common.domain.model.id.Identifier;
import com.siemens.cto.aem.common.domain.model.jvm.Jvm;
import com.siemens.cto.aem.common.domain.model.jvm.JvmControlOperation;
import com.siemens.cto.aem.common.rule.jvm.JvmIdRule;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class ControlJvmRequest implements Serializable, Request {

    private static final long serialVersionUID = 1L;

    private final Identifier<Jvm> jvmId;
    private final JvmControlOperation controlOperation;

    public ControlJvmRequest(final Identifier<Jvm> theId,
                             final JvmControlOperation theControlOperation) {
        jvmId = theId;
        controlOperation = theControlOperation;
    }

    public Identifier<Jvm> getJvmId() {
        return jvmId;
    }

    public JvmControlOperation getControlOperation() {
        return controlOperation;
    }

    @Override
    public void validate() {
        new JvmIdRule(jvmId).validate();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        ControlJvmRequest rhs = (ControlJvmRequest) obj;
        return new EqualsBuilder()
                .append(this.jvmId, rhs.jvmId)
                .append(this.controlOperation, rhs.controlOperation)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(jvmId)
                .append(controlOperation)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("jvmId", jvmId)
                .append("controlOperation", controlOperation)
                .toString();
    }
}
