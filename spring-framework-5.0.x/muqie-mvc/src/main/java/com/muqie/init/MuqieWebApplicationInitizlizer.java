package com.muqie.init;

import org.springframework.web.WebApplicationInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class MuqieWebApplicationInitizlizer implements WebApplicationInitializer {
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		// 初始化 spring 和 springWeb 环境
		System.out.println("=========================");
	}
}
