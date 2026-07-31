package com.salgosipo.user.controller;

import com.salgosipo.user.dto.SignupRequestDto;
import com.salgosipo.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email){
        boolean available = userService.isEmailAvailable(email);
        return ResponseEntity.ok(available);
    }
}