package com.muqie.app;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.stereotype.Component;

//@Component
public class MuqieFactory implements BeanFactoryPostProcessor {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		GenericBeanDefinition index = (GenericBeanDefinition) beanFactory.getBeanDefinition("userService");
		// 给构造函数得属性添加一个值
		index.getConstructorArgumentValues().addGenericArgumentValue("com.muqie.service.Muqie");
	}
}
