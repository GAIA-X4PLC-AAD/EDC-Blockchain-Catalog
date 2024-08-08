package com.msg.plcaad.edc.ccp.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msg.plcaad.edc.ccp.exception.CcpException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.eclipse.edc.spi.monitor.Monitor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;

public class CcpCallingService {

    private ClaimComplianceServiceApi claimComplianceServiceApi;
    private Monitor monitor;

    public CcpCallingService(String url, final Monitor monitor) {
        this.monitor = monitor;
        final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        if (url != null && !url.endsWith("/")) {
            // Add trailing slash if not present since retrofit requires it
            url = url + "/";
        }
        final Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(url)
                .addConverterFactory(JacksonConverterFactory.create(new ObjectMapper()))
                .build();
        this.claimComplianceServiceApi = retrofit.create(ClaimComplianceServiceApi.class);
    }

    public CcpCallingService(final String url, final ClaimComplianceServiceApi claimComplianceServiceApi, final Monitor monitor) {
        this(url, monitor);
        this.claimComplianceServiceApi = claimComplianceServiceApi;
    }

    public String executeClaimComplianceProviderCall(final String claims, final String participantCredentials) throws CcpException {
        final String jsonInputString = String.format("{\"claims\": %s, \"verifiableCredentials\": %s}", claims, participantCredentials);
        final RequestBody body = RequestBody.create( MediaType.parse("application/json; charset=utf-8"), jsonInputString);
        final Call<String> call = claimComplianceServiceApi.callClaimComplianceProvider(body);
        final Response<String> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            throw new CcpException("IOException occurred", e);
        }
        if (!response.isSuccessful()) {
            monitor.severe("Unexpected response status: " + response.code());
            monitor.severe("Response body: " + response.body());
            throw new CcpException("Unexpected response status: " + response.code());
        }
        return response.body();
    }
}
