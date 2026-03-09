package com.tavemakers.surf.global.config;

import org.springframework.stereotype.Component;

@Component
public class PermitUrlConfig {

    public String[] getPublicUrl(){
        return new String[]{
                "/login/**",

                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/v1/manager/sign-in"
//                "/v1/member/**",
//                "/v1/manager/**"

        };
    }

    public String[] getMemberUrl(){
        return new String[]{
                "/v1/user/**"
        };
    }

    public String[] getAdminUrl(){
        return new String[]{
                "/v1/admin/**",
                "/v1/manager/**"
        };
    }

}