package com.matdori.matdori.controller;

import com.matdori.matdori.domain.*;
import com.matdori.matdori.repositoy.Dto.StoreInformationHeader;
import com.matdori.matdori.repositoy.Dto.StoreListByCategory;
import com.matdori.matdori.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "가게 API", description = "가게와 관련된 API들")
@RestController
@RequiredArgsConstructor
public class StoreApiController {

    private final StoreService storeService;

    /**
     * 가게 정보 탭 조회하기.
     *
     */
    @Operation(summary = "가게 정보 탭 조회 API", description = "가게 정보 탭을 조회합니다.")
    @Parameter(name = "storeIndex", description = "가게 id", required = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "필수 파라미터 누락(INVALID_REQUIRED_PARAM)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 가게(NOT_EXISTED_STORE)"),
            @ApiResponse(responseCode = "500", description = "서버 에러", content = @Content(schema = @Schema(implementation = Error.class)))
    })
    @GetMapping("/stores/{storeIndex}/information")
    public ResponseEntity<Response<StoreInformationResponse>> readStoreInformation(@PathVariable("storeIndex") Long id){
        Store store = storeService.findOne(id);

        return ResponseEntity.ok().body(
                Response.success(
                        new StoreInformationResponse(
                                store.getOpenHours(),
                                store.getPhoneNumber(),
                                store.getAddress()
                                )));
    }

    /**
     * 메뉴 탭 조회하기.
     *
     */
    @Operation(summary = "메뉴 탭 조회 API", description = "가게 정보의 메뉴 탭을 조회합니다.")
    @Parameter(name = "storeIndex", description = "가게 id", required = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "필수 파라미터 누락(INVALID_REQUIRED_PARAM)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 가게(NOT_EXISTED_STORE)"),
            @ApiResponse(responseCode = "500", description = "서버 에러", content = @Content(schema = @Schema(implementation = Error.class)))
    })
    @GetMapping("/stores/{storeIndex}/menu")
    public ResponseEntity<Response<List<StoreMenuResponse>>> readStoreMenu(@PathVariable("storeIndex") Long id){
        List<Category> categories = storeService.findAllCategoryWithMenu(id);

        return ResponseEntity.ok().body(Response.success(categories.stream().map(c -> new StoreMenuResponse(c))
                .collect(Collectors.toList())));
    }

    /**
     * 가게 족보 탭 조회하기.
     */
    @Operation(summary = "가게 족보 탭 조회 API", description = "가게 정보의 족보 탭을 조회합니다.")
    @Parameters({
            @Parameter(name = "storeIndex", description = "가게 id", required = true),
            @Parameter(name = "pageCount", description = "시작페이지 : 1 , 한 페이지 당 15개씩 응답", required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "필수 파라미터 누락(INVALID_REQUIRED_PARAM)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 가게(NOT_EXISTED_STORE)"),
            @ApiResponse(responseCode = "500", description = "서버 에러", content = @Content(schema = @Schema(implementation = Error.class)))
    })
    @GetMapping("/stores/{storeIndex}/jokbos")
    public ResponseEntity<Response<ReadAllJokboResponse>> readAllJokbo(@PathVariable("storeIndex") Long storeIndex,
                                                                      @RequestParam int pageCount){
        int jokboCount = storeService.countAllJokbos(storeIndex);
        List<Jokbo> jokbos = storeService.findAllJokbo(storeIndex, pageCount);
        List<JokboResponse> jokboList = jokbos.stream()
                .map(j -> {
                    Double totalRating = (double) ((j.getCleanRating() + j.getFlavorRating() + j.getUnderPricedRating())/3);
                    return new JokboResponse(
                            j.getId(),
                            j.getTitle(),
                            j.getContents(),
                            j.getJokboImgs(),
                            j.getJokboFavorites().size() ,
                            j.getJokboComments().size(),
                            totalRating);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok()
                .body(
                        Response.success(
                                new ReadAllJokboResponse(
                                        jokboCount,
                                        jokboList
                                )));
    }

    /**
     * 상단 가게 이름 및 별점 표시 부분 조회하기.
     */
    @Operation(summary = "가게 이름 및 별점 표시 조회 API", description = "가게 정보 상단의 가게 이름 및 별점 표시 부분을 조회합니다.")
    @Parameter(name = "storeIndex", description = "가게 id", required = true)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "필수 파라미터 누락(INVALID_REQUIRED_PARAM)", content = @Content(schema = @Schema(implementation = Error.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 가게(NOT_EXISTED_STORE)", content = @Content(schema = @Schema(implementation = Error.class))),
            @ApiResponse(responseCode = "500", description = "서버 에러", content = @Content(schema = @Schema(implementation = Error.class)))
    })
    @GetMapping("/stores/{storeIndex}/info-header")
    public ResponseEntity<Response<StoreInformationHeader>> readStoreInformationHeader(@PathVariable("storeIndex") Long storeIndex){
        StoreInformationHeader storeInformationHeader = storeService.readStoreInformationHeader(storeIndex);

        return ResponseEntity.ok().body(Response.success(storeInformationHeader));
    }

    /**
     * 카테고리별 가게 리스트 조회하기
     */
    @Operation(summary = "카테고리별 가게 조회", description = "카테고리에 해당하는 가게들을 조회합니다.")
    @Parameters({
            @Parameter(name = "category", description = "카테고리", required = true),
            @Parameter(name = "pageCount", description = "시작페이지 : 1 , 한 페이지 당 15개씩 응답", required = true)
    })
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "필수 파라미터 누락(INVALID_REQUIRED_PARAM)<br> 유효하지 않은 카테고리(NOT_EXISTED_STORE_CATEGORY)", content = @Content(schema = @Schema(implementation = Error.class))),
            @ApiResponse(responseCode = "500", description = "서버 에러", content = @Content(schema = @Schema(implementation = Error.class)))
    })
    @GetMapping("/stores")
    public ResponseEntity<Response<List<StoreListByCategory>>> readStoresByCategory(@RequestParam("category")String category,
                                                                                    @RequestParam("pageCount")int startIndex){
        List<StoreListByCategory> stores = storeService.findByCategory(category, startIndex);

        return ResponseEntity.ok().body(Response.success(stores));
    }

    @Data
    @AllArgsConstructor
    static class StoreMenuResponse{
        String name;
        List<MenuDto> menus = new ArrayList<>();
        public StoreMenuResponse(Category category) {
            this.name = category.getName();
            this.menus = category.getMenus().stream()
                    .map(c -> new MenuDto(c))
                    .collect(Collectors.toList());
        }
    }

    @Data
    @AllArgsConstructor
     static class MenuDto{
        private String name;
        private String price;
        private String imgUrl;

        public MenuDto(Menu menu) {
            this.name = menu.getName();
            this.price = menu.getPrice();
            this.imgUrl = menu.getImgUrl();
        }
    }

    @Data
    @AllArgsConstructor
    static class StoreInformationResponse{
        OpenHours time;
        String phoneNumber;
        String address;
    }

    @Data
    @AllArgsConstructor
    static class JokboResponse{
        private Long jokboId;
        private String title;
        private String contents;
        private String imgUrl;
        private int favoriteCnt;
        private int commentCnt;
        private double totalRating;

        public JokboResponse(Long jokboId, String title, String contents, List<JokboImg> imgUrl, int favoriteCnt, int commentCnt, double totalRating) {
            this.jokboId = jokboId;
            this.title = title;
            this.contents = contents;
            if(imgUrl.size() != 0)
                this.imgUrl = imgUrl.get(0).getImgUrl();
            this.favoriteCnt = favoriteCnt;
            this.commentCnt = commentCnt;
            this.totalRating = totalRating;
        }
    }

    @Data
    @AllArgsConstructor
    static class ReadAllJokboResponse {
        private int jokboCnt;
        List<JokboResponse> jokboList;
    }

}
