package com.muqie.app;

import com.muqie.anno.EnableRedisHttpSession;
import com.muqie.service.OrderService;
import com.muqie.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@ComponentScan("com.muqie")
//@EnableAspectJAutoProxy
//@EnableRedisHttpSession(maxInactiveIntervalInSeconds=10000, keyPrefix = "adasda")
public class Appconfig {

	/*@Bean
	public UserService userService(){
		return new UserService();
	}

	@Bean
	public OrderService orderService(){
		userService();
		return new OrderService();
	}*/

}
