package com.ssafy.roomDeal.repository;

import com.ssafy.roomDeal.domain.RoomDeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomDealRepository extends JpaRepository<RoomDeal, Long> {

}
