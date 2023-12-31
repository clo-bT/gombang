package com.ssafy.elasticsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.awt.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SearchNearestStationUnivRequestDto {

    private String lat; // 위도

    private String lon; // 경도

    private String content; // 본문 내용

    public SearchNearestStationUnivRequestDto(String lat, String lon) {
        this.lat = lat;
        this.lon = lon;
    }
}
