package com.example.buoi1;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface AddressApiService {
    @GET("p/")
    Call<List<AddressModels.Province>> getProvinces();

    @GET("p/{code}?depth=2")
    Call<AddressModels.Province> getDistricts(@Path("code") int provinceCode);

    @GET("d/{code}?depth=2")
    Call<AddressModels.District> getWards(@Path("code") int districtCode);
}
