package com.example.fcmmembermicroservice.mapper;

import com.example.fcmspi.model.RefreshTokenEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 刷新令牌表 Mapper（t_refresh_token，audience=member 段）
 * 说明：user-api 与 member-api 共用同一张表，以 audience 列隔离；
 *       刷新令牌一次性轮换——刷新成功删除旧记录写入新记录，登出/强制下线删除记录即主动失效
 */
@Mapper
public interface RefreshTokenMapper {

    @Insert("insert into t_refresh_token (audience, user_id, token, expire_time) " +
            "values (#{audience}, #{userId}, #{token}, #{expireTime})")
    int insert(RefreshTokenEntity entity);

    /** 按令牌查询（校验刷新令牌有效性；过期记录由查询后 Java 侧比较） */
    @Select("select id, audience, user_id as userId, token, expire_time as expireTime, " +
            "create_time as createTime from t_refresh_token " +
            "where audience = #{audience} and token = #{token}")
    RefreshTokenEntity findByToken(@Param("audience") String audience, @Param("token") String token);

    /** 删除指定令牌（登出/刷新轮换时删除旧记录） */
    @Delete("delete from t_refresh_token where audience = #{audience} and token = #{token}")
    int deleteByToken(@Param("audience") String audience, @Param("token") String token);

    /** 删除某用户全部刷新令牌（强制下线/改密/禁用） */
    @Delete("delete from t_refresh_token where audience = #{audience} and user_id = #{userId}")
    int deleteByUserId(@Param("audience") String audience, @Param("userId") Long userId);

    /** 清理已过期记录（登录/刷新时顺带执行，避免表无限膨胀） */
    @Delete("delete from t_refresh_token where expire_time < now()")
    int deleteExpired();
}
