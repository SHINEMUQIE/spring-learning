package com.muqie.beanPostProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;


//@Component
public class TestBeanPostProcessor1 implements BeanPostProcessor, PriorityOrdered {
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

		if(beanName.equals("indexDao"))
			System.out.println("BeforeInitialization1");
		/**
		 * 在此处可以用代理对象处理bean
		 */
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

		if(beanName.equals("indexDao"))
			System.out.println("AfterInitialization1");
		return bean;
	}

	@Override
	public int getOrder() {
		return 102;
	}
}
