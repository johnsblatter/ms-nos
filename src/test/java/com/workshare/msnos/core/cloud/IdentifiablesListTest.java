package com.workshare.msnos.core.cloud;

import static com.workshare.msnos.core.CoreHelper.createMockAgent;
import static com.workshare.msnos.core.CoreHelper.newAgentIden;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;

import com.workshare.msnos.core.Agent;
import com.workshare.msnos.core.cloud.IdentifiablesList.Callback;

@SuppressWarnings("unchecked")
public class IdentifiablesListTest {

    private IdentifiablesList<Agent> identifiables;

    private Callback<Agent> callback;
    
    @Before
    public void setup() {
        callback = mock(Callback.class);
        identifiables = new IdentifiablesList<Agent>(callback);
    }

    @Test
    public void shouldInvokeCallbackOnAdd() {
        Agent agent = createMockAgent();
        
        identifiables.add(agent);
        
        verify(callback).onAdd(agent);
    }
    
    @Test
    public void shouldInvokeCallbackOnRemove() {
        Agent agent = createMockAgent();
        identifiables.add(agent);
        
        identifiables.remove(agent.getIden());

        verify(callback).onRemove(agent);
    }
    
    @Test
    public void shouldNOTInvokeCallbackOnRemoveIfIdentifiableNotPresent() {
        identifiables.remove(newAgentIden());
        verifyZeroInteractions(callback);
    }
}
