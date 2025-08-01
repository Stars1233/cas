package org.apereo.cas.web.flow.actions;

import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.util.MockRequestContext;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.support.WebUtils;
import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.FlowVariable;
import org.springframework.webflow.engine.VariableValueFactory;
import org.springframework.webflow.execution.Event;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link ClearWebflowCredentialActionTests}.
 *
 * @author Misagh Moayyed
 * @since 6.2.0
 */
@Tag("WebflowActions")
class ClearWebflowCredentialActionTests {

    @Test
    void verifyOperation() throws Throwable {
        verifyAction(CasWebflowConstants.TRANSITION_ID_AUTHENTICATION_FAILURE);
        verifyAction(CasWebflowConstants.TRANSITION_ID_ERROR);
    }
    
    private void verifyAction(final String currentEvent) throws Exception {
        val context = MockRequestContext.create();

        val action = new ClearWebflowCredentialAction();
        context.setCurrentEvent(null);
        assertNull(action.execute(context));

        context.setCurrentEvent(new Event(this, CasWebflowConstants.TRANSITION_ID_SUCCESS));
        assertNull(action.execute(context));

        WebUtils.putCredential(context, CoreAuthenticationTestUtils.getCredentialsWithSameUsernameAndPassword());
        val flow = (Flow) context.getActiveFlow();
        val factory = mock(VariableValueFactory.class);
        when(factory.createInitialValue(any())).thenReturn(CoreAuthenticationTestUtils.getCredentialsWithSameUsernameAndPassword());
        val variable = new FlowVariable(CasWebflowConstants.VAR_ID_CREDENTIAL, factory);
        flow.addVariable(variable);
        context.setCurrentEvent(new Event(this, currentEvent));
        assertNull(action.execute(context));
        assertNotNull(WebUtils.getCredential(context));
        assertNotNull(context.getConversationScope().get(Credential.class.getName()));
    }
}
