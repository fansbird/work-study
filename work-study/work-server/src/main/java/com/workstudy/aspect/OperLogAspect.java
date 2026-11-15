package com.workstudy.aspect;

import com.alibaba.fastjson.JSON;
import com.workstudy.annotation.Log;
import com.workstudy.context.BaseContext;
import com.workstudy.entity.SysOperLog;
import com.workstudy.entity.SysUser;
import com.workstudy.mapper.SysOperLogMapper;
import com.workstudy.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志切面：拦截标注 @Log 的 Controller 方法，记录到 sys_oper_log。
 * 全程 try-catch 包裹，绝不因记录日志而影响主业务。
 */
@Aspect
@Component
@Slf4j
public class OperLogAspect {

    /**
     * 请求参数最大记录长度
     */
    private static final int MAX_PARAM_LENGTH = 2000;

    /**
     * 异常信息最大记录长度
     */
    private static final int MAX_ERROR_LENGTH = 2000;

    @Autowired
    private SysOperLogMapper sysOperLogMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 切入点：所有标注 @Log 注解的方法
     */
    @Pointcut("@annotation(com.workstudy.annotation.Log)")
    public void operLogPointCut() {
    }

    /**
     * 环绕通知：记录方法名、URL、请求方式、参数、操作人、IP、耗时、正常/异常
     */
    @Around("operLogPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            // 主业务抛异常：记录异常日志后原样抛出，不改变原有异常语义
            recordLog(joinPoint, ex, System.currentTimeMillis() - start);
            throw ex;
        }
        // 主业务正常返回
        recordLog(joinPoint, null, System.currentTimeMillis() - start);
        return result;
    }

    /**
     * 组装并写入操作日志（异常不得外溢）
     */
    private void recordLog(ProceedingJoinPoint joinPoint, Throwable ex, long costTime) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Log logAnnotation = method.getAnnotation(Log.class);
            String title = logAnnotation == null ? "" : logAnnotation.title();

            HttpServletRequest request = currentRequest();

            SysOperLog operLog = new SysOperLog();
            operLog.setTitle(title);
            operLog.setMethod(joinPoint.getTarget().getClass().getName() + "." + method.getName() + "()");
            operLog.setRequestMethod(request == null ? null : request.getMethod());
            operLog.setRequestUrl(request == null ? null : request.getRequestURI());
            operLog.setRequestParam(buildParam(joinPoint.getArgs()));
            operLog.setOperIp(getIp(request));
            operLog.setOperTime(LocalDateTime.now());
            operLog.setCostTime(costTime);

            // 操作人：id 取自 ThreadLocal 上下文，姓名从数据库查询
            Long userId = BaseContext.getCurrentId();
            operLog.setOperUserId(userId);
            operLog.setOperUserName(getUserName(userId));

            if (ex == null) {
                operLog.setStatus(1);
            } else {
                operLog.setStatus(0);
                operLog.setErrorMsg(truncate(stackTraceToString(ex), MAX_ERROR_LENGTH));
            }

            sysOperLogMapper.insert(operLog);
        } catch (Exception e) {
            // 记录日志失败绝不能影响主业务
            log.error("操作日志记录失败：{}", e.getMessage());
        }
    }

    /**
     * 获取当前请求对象
     */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    /**
     * 序列化请求参数（fastjson），过滤 Servlet 对象与上传文件，超长截断
     */
    private String buildParam(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> paramList = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof ServletRequest || arg instanceof ServletResponse
                    || arg instanceof MultipartFile || arg instanceof MultipartFile[]) {
                continue;
            }
            paramList.add(arg);
        }
        if (paramList.isEmpty()) {
            return null;
        }
        try {
            return truncate(JSON.toJSONString(paramList), MAX_PARAM_LENGTH);
        } catch (Exception e) {
            return truncate(String.valueOf(paramList), MAX_PARAM_LENGTH);
        }
    }

    /**
     * 获取操作人IP
     */
    private String getIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            return index > 0 ? ip.substring(0, index).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 查询操作人姓名（查询失败不影响日志记录）
     */
    private String getUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            SysUser sysUser = sysUserMapper.getById(userId);
            return sysUser == null ? null : sysUser.getName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 异常堆栈摘要
     */
    private String stackTraceToString(Throwable ex) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        ex.printStackTrace(printWriter);
        printWriter.flush();
        return stringWriter.toString();
    }

    /**
     * 字符串截断
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
