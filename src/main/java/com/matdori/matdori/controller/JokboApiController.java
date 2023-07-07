package com.matdori.matdori.controller;

import com.matdori.matdori.domain.*;
import com.matdori.matdori.service.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class JokboApiController {

    private final JokboService jokboService;
    private final StoreService storeService;
    private final UserService userService;
    private final S3UploadService s3UploadService;

    /**
     * 족보 작성하기.
     */
    @PostMapping("/users/{userIndex}/jokbo")
    public void createJokbo(@PathVariable("userIndex") Long userIndex,
                            @Valid CreateJokboRequest request) throws IOException {
        Jokbo jokbo = new Jokbo();
        User user = userService.findOne(userIndex);
        jokbo.setUser(user);

        jokbo.setTotalRating(request.getTotal_rating());
        jokbo.setFlavorRating(request.getFlavor_rating());
        jokbo.setUnderPricedRating(request.getUnder_priced_rating());
        jokbo.setCleanRating(request.getClean_rating());

        Store mappingStore = storeService.findOne(request.getStore_index());
        jokbo.setStore(mappingStore);

        jokbo.setTitle(request.getTitle());
        jokbo.setContents(request.getContents());

        jokboService.createJokbo(jokbo);


        List<MultipartFile> images = request.getImages();
        List<String> imageUrls = s3UploadService.uploadFiles(images);

        if(!CollectionUtils.isEmpty(imageUrls)) {
            List<JokboImg> jokboImgs = new ArrayList<>();
            for(String imgUrl : imageUrls) {
                JokboImg jokboImg = new JokboImg();
                jokboImg.setJokbo(jokbo);
                jokboImg.setImgUrl(imgUrl);

                jokboImgs.add(jokboImg);
            }
            jokboService.createJokboImg(jokboImgs);
        }
    }

    /**
     * 족보 내용 조회하기
     */
    @GetMapping("/jokbos/{jokboIndex}")
    public ResponseEntity<Response<JokboContentsResponse>>
            readJokbo(@PathVariable("jokboIndex") Long id) {
        Jokbo jokbo = jokboService.findOne(id);
        List<String> jokboImgUrls = jokboService.getImageUrls(jokbo.getJokboImgs());

        return ResponseEntity.ok().body(
                Response.success(
                        new JokboContentsResponse(
                                jokbo.getStore().getId(),
                                jokbo.getStore().getName(),
                                jokbo.getStore().getImg_url(),
                                jokbo.getTitle(),
                                jokbo.getUser().getNickname(),
                                jokbo.getContents(),
                                jokboImgUrls
                        )
                )
        );
    }

    /**
     * 내가 쓴 족보 삭제하기
     */
    @DeleteMapping("/users/{userIndex}/jokbos/{jokboIndex}")
    public ResponseEntity<Response<Void>> deleteJokbo (
            @PathVariable("userIndex") Long userId,
            @PathVariable("jokboIndex") Long jokboId) {

        AuthorizationService.checkSession(userId);

        Jokbo jokbo = jokboService.findOne(jokboId);
        List<JokboImg> jokboImgs = jokbo.getJokboImgs();
        List<String> imgUrls = jokboService.getImageUrls(jokboImgs);

        jokboService.deleteJokbo(jokbo, userId, jokboImgs);
        s3UploadService.deleteFile(imgUrls);

        return ResponseEntity.ok().body(
                Response.success(null)
        );
    }

    /**
     * 족보에 댓글 등록하기.
     */
    @PostMapping("/jokbos/{jokboIndex}/comment")
    public ResponseEntity<Response<Void>> createJokboComment(
            @PathVariable("jokboIndex") Long jokboId,
            @RequestBody @Valid CreateJokboCommentRequest request) {

        AuthorizationService.checkSession(request.getUser_index());

        JokboComment jokboComment = new JokboComment();

        Jokbo jokbo = jokboService.findOne(jokboId);
        jokboComment.setJokbo(jokbo);

        User user = userService.findOne(request.getUser_index());
        jokboComment.setUser(user);

        jokboComment.setContents(request.getContents());
        jokboComment.setIsDeleted(false);

        jokboService.createJokboComment(jokboComment);

        return ResponseEntity.ok().body(
                Response.success(null)
        );
    }

    /**
     * 족보 글에 달린 모든 댓글 조회하기.
     * 페이징 처리 및 정렬 처리 구현 필요
     */
    @GetMapping("/jokbos/{jokboIndex}/comments")
    public ResponseEntity<Response<List<JokboCommentResponse>>> getAllJokboComments (
            @PathVariable("jokboIndex") Long jokboId,
            @RequestParam(value = "order", required = false) String order,
            @RequestParam(value = "pageCount", required = false) Long pageCount) {

        // jokboId가 존재하는 족보의 id인지 유효성 체크 필요.
        // 페이징 처리 및 정렬 처리 구현 필요.

        List<JokboComment> jokboComments = jokboService.getAllJokboComments(jokboId);
        return ResponseEntity.ok().body(
                Response.success(jokboComments.stream()
                        .map(c -> new JokboCommentResponse(
                                c.getId(),
                                c.getCreatedAt(),
                                c.getContents(),
                                c.getIsDeleted(),
                                c.getUser().getId(),
                                c.getUser().getNickname()))
                        .collect(Collectors.toList())));
    }

    /**
     * 총 족보 개수 조회하기.
     */
    @GetMapping("/jokbo-count")
    public int countAllJokbos() {
        int count = jokboService.countAll();
        return count;
    }

    /**
     * 족보 생성을 위한 DTO
     */
    @Data
    static class CreateJokboRequest {
        private int total_rating;
        private int flavor_rating;
        private int under_priced_rating;
        private int clean_rating;

        private Long store_index;

        private String title;
        private String contents;

        private List<MultipartFile> images;
    }

    /**
     * 족보 조회하기의 응답을 위한 DTO
     */
    @Data
    @AllArgsConstructor
    static class JokboContentsResponse {
        Long store_index;
        String store_name;
        String store_img_url;

        String title;
        String nickname;
        String contents;

        List<String> jokbo_img_url_list;
    }

    /**
     * 족보 댓글 작성을 위한 DTO
     */
    @Data
    static class CreateJokboCommentRequest {
        private Long user_index;
        private String contents;
    }

    /**
     * 족보에 달린 모든 댓글 조회하기의 응답을 위한 DTO
     */
    @Data
    @AllArgsConstructor
    static class JokboCommentResponse {
        Long comment_index;
        LocalDateTime created_at;
        String contents;
        boolean check_deleted;

        Long user_index;
        String nickname;
    }
}