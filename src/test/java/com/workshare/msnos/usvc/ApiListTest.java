package com.workshare.msnos.usvc;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class ApiListTest {

    ApiList apiList;

    @Before
    public void preparation() {
        apiList = new ApiList();
    }

    @Test
    public void shouldStoreOneApi() {
        RestApi rest = newRestApi("alfa");
        RemoteMicroservice remote = getRemoteMicroservice();

        apiList.add(remote, rest);

        assertEquals(rest, apiList.get());
        assertEquals(rest, apiList.get());
    }

    @Test
    public void shouldSelectApisUsingRoundRobin() {
        RestApi alfa = newRestApi("alfa");
        RestApi beta = newRestApi("beta");
        RemoteMicroservice remote = getRemoteMicroservice();

        apiList.add(remote, alfa);
        apiList.add(remote, beta);

        assertEquals(alfa, apiList.get());
        assertEquals(beta, apiList.get());
        assertEquals(alfa, apiList.get());
    }

    @Test
    public void shouldHonourAffinity() {
        RestApi alfa = newRestApi("alfa");
        RestApi beta = newRestApiWithAffinity("beta");
        RemoteMicroservice remote = getRemoteMicroservice();

        apiList.add(remote, alfa);
        apiList.add(remote, beta);

        assertEquals(alfa, apiList.get());
        assertEquals(beta, apiList.get());
        assertEquals(beta, apiList.get());
    }

    @Test
    public void shouldRemoveAffinityWhenFaulty() {
        RestApi alfa = newRestApi("alfa");
        RestApi beta = newRestApiWithAffinity("beta");
        RestApi thre = newRestApi("thre");
        RemoteMicroservice remoteAlfa = getRemoteMicroservice();
        RemoteMicroservice remoteBeta = getRemoteMicroservice();
        RemoteMicroservice remoteThre = getRemoteMicroservice();


        apiList.add(remoteAlfa, alfa);
        apiList.add(remoteBeta, beta);
        apiList.add(remoteThre, thre);

        markAsFaulty(beta);

        assertEquals(alfa, apiList.get());
        assertEquals(thre, apiList.get());
        assertEquals(alfa, apiList.get());
    }

    @Test
    public void shouldMapNewAffinityWhenOriginalFaulty() {
        RestApi alfa = newRestApiWithAffinity("alfa");
        RestApi beta = newRestApiWithAffinity("beta");
        RestApi thre = newRestApi("thre");
        RemoteMicroservice remote = getRemoteMicroservice();

        apiList.add(remote, alfa);
        apiList.add(remote, beta);
        apiList.add(remote, thre);

        markAsFaulty(alfa);

        assertEquals(beta, apiList.get());
        assertEquals(beta, apiList.get());

    }


    @Test
    public void shouldNOTRemapToNewlyWorkingAffinityAfterFaulty() {
        RestApi alfa = newRestApiWithAffinity("alfa");
        RestApi beta = newRestApiWithAffinity("beta");
        RestApi thre = newRestApi("thre");
        RemoteMicroservice remote = getRemoteMicroservice();

        apiList.add(remote, alfa);
        apiList.add(remote, beta);
        apiList.add(remote, thre);

        markAsFaulty(alfa);

        assertEquals(beta, apiList.get());
        assertEquals(beta, apiList.get());

        markAsWorking(alfa);

        assertEquals(beta, apiList.get());
        assertEquals(beta, apiList.get());

    }

    private RemoteMicroservice getRemoteMicroservice() {
        return Mockito.mock(RemoteMicroservice.class);
    }

    private void markAsWorking(RestApi api) {
        Mockito.when(api.isFaulty()).thenReturn(false);
    }

    private void markAsFaulty(RestApi api) {
        Mockito.when(api.isFaulty()).thenReturn(true);
    }

    private RestApi newRestApi(String name) {
        RestApi api = Mockito.mock(RestApi.class);
        Mockito.when(api.toString()).thenReturn(name);
        return api;
    }

    private RestApi newRestApiWithAffinity(String name) {
        RestApi api = newRestApi(name);
        Mockito.when(api.hasAffinity()).thenReturn(true);
        return api;
    }
}
