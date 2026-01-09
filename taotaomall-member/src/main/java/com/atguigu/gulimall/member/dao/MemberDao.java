package com.atguigu.gulimall.member.dao;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author paopao
 * @email 1903980165@qq.com
 * @date 2025-03-17 08:44:57
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
