package com.msg.plcaad.edc.ccp.api;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ClaimComplianceServiceApi {

    @Headers({"Content-Type: application/json",
              "Accept: application/json"})
    @POST(".")
    Call<String> callClaimComplianceProvider(@Body RequestBody body);
}
