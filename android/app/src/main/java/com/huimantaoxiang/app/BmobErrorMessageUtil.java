package com.huimantaoxiang.app;

public final class BmobErrorMessageUtil {

    private BmobErrorMessageUtil() {
    }

    public static String toFriendlyMessage(Throwable e) {
        if (e == null) {
            return "操作失败，请稍后重试";
        }

        int code = extractErrorCode(e);
        switch (code) {
            case 101:
                return "账号或密码错误";
            case 202:
                return "该用户名已被注册";
            case 203:
                return "该邮箱已被注册";
            case 204:
                return "邮箱格式不正确";
            case 205:
                return "用户不存在，请先注册";
            case 206:
                return "登录状态已失效，请重新登录";
            case 207:
                return "用户名不能为空";
            case 209:
                return "该手机号已被注册";
            case 210:
                return "旧密码不正确";
            case 211:
                return "请先登录";
            case 901:
                return "网络异常，请检查网络后重试";
            case 902:
                return "请求超时，请稍后重试";
            default:
                String detail = e.getMessage();
                if (detail == null || detail.trim().isEmpty()) {
                    return code > 0 ? "操作失败（错误码：" + code + "）" : "操作失败，请稍后重试";
                }
                return code > 0 ? "操作失败（错误码：" + code + "）：" + detail : detail;
        }
    }

    private static int extractErrorCode(Throwable e) {
        try {
            Object value = e.getClass().getMethod("getErrorCode").invoke(e);
            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (Exception ignored) {
            // Ignore reflection failures and fallback to generic message.
        }
        return -1;
    }
}


