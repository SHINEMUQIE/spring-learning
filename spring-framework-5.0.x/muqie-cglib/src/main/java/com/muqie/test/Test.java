package com.muqie.test;

import com.muqie.app.Appconfig;
import com.muqie.factorybean.DaoFactoryBean;
import com.muqie.factorybean.TempDaoFactoryBean;
import com.muqie.service.IndexService;
import com.muqie.service.OrderService;
import com.muqie.service.UserService;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {

	public static void main(String[] args) {

		AnnotationConfigApplicationContext annotationConfigApplicationContext =
				new AnnotationConfigApplicationContext(Appconfig.class);

//		annotationConfigApplicationContext.scan();
		//annotationConfigApplicationContext.getBean(UserService.class).query();
		/*DaoFactoryBean daoFactoryBean = (DaoFactoryBean) annotationConfigApplicationContext.getBean("&daoFactoryBean");
		daoFactoryBean.testBean();

		TempDaoFactoryBean tempDaoFactoryBean = (TempDaoFactoryBean) annotationConfigApplicationContext.getBean("daoFactoryBean");
		tempDaoFactoryBean.test();*/



		//OrderService userService = (OrderService) annotationConfigApplicationContext.getBean("orderService");
		//userService.query();
//		/System.out.println(annotationConfigApplicationContext.getBean(UserService.class).getClass().getName());


//java -classpath "D:\root\toolsSofts\Java\jdk1.8.0\lib\sa-jdi.jar" sun.jvm.hotspot.HSDB
		/*Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(UserService.class);
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
		enhancer.setCallback(new MuqieMethodInterceptor());
		UserService userService1 = (UserService) enhancer.create();
		userService1.query();

		try{
			System.in.read();
		}catch (Exception e){

		}*/

	}



}
