package com.matdori.matdori.controller;


import com.matdori.matdori.domain.Response;
import com.matdori.matdori.domain.StoreFavorite;
import com.matdori.matdori.domain.User;
import com.matdori.matdori.service.AuthorizationService;
import com.matdori.matdori.service.MailService;
import com.matdori.matdori.service.UserService;
import com.matdori.matdori.service.UserSha256;
import com.matdori.matdori.util.SessionUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;
    private final AuthorizationService authorizationService;
    private final MailService mailService;
    // 회원 가입
    @PostMapping("/sign-up")
    public ResponseEntity<Response<Void>> createUser(@RequestBody @Valid CreateUserRequest request) throws NoSuchAlgorithmException {
        User user = new User();
        user.setEmail(request.email);
        user.setDepartment("학과 parsing 필요");
        user.setPassword(UserSha256.encrypt(request.password));
        user.setNickname("맛도리1234");
        // 약관 동의 추가하는 로직 필요
        userService.signUp(user);
        return ResponseEntity.ok()
                .body(Response.success(null));
    }

    // 가게 좋아요 누르기
    @PostMapping("/users/{userIndex}/favorite-store")
    public ResponseEntity<Response<Void>> createFavoriteStore(@PathVariable("userIndex") Long userId,
                                    @RequestBody @Valid CreateFavoriteStoreRequest requestDto){

        AuthorizationService.checkSession(userId);
        userService.createFavoriteStore(requestDto.storeId, userId);
        return ResponseEntity.ok()
                .body(Response.success(null));
    }

    // 내가 좋아요한 가게 리스트 조회
    @GetMapping("/users/{userIndex}/favorite-stores")
    public ResponseEntity<Response<List<readFavoriteStoresResponse>>> readFavoriteStores(
            @PathVariable("userIndex") Long userId,
            @RequestParam Long pageCount){

        AuthorizationService.checkSession(userId);
        List<StoreFavorite> FavoriteStores = userService.findAllFavoriteStore(userId);
        return ResponseEntity.ok().body(Response.success(FavoriteStores.stream()
                .map(s -> new readFavoriteStoresResponse(s.getId(), s.getStore().getId(), s.getStore().getName(), s.getStore().getImg_url()))
                .collect(Collectors.toList())));
    }

    // 내가 좋아요한 가게 삭제
    @DeleteMapping("/users/{userIndex}/favorite-stores/{favoriteStoreIndex}")
    public ResponseEntity<Response<Void>> deleteFavoriteStore(
            @PathVariable("userIndex") Long userId,
            @PathVariable("favoriteStoreIndex") Long storeId){

        AuthorizationService.checkSession(userId);
        userService.deleteFavoriteStore(storeId);
        return ResponseEntity.ok()
                .body(Response.success(null));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<Response<LoginResponse>> login(@RequestBody LoginRequest request) throws NoSuchAlgorithmException {
        User user = authorizationService.login(request.email, UserSha256.encrypt(request.password));
        String uuid = UUID.randomUUID().toString();

        SessionUtil.setAttribute(uuid, String.valueOf(user.getId()));
        return ResponseEntity.ok()
                .header("set-cookie","sessionId="+uuid)
                .body(Response.success(new LoginResponse(new LoginResult(user.getId(), user.getNickname(), user.getDepartment()))));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Response<Void>> login(HttpServletRequest request) {
        AuthorizationService.logout(request.getSession());
        return ResponseEntity.ok()
                .body(Response.success(null));
    }

    // 이메일 인증
    @PostMapping("/email-authentication")
    public ResponseEntity<Response<Void>> authenticateEmail(@RequestBody @Valid AuthenticateEmailRequest request){

        mailService.sendAuthorizationMail(request.email);
        return ResponseEntity.ok()
                .body(Response.success(null));
    }
    // 인증번호 체크
    @PostMapping("/authentication-number")
    public ResponseEntity<Response<Void>> authenticateNumber(@RequestBody @Valid AuthenticateNumberRequest request){
        // 인증하고 최종 회원 가입 시까지 인증여부를 남겨둬야될 듯
        AuthorizationService.checkAuthorizationNumber(request.number);
        return ResponseEntity.ok()
                .body(Response.success(null));
    }

    /* 좋아하는 족보 테이블 완성되면 작업
    @PostMapping("/users/{userIndex}/favorite-jokbo")
    public ResponseEntity<Response<Void>> createFavoriteJokbo(@RequestBody @Valid CreateFavoriteJokboRequest request,
                                                              @PathVariable("userIndex") Long userId){
        AuthorizationService.checkSession(userId);
        return ResponseEntity.ok()
                .body(Response.success(null));
    }   
    */

    @PutMapping("/users/{userIndex}/password")
    public ResponseEntity<Response<Void>> updatePassword(@RequestBody @Valid UpdatePasswordRequest request,
                                                         @PathVariable("userIndex") Long userId) throws NoSuchAlgorithmException{
        AuthorizationService.checkSession(userId);
        userService.updatePassword(userId,request.password);
        return ResponseEntity.ok()
                .body(Response.success(null));
    }

    @Data
    static class LoginRequest{
        private String email;
        private String password;
    }

    @Data
    @AllArgsConstructor
    static class LoginResponse<T>{
        private T data;
    }
    @Data
    @AllArgsConstructor
    static class LoginResult{
        private Long userId;
        private String nickname;
        private String department;
    }
    @Data
    @AllArgsConstructor
    static class CreateFavoriteStoreRequest{
        private Long storeId;
    }

    @Data
    @AllArgsConstructor
    static class readFavoriteStoresResponse{
        private Long favoriteStoreId;
        private Long storeId;
        private String name;
        private String imgUrl;
    }

    @Data
    static class CreateUserRequest{
        private String email;
        private String password;
        //private int[] termIndex;
    }

    @Data
    static class AuthenticateEmailRequest{
        private String email;
    }
    @Data
    static class AuthenticateNumberRequest{
        private String number;
    }

    @Data
    static class CreateFavoriteJokboRequest{
        private Long storeId;
    }

    @Data
    static class UpdatePasswordRequest{
        private String password;
    }
}