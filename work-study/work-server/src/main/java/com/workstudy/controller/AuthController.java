package com.workstudy.controller;

import com.workstudy.annotation.Log;
import com.workstudy.constant.JwtClaimsConstant;
import com.workstudy.dto.LoginDTO;
import com.workstudy.entity.SysUser;
import com.workstudy.properties.JwtProperties;
import com.workstudy.result.Result;
import com.workstudy.service.AuthService;
import com.workstudy.utils.JwtUtil;
import com.workstudy.vo.LoginVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一登录鉴权（全系统单一登录通道）
 */
@RestController
@Slf4j
@Tag(name = "鉴权相关接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param loginDTO
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "登录")
    @Log(title = "用户登录")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) {
        log.info("用户登录：{}", loginDTO.getUsername());

        //1、账号密码与状态校验
        SysUser sysUser = authService.login(loginDTO);

        //2、登录成功后，生成jwt令牌（claims 中携带 userId、role、username、name）
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, sysUser.getId());
        claims.put(JwtClaimsConstant.ROLE, sysUser.getRole());
        claims.put(JwtClaimsConstant.USERNAME, sysUser.getUsername());
        claims.put(JwtClaimsConstant.NAME, sysUser.getName());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        //3、封装返回
        LoginVO loginVO = LoginVO.builder()
                .id(sysUser.getId())
                .username(sysUser.getUsername())
                .name(sysUser.getName())
                .role(sysUser.getRole())
                .deptId(sysUser.getDeptId())
                .studentId(sysUser.getStudentId())
                .avatar(sysUser.getAvatar())
                .token(token)
                .build();

        return Result.success(loginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    @Log(title = "用户退出")
    public Result<String> logout() {
        return Result.success();
    }

}
