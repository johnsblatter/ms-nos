package com.workshare.msnos.core.protocols.ip.www;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.mockito.ArgumentCaptor;

public class HttpClientHelper {

    private HttpClient client;
    private HttpResponse response;

    public HttpClientHelper() throws Exception {
        this.client = mock(HttpClient.class);
        this.response = mock(HttpResponse.class);
        
        when(response().getEntity()).thenReturn(new StringEntity(""));
        when(client().execute(any(HttpUriRequest.class))).thenReturn(response());
    }

    public HttpClient client() {
        return client;
    }

    public HttpResponse response() {
        return response;
    }

    public HttpPost getLastPostToWWW() throws Exception {
        return getLastRequestToWWW(HttpPost.class);
    }

    public HttpGet getLastGetToWWW() throws Exception {
        return getLastRequestToWWW(HttpGet.class);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getAllRequestToWWW(Class<T> type) throws Exception {
        List<T> result = new ArrayList<T>();
    
        List<HttpUriRequest> requests = getLastRequestsToWWW();
        for (HttpUriRequest request : requests) {
            if (request.getClass() == type) {
                result.add((T) request);
            }
        }
    
        return result;
    }

    public <T> T getLastRequestToWWW(Class<T> type) throws Exception {
        List<T> all = getAllRequestToWWW(type);
        if (all.size() > 0)
            return all.get(all.size() - 1);
        else
            return null;
    }

    public List<HttpUriRequest> getLastRequestsToWWW() throws IOException, ClientProtocolException {
        try {
            ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
            verify(client, atLeastOnce()).execute(captor.capture());
            return captor.getAllValues();
        } catch (Throwable any) {
            return Collections.<HttpUriRequest>emptyList();
        }
    }

    public void assertRequestsContains(List<? extends HttpUriRequest> requests, String url) {
        for (HttpUriRequest request : requests) {
            if (request.getURI().toString().equalsIgnoreCase(url))
                return;
        }
    
        fail("Request for url " + url + " not found!");
    }
}
