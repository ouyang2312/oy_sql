package com.oy.oy_sql.cons;

import java.lang.annotation.*;

/**
 * 不需要逻辑删除，也就是物理删除
 *
 * @author ouyang
 * @createDate 2023/5/22 17:08
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotLogicDelete {



}
