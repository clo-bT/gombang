package com.ssafy.roomDeal.domain;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.awt.*;
import java.sql.Date;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDeal {

    @Id
    @Column(name = "room_deal_id")
    private UUID id;

    @NotNull
    private String roomType;

    @NotNull
    private Double roomSize;

    @NotNull
    private Integer roomCount;

    private String oneroomType;

    @NotNull
    private Integer bathroomCount;

    @NotNull
    private String roadAddress;

    @NotNull
    private String jibunAddress;

    @NotNull
    private Integer monthlyFee;

    @NotNull
    private Integer deposit;

    @NotNull
    private Integer managementFee;

    @NotNull
    private Date usageDate;

    @NotNull
    private Date moveInDate;

    @NotNull
    private Date expirationDate;

    @NotNull
    private Integer floor;

    @NotNull
    private Integer totalFloor;

    @NotNull
    private Point position;

    @NotNull
    private DealStatus dealStatus;

    private String thumbnail;

    private String station;

    private Double stationDistance;

    private String univ;

    private Double univDistance;

    private String content;

    @NotNull
    private Date regTime;

    // User -> Member로 변경시 FK로 가져올 것

}
