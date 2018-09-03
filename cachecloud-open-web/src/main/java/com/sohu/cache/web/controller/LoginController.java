package com.sohu.cache.web.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.sohu.cache.constant.AppUserTypeEnum;
import com.sohu.cache.entity.AppUser;
import com.sohu.cache.entity.LoginResponse;
import com.sohu.cache.entity.LoginResult;
import com.sohu.cache.entity.User;
import com.sohu.cache.entity.UserDetail;
import com.sohu.cache.util.ConstUtils;
import com.sohu.cache.util.HttpPostUtil;
import com.sohu.cache.web.enums.AdminEnum;
import com.sohu.cache.web.enums.LoginEnum;
import com.sohu.cache.web.enums.SuccessEnum;
import com.sohu.cache.web.service.UserLoginStatusService;
import com.sohu.cache.web.util.LoginUtil;

/**
 * 登录逻辑
 *
 * @author leifu
 * @Time 2014年6月12日
 */
@Controller
@RequestMapping("manage")
public class LoginController extends BaseController {
    
    @Resource(name = "userLoginStatusService")
    private UserLoginStatusService userLoginStatusService;
    
    private static String SSO_URL = "https://sso.onething.net/login";

    /**
     * 用户登录界面
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/newlogin", method = RequestMethod.GET)
    public void init(HttpServletRequest request,HttpServletResponse response) {
    	String url="";
		try {
			url = URLEncoder.encode("http://" + request.getServerName()  + ":"  + request.getServerPort(),"utf8");
		} catch (UnsupportedEncodingException e) {
		}
		System.out.println(url);
        //return new ModelAndView("redrect:" + SSO_URL + "?redirect_url=" + url);
		try {
			response.sendRedirect(SSO_URL+ "?redirect_url=" + url);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView oldLogin(HttpServletRequest request) {
        return new ModelAndView("manage/login");
    }
    
    /**
     * 用户登录界面
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/ssologin", method = RequestMethod.GET)
    public ModelAndView ssologin(HttpServletRequest request) {
    	Cookie[] cookies = request.getCookies();
    	for(Cookie ele : cookies){
    		System.out.println(ele.getName() + "  " + ele.getValue());
    	}
        return new ModelAndView("admin/app/list");
    }

    /**
     * 用户登录
     *
     * @param userName 用户名
     * @param password 密码
     * @param isAdmin  是否勾选超级管理员选项,1是0否
     * @return
     */
    @RequestMapping(value = "/loginIn", method = RequestMethod.POST)
    public ModelAndView loginIn(HttpServletRequest request,
                                HttpServletResponse response, Model model, String userName, String password, boolean isAdmin) {

        // 登录结果
        LoginResult loginResult = new LoginResult();
        loginResult.setAdminEnum((isAdmin == true ? AdminEnum.IS_ADMIN : AdminEnum.NOT_ADMIN));
        loginResult.setLoginEnum(LoginEnum.LOGIN_WRONG_USER_OR_PASSWORD);

        AppUser userModel = null;
        if (userName.contains("@")){
        	userName = userName.substring(0,userName.indexOf("@"));
        }
        if (ConstUtils.SUPER_ADMIN_NAME.equals(userName)) {
            userModel = userService.getByName(userName);
            if (userModel != null && ConstUtils.SUPER_ADMIN_PASS.equals(password)) {
                loginResult.setLoginEnum(LoginEnum.LOGIN_SUCCESS);
            } else {
                loginResult.setLoginEnum(LoginEnum.LOGIN_WRONG_USER_OR_PASSWORD);
            }
        }
        else{
        	//非admin用户
            if (LoginUtil.passportCheck(userName, password)) {
                //发送请求验证账户密码
                String resp = HttpPostUtil.sendHttpPostRequest(ConstUtils.LOGIN_URL,  "data="+ JSON.toJSONString(new User(userName, password)));
                LoginResponse loginResponse = JSON.parseObject(resp, LoginResponse.class);

                //登陆成功
                if(loginResponse.getRet() == 0) {
                    userModel = userService.getByName(userName);

                    //未注册的用户自动注册
                    if(userModel == null) {
                    	String userString = HttpPostUtil.sendHttpGetRequest(ConstUtils.USER_INFO_URL, "id=" + loginResponse.getUser_id());
                        UserDetail userDetail = JSON.parseObject(userString,UserDetail.class);
                        AppUser newUser = new AppUser(userName, userDetail.getName(), userDetail.getEmail(), userDetail.getPhone(), AppUserTypeEnum.REGULAR_USER.value());
                        SuccessEnum successEnum = userService.save(newUser);
                        if(SuccessEnum.SUCCESS.value() == successEnum.value()) {
                            userModel = userService.getByName(userName);
                        } else {  //自动注册失败
                            model.addAttribute("success", LoginEnum.LOGIN_USER_NOT_EXIST);
                            return new ModelAndView();
                        }
                    }
                    if (isAdmin) {
                        if (AppUserTypeEnum.ADMIN_USER.value().equals(userModel.getType())) {
                            loginResult.setLoginEnum(LoginEnum.LOGIN_SUCCESS);
                        } else {
                            loginResult.setLoginEnum(LoginEnum.LOGIN_NOT_ADMIN);
                        }
                    } else {
                        loginResult.setLoginEnum(LoginEnum.LOGIN_SUCCESS);
                    }
                } else {
                    //非法的用户名或密码
                    loginResult.setLoginEnum(LoginEnum.LOGIN_WRONG_USER_OR_PASSWORD);
                }
            }
        }

        // 登录成功写入登录状态
        if (loginResult.getLoginEnum().equals(LoginEnum.LOGIN_SUCCESS)) {
            userLoginStatusService.addLoginStatus(request, response, userModel.getId().toString());
        }
        model.addAttribute("success", loginResult.getLoginEnum().value());
        model.addAttribute("admin", loginResult.getAdminEnum().value());
        return new ModelAndView();
    }

    /**
     * 用户注销
     *
     * @param reqeust
     * @return
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public ModelAndView logout(HttpServletRequest request, HttpServletResponse response) {
        userLoginStatusService.removeLoginStatus(request, response);
        return new ModelAndView("redirect:/manage/login");
    }

}
