package com.muqie.springEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MailBean {

	@Autowired
	ApplicationContext applicationContext;



	public void sendMail(){

		// 触发邮件事件
		// applicationContext 有哪些事件？ start、refresh、close、stop
		applicationContext.publishEvent(new SpringMailEvent(applicationContext));
	}
}
