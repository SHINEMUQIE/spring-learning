package com.muqie.test;

import com.muqie.app.AppConfig;
import com.muqie.beanFactoryProcessor.MyBeanFactoryProcessor;
import com.muqie.dao.Dao;
import com.muqie.dao.IndexDao;
import com.muqie.dao.IndexDao1;
import com.muqie.dao.IndexDao2;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.CallbackFilter;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {

	public static void main(String[] args) {
		//注解配置应用上下文，
		AnnotationConfigApplicationContext annotationConfigApplicationContext =
				new AnnotationConfigApplicationContext();
		annotationConfigApplicationContext.register(AppConfig.class);

		//annotationConfigApplicationContext.addBeanFactoryPostProcessor(new MyBeanFactoryProcessor());

		//refresh()方法初始化spring的环境
		annotationConfigApplicationContext.refresh();
		//annotationConfigApplicationContext.getBean(IndexDao1.class).query();
		Dao dao = (Dao) annotationConfigApplicationContext.getBean("indexDao");
		//IndexDao dao1 = annotationConfigApplicationContext.getBean(IndexDao.class);
		//System.out.println(dao.hashCode() + "------" + dao1.hashCode());
		//dao.query();

		// CGLIB 代理
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(IndexDao.class);
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
		// 对代理对象的所有方法进行拦截
		enhancer.setCallback(new TestMethodCallback());
		IndexDao indexDao = (IndexDao) enhancer.create();
		indexDao.query();
	}
}
