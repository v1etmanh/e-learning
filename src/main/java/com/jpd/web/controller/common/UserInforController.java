package com.jpd.web.controller.common;

import com.jpd.web.transform.UserInfoMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpd.web.dto.UserInfoDto;


@RestController
@RequestMapping("/api/customer")
public class UserInforController {

    @GetMapping("/account_infor")
    public ResponseEntity<UserInfoDto> getAccountInfor(
            @AuthenticationPrincipal Jwt jwt) {

        UserInfoDto userInfo = UserInfoMapper.fromJwt(jwt);
        return ResponseEntity.ok(userInfo);
    }
}
