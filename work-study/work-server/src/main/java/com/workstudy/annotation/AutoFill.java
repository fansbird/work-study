package com.workstudy.annotation;

import com.workstudy.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动填充注解，用于在新增或更新实体时，自动填充创建人、修改人、创建时间、修改时间
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    //数据库操作类型:insert/update
    OperationType value();

}
