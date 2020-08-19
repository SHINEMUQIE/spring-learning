package com.muqie.anno;

import com.muqie.app.RedisHttpSessionConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(RedisHttpSessionConfiguration.class)
@Configuration
public @interface EnableRedisHttpSession {

	//Session默认过期时间,秒为单位，默认30分钟
	int maxInactiveIntervalInSeconds() default 10000;

	String keyPrefix() default "";
}
