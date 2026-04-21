package com.ghf.fcg.common.constant;

public class MessageConstant {

    // 登录/注册
    public static final String LOGIN_SUCCESS = "登录成功";
    public static final String LOGIN_FAILED = "账号或密码错误";
    public static final String FAMILY_USERNAME_EXIST = "家庭账号已存在";
    public static final String FAMILY_NOT_EXIST = "家庭不存在";
    public static final String ADMIN_VERIFY_FAILED = "管理员密码验证失败";

    // 成员
    public static final String USER_NOT_EXIST = "成员不存在";
    public static final String USER_NOT_IN_FAMILY = "成员不属于当前家庭";
    public static final String USER_FAMILY_MISMATCH = "成员不存在或不属于当前家庭";
    public static final String NO_PERMISSION = "无权限，仅管理员可操作";
    public static final String NEED_MEMBER_TOKEN = "请先选择成员";

    // 关怀模式
    public static final String CARE_MODE_PARAM_ERROR = "关怀模式参数错误";

    // 药品
    public static final String MEDICINE_NOT_EXIST = "药品不存在";
    public static final String MEDICINE_FAMILY_MISMATCH = "药品不存在或不属于当前家庭";
    public static final String MEDICINE_STOCK_NOT_ENOUGH = "库存不足，请先补充药品";

    // 用药计划
    public static final String PLAN_NOT_EXIST = "用药计划不存在";
    public static final String PLAN_FAMILY_MISMATCH = "用药计划不存在或不属于当前家庭";

    // 服药记录
    public static final String RECORD_NOT_EXIST = "服药记录不存在";
    public static final String RECORD_PLAN_USER_MISMATCH = "记录使用者与计划不一致";
    public static final String RECORD_PLAN_MEDICINE_MISMATCH = "记录药品与计划不一致";

    // 健康
    public static final String VITAL_NOT_EXIST = "体征记录不存在";
    public static final String REPORT_NOT_EXIST = "健康周报不存在";

    // 通用
    public static final String UNAUTHORIZED = "未授权，请先登录";
    public static final String FORBIDDEN = "无权限访问";
    public static final String PARAM_ERROR = "参数错误";
    public static final String OPERATION_SUCCESS = "操作成功";
    public static final String OPERATION_FAILED = "操作失败";
    public static final String SYSTEM_ERROR = "系统异常，请联系管理员";

    // OSS / OCR
    public static final String OCR_FAILED = "OCR识别失败";
    public static final String OSS_CONFIG_MISSING = "OSS配置缺失";
    public static final String OSS_UPLOAD_FAILED = "OSS上传失败";
}
