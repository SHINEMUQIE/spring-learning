package com.muqie.test;

import com.muqie.app.Appconfig;
import com.muqie.service.CityService;
import com.muqie.service.Service;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class TestPostProcessor {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext annotationConfigApplicationContext =
				new AnnotationConfigApplicationContext(Appconfig.class);

		annotationConfigApplicationContext.getBean(CityService.class).query();
	}
}
