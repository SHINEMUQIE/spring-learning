package com.muqie.test;

import com.muqie.muqie.Appconfig;
import com.muqie.springEvent.MailBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext annotationConfigApplicationContext =
				new AnnotationConfigApplicationContext(Appconfig.class);
		annotationConfigApplicationContext.getBean(MailBean.class).sendMail();
		//annotationConfigApplicationContext.start();
	}
}
