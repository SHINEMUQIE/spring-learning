package com.muqie.app;

import com.muqie.anno.EnableRedisHttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

//@Configuration
public class RedisHttpSessionConfiguration implements ImportAware {
	public int maxInactiveIntervalInSeconds;

	public String keyPrefix;

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		/**
		 * 获取注解类的注解信息，如注解的属性值
		 * EnableRedisHttpSession.class.getName() 获取注解类的全路径名
		 */
		Map<String, Object> map = importMetadata.getAnnotationAttributes(EnableRedisHttpSession.class.getName());
		// 将注解中的信息转换成 map 数组
		AnnotationAttributes attrs = AnnotationAttributes.fromMap(map);
		this.keyPrefix = attrs.getString("keyPrefix");
		this.maxInactiveIntervalInSeconds = attrs.getNumber("maxInactiveIntervalInSeconds");

	}
}
