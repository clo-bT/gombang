package com.ssafy.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
public class KakaoLoginResponseDto {

    private String id;

    private KakaoAccount kakao_account;

    @Getter
    @NoArgsConstructor
    public class KakaoAccount {

        private String email;

        private String gender;

    }

}
