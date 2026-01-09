package com.atguigu.gulimall.member.service;

import com.atguigu.common.vo.MemberLoginVo;
import com.atguigu.common.vo.MemberRegistVo;
import com.atguigu.common.vo.SocialUserVo;
import com.atguigu.gulimall.member.exception.PhoneExistException;
import com.atguigu.gulimall.member.exception.UsernameExistException;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.member.entity.MemberEntity;

import java.util.Map;

/**
 * 会员
 *
 * @author paopao
 * @email 1903980165@qq.com
 * @date 2025-03-17 08:44:57
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExistException;

    void checkUserNameUnique(String userName) throws UsernameExistException;

    MemberEntity login(MemberLoginVo vo);


    MemberEntity login(SocialUserVo socialUser) throws Exception;
}

