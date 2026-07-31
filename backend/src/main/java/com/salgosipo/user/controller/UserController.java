package com.salgosipo.user.controller;

import com.salgosipo.global.security.account.domain.CustomUser;
import com.salgosipo.user.dto.*;
import com.salgosipo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@Log4j2
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequestDto dto){
        userService.signup(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/check-id")
    public ResponseEntity<?> checkId(@RequestParam String loginId){
        boolean available = userService.isLoginIdAvailable(loginId);
        return ResponseEntity.ok(available);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal CustomUser customUser){
        String loginId = customUser.getUsername();
        UserProfileResponseDto profile = userService.getProfile(loginId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal CustomUser customUser,
                                           @RequestBody UserUpdateRequestDto dto){
        userService.updateProfile(customUser.getUsername(), dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal CustomUser customUser,
                                            @RequestBody PasswordChangeRequestDto dto){
        userService.changePassword(customUser.getUsername(), dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal CustomUser customUser,
                                      @RequestBody WithdrawRequestDto dto) {
        userService.withdraw(customUser.getUsername(), dto.getPassword());
        return ResponseEntity.ok().build();
    }

}