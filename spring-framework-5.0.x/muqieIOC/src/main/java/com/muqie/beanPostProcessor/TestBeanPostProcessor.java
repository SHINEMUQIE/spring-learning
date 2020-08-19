package com.muqie.beanPostProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

/**
 * PriorityOrdered  bean 类加载顺序，值越小先执行
 */

//@Component
public class TestBeanPostProcessor implements BeanPostProcessor, PriorityOrdered, ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

		if(beanName.equals("indexDao"))
			System.out.println("BeforeInitialization");
		/**
		 * 在此处可以用代理对象处理bean
		 */
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

		if(beanName.equals("indexDao"))
			System.out.println("AfterInitialization");
		// applicationContext.getApplicationName("xxx");
		return bean;
	}

	@Override
	public int getOrder() {
		return 103;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		System.out.println(applicationContext);
	}
}
