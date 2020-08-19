package com.muqie.dao;

import com.muqie.imports.MyInocationHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.lang.reflect.Proxy;


public class IndexDao2 implements BeanPostProcessor {


	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

		if(beanName.equals("indexDao")){
			bean = Proxy.newProxyInstance(this.getClass().getClassLoader(),new Class[]{Dao.class},new MyInocationHandler(bean));
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return null;
	}
}
