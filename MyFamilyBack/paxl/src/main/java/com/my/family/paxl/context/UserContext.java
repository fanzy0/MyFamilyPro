package com.my.family.paxl.context;

import com.my.family.paxl.domain.entity.UserDO;

/**
 * 用户上下文，基于 ThreadLocal 存储当前请求的登录用户
 * 由鉴权拦截器在请求开始时写入，请求结束后清理
 *
 * @author ai
 * @date 2026/03/13
 */
public class UserContext {

    private static final ThreadLocal<UserDO> USER_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 设置当前请求的登录用户
     *
     * @param user 用户数据对象
     */
    public static void setCurrentUser(UserDO user) {
        USER_HOLDER.set(user);
    }

    /**
     * 获取当前请求的登录用户
     *
     * @return 当前登录用户，未登录时返回 null
     */
    public static UserDO getCurrentUser() {
        return USER_HOLDER.get();
    }

    /**
     * 获取当前登录用户的主键 ID
     *
     * @return 用户 ID，未登录时返回 null
     */
    public static Long getCurrentUserId() {
        UserDO user = USER_HOLDER.get();
        return user != null ? user.getId() : null;
    }

    /**
     * 清理当前线程的用户上下文，防止线程池复用时数据污染
     * 必须在请求结束后调用（由拦截器的 afterCompletion 保证）
     */
    public static void clear() {
        USER_HOLDER.remove();
    }
}
