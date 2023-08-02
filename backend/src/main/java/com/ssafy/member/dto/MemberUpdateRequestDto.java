package com.ssafy.member.dto;

import com.ssafy.member.domain.Member;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@AllArgsConstructor
public class MemberUpdateRequestDto {

    @NotNull
    private Member member;
}
