package com.ssafy.roomDeal.service;

import com.ssafy.elasticsearch.dto.RoomDealNearestStationDto;
import com.ssafy.elasticsearch.dto.RoomDealSearchDto;
import com.ssafy.elasticsearch.dto.SearchByAddressRequestDto;
import com.ssafy.elasticsearch.dto.SearchNearestStationUnivRequestDto;
import com.ssafy.elasticsearch.repository.RoomDealElasticSearchRepository;
import com.ssafy.member.domain.Member;
import com.ssafy.member.repository.MemberRepository;
import com.ssafy.roomDeal.domain.RoomDeal;
import com.ssafy.roomDeal.domain.RoomDealOption;
import com.ssafy.roomDeal.dto.*;
import com.ssafy.roomDeal.repository.RoomDealOptionReposiroty;
import com.ssafy.roomDeal.repository.RoomDealRepository;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortMode;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RoomDealService {

    private final ElasticsearchOperations elasticsearchOperations;

    private final MemberRepository memberRepository;
    private final RoomDealRepository roomDealRepository;
    private final RoomDealOptionReposiroty roomDealOptionReposiroty;
    private final RoomDealElasticSearchRepository roomDealElasticSearchRepository;

    // 매물 등록
    @Transactional
    public RoomDealResponseDto registerRoomDeal(RoomDealRegisterRequestDto roomDealRegisterRequestDto) {
        Member newMember = memberRepository.findById(roomDealRegisterRequestDto.getRoomDealRegisterDefaultDto().getId()).get();
        RoomDeal newRoomDeal = new RoomDeal(roomDealRegisterRequestDto.getRoomDealRegisterDefaultDto(), newMember);
        RoomDealOption newRoomDealOption = new RoomDealOption(newRoomDeal, roomDealRegisterRequestDto.getRoomDealRegisterOptionDto());

        roomDealRepository.save(newRoomDeal);
        roomDealOptionReposiroty.save(newRoomDealOption);

        String id = String.valueOf(newRoomDeal.getId());
        Long roomId = newRoomDeal.getId();
        String address = newRoomDeal.getJibunAddress();
        SearchNearestStationUnivRequestDto searchNearestStationUnivRequestDto = new SearchNearestStationUnivRequestDto("37.1", "127.1");
        String content = newRoomDeal.getContent();

        RoomDealSearchDto roomDealSearchDto = new RoomDealSearchDto(id, roomId, address, searchNearestStationUnivRequestDto, content);

        /* ES 매물 등록 - 추후 Position 수정 */
        try {
            roomDealElasticSearchRepository.save(roomDealSearchDto);
        } catch (Exception e) {
            return new RoomDealResponseDto(newRoomDeal, newRoomDealOption);
        }

        return new RoomDealResponseDto(newRoomDeal, newRoomDealOption);
    }

    // 매믈 조회
    public RoomDealResponseDto getRoomDeal(Long id) {
        Optional<RoomDeal> roomDeal = roomDealRepository.findById(id);
        Optional<RoomDealOption> roomDealOption = roomDealOptionReposiroty.findById(id);

        if (roomDeal.isPresent() && roomDealOption.isPresent()) {
            return new RoomDealResponseDto(roomDeal.get(), roomDealOption.get());
        } else {
            throw new IllegalArgumentException("존재하지 않는 roomDeal입니다.");
        }
    }

    // 매물 수정
    @Transactional
    public RoomDealResponseDto updateRoomDeal(RoomDealUpdateRequestDto roomDealUpdateRequestDto) {
        Optional<RoomDeal> roomDealOptional = roomDealRepository.findById(roomDealUpdateRequestDto.getRoomDealId());
        Optional<RoomDealOption> roomDealOptionOptional = roomDealOptionReposiroty.findById(roomDealUpdateRequestDto.getRoomDealId());

        if (roomDealOptional.isPresent() && roomDealOptionOptional.isPresent()) {
            RoomDeal roomDeal = roomDealOptional.get();

            // 본인 확인
            if (roomDeal.getMember().getId().equals(roomDealUpdateRequestDto.getMemberId())) {
                roomDeal.roomDealUpdate(roomDealUpdateRequestDto);
                return new RoomDealResponseDto(roomDealOptional.get(), roomDealOptionOptional.get());
            } else {
                throw new IllegalArgumentException("작성자와 수정자가 일치하지 않습니다.");
            }
        } else {
            throw new IllegalArgumentException("존재하지 않는 roomDeal입니다.");
        }


    }

    // 매물 삭제
    @Transactional
    public RoomDealDeleteResponseDto deleteRoomDeal(RoomDealDeleteRequestDto roomDealDeleteRequestDto) {

        Optional<RoomDeal> roomDealOptional = roomDealRepository.findById(roomDealDeleteRequestDto.getRoomDealId());
        Optional<RoomDealOption> roomDealOptionOptional = roomDealOptionReposiroty.findById(roomDealDeleteRequestDto.getRoomDealId());

        if (roomDealOptional.isPresent() && roomDealOptionOptional.isPresent()) {
            RoomDeal roomDeal = roomDealOptional.get();
            RoomDealOption roomDealOption = roomDealOptionOptional.get();
            // 본인 확인
            if (roomDeal.getMember().getId().equals(roomDealDeleteRequestDto.getMemberId())) {
                roomDealOptionReposiroty.deleteById(roomDealDeleteRequestDto.getRoomDealId());
                roomDealRepository.deleteById(roomDealDeleteRequestDto.getRoomDealId());
                return new RoomDealDeleteResponseDto(roomDealDeleteRequestDto.getRoomDealId());
            } else {
                throw new IllegalArgumentException("작성자와 삭제자가 일치하지 않습니다.");
            }
        } else {
            throw new IllegalArgumentException("존재하지 않는 roomDeal입니다.");
        }
    }

    /**
     * 지번주소로 매물 검색
     *
     * @param searchByAddressRequestDto
     * @return
     */
    public List<RoomDealSearchDto> searchByAddress(SearchByAddressRequestDto searchByAddressRequestDto) {
        // match_phrase query 생성
        MatchPhraseQueryBuilder matchPhraseQuery = QueryBuilders.matchPhraseQuery("address", searchByAddressRequestDto.getAddress());

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder(); // Bool Query 생성
        ArrayList<QueryBuilder> queryBuilderList = new ArrayList<>(); // Bool Query 안에 넣을 query List 생성
        queryBuilderList.add(matchPhraseQuery); // match_phrase query를 list 안에 저장

        if (!searchByAddressRequestDto.getContent().isEmpty()) {
            // match query 생성
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("content.nori", searchByAddressRequestDto.getContent());
            queryBuilderList.add(matchQuery);
        }

        boolQueryBuilder.must().addAll(queryBuilderList); // Bool Query List를 Bool에 저장 => must : 조건 모두 일치

        // _search query 생성
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(boolQueryBuilder);

        // 결과 출력
        SearchHits<RoomDealSearchDto> articles = elasticsearchOperations
                .search(queryBuilder.build(), RoomDealSearchDto.class, IndexCoordinates.of("rooms_data"));

        // 결과 => Document로 매핑
        List<SearchHit<RoomDealSearchDto>> searchHitList = articles.getSearchHits();
        ArrayList<RoomDealSearchDto> roomDealSearchDtoList = new ArrayList<>();
        for (SearchHit<RoomDealSearchDto> item : searchHitList) {
            roomDealSearchDtoList.add(item.getContent());
        }

        return roomDealSearchDtoList;
    }

    /**
     * 위도, 경도로 매물 검색
     *
     * @param searchNearestStationUnivRequestDto
     * @return
     */
    public List<RoomDealSearchDto> searchByLocation(SearchNearestStationUnivRequestDto searchNearestStationUnivRequestDto) {
        double lat = Double.parseDouble(searchNearestStationUnivRequestDto.getLat());
        double lon = Double.parseDouble(searchNearestStationUnivRequestDto.getLon());

        // geo_point query 생성
        GeoDistanceQueryBuilder geoDistanceQueryBuilder = QueryBuilders.geoDistanceQuery("location")
                .point(lat, lon)
                .distance(1000, DistanceUnit.KILOMETERS);

        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder(); // Bool Query 생성
        ArrayList<QueryBuilder> queryBuilderList = new ArrayList<>(); // Bool Query 안에 넣을 query List 생성
        queryBuilderList.add(geoDistanceQueryBuilder); // match_phrase query를 list 안에 저장

        if (!searchNearestStationUnivRequestDto.getContent().isEmpty()) {
            // match query 생성
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("content.nori", searchNearestStationUnivRequestDto.getContent());
            queryBuilderList.add(matchQuery); // match query 저장
        }

        boolQueryBuilder.must().addAll(queryBuilderList); // Bool Query List를 Bool에 저장 => must : 조건 모두 일치

        // _search query 생성
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        // match_phrase query를 _search 안에 저장
        queryBuilder.withQuery(boolQueryBuilder)
                .withSort(SortBuilders // sort builder
                        .geoDistanceSort("location", lat, lon) // 거리 기준 오름차순 정렬
                        .order(SortOrder.ASC)
                        .sortMode(SortMode.MIN))
                .withPageable(PageRequest.of(0, 100)).build(); // size 제한 (100개)

        // 결과 출력
        SearchHits<RoomDealSearchDto> articles = elasticsearchOperations
                .search(queryBuilder.build(), RoomDealSearchDto.class, IndexCoordinates.of("rooms_data"));

        // 결과 => Document로 매핑
        List<SearchHit<RoomDealSearchDto>> searchHitList = articles.getSearchHits();
        ArrayList<RoomDealSearchDto> roomDealSearchDtoList = new ArrayList<>();
        for (SearchHit<RoomDealSearchDto> item : searchHitList) {
            roomDealSearchDtoList.add(item.getContent());
        }

        return roomDealSearchDtoList;
    }

    /**
     * 본문 검색
     *
     * @param sentence
     * @return
     */
    public List<RoomDealSearchDto> searchByContent(String sentence) {
        // match_phrase query 생성
        MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("content.nori", sentence);

        // _search query 생성
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(matchQuery); // match_phrase query를 _search 안에 저장

        // 결과 출력
        SearchHits<RoomDealSearchDto> articles = elasticsearchOperations
                .search(queryBuilder.build(), RoomDealSearchDto.class, IndexCoordinates.of("rooms_data"));

        // 결과 => Document로 매핑
        List<SearchHit<RoomDealSearchDto>> searchHitList = articles.getSearchHits();
        ArrayList<RoomDealSearchDto> roomDealSearchDtoList = new ArrayList<>();
        for (SearchHit<RoomDealSearchDto> item : searchHitList) {
            roomDealSearchDtoList.add(item.getContent());
        }

        return roomDealSearchDtoList;
    }

    /**
     * 주소 API를 통해 위도, 경도 가져옴 => 가까운 역 찾기
     * @param searchNearestStationUnivRequestDto
     */
    public List<RoomDealNearestStationDto> getNearestStation(SearchNearestStationUnivRequestDto searchNearestStationUnivRequestDto) {
        double lat = Double.parseDouble(searchNearestStationUnivRequestDto.getLat());
        double lon = Double.parseDouble(searchNearestStationUnivRequestDto.getLon());

        // geo_point query 생성
        GeoDistanceQueryBuilder geoDistanceQueryBuilder = QueryBuilders.geoDistanceQuery("location")
                .point(lat, lon)
                .distance(3, DistanceUnit.KILOMETERS);

        // _search query 생성
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        // match_phrase query를 _search 안에 저장
        queryBuilder.withQuery(geoDistanceQueryBuilder)
                .withSort(SortBuilders // sort builder
                        .geoDistanceSort("location", lat, lon) // 거리 기준 오름차순 정렬
                        .order(SortOrder.ASC)
                        .sortMode(SortMode.MIN))
                .withPageable(PageRequest.of(0, 3)).build(); // size 제한 (3개)

        // 결과 출력
        SearchHits<RoomDealNearestStationDto> articles = elasticsearchOperations
                .search(queryBuilder.build(), RoomDealNearestStationDto.class, IndexCoordinates.of("stations"));

        // 결과 => Document로 매핑
        List<SearchHit<RoomDealNearestStationDto>> searchHitList = articles.getSearchHits();
        ArrayList<RoomDealNearestStationDto> roomDealSearchDtoList = new ArrayList<>();
        for (SearchHit<RoomDealNearestStationDto> item : searchHitList) {
            roomDealSearchDtoList.add(item.getContent());
        }

        return roomDealSearchDtoList;
    }
}
