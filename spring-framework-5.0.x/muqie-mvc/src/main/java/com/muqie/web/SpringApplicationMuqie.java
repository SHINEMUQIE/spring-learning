package com.muqie.web;

import com.muqie.config.Appconfig;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.File;

public class SpringApplicationMuqie {

	public static void run() throws LifecycleException {


		AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext =
				new AnnotationConfigWebApplicationContext();

		annotationConfigWebApplicationContext.register(Appconfig.class);
		annotationConfigWebApplicationContext.refresh();

		File base = new File(System.getProperty("java.io.tmpdir"));

		Tomcat tomcat = new Tomcat();
		tomcat.setPort(8080);

		/**
		 * addWebapp 表示这是一个 web 项目
		 * contextPath 表示tomcat 访问的路径
		 * docBase 表示 web  目录
		 * 因为这里不是一个 web 项目，所以这里不能用 addWebapp
		 */
		//tomcat.addWebapp("/", base.getAbsolutePath());
		 Context rootContext = tomcat.addContext("/",base.getAbsolutePath());

		DispatcherServlet dispatcherServlet = new DispatcherServlet(annotationConfigWebApplicationContext);
		// tomcat 启动过程中就会调用 DispatcherServlet # init
		// DispatcherServlet # init 初始化controller 和 请求映射
		tomcat.addServlet(rootContext, "muqie", dispatcherServlet).setLoadOnStartup(1);

		tomcat.start();
		tomcat.getServer().await();

		/*// 先进行初始化 spring环境
		AnnotationConfigWebApplicationContext annotationConfigWebApplicationContext =
				new AnnotationConfigWebApplicationContext();

		annotationConfigWebApplicationContext.register(Appconfig.class);
		annotationConfigWebApplicationContext.refresh();

		File base = new File(System.getProperty(""));

		Tomcat tomcat = new Tomcat();
		tomcat.setPort(8080);

		Context rootCtx = tomcat.addContext("/",base.getAbsolutePath());

		DispatcherServlet dispatcherServlet = new DispatcherServlet(annotationConfigWebApplicationContext);

		// tomcat 启动过程中会调用 DispatcherServlet 的 init 方法
		// 初始化controller和请求映射
		tomcat.addServlet(rootCtx, "muqie", dispatcherServlet).setLoadOnStartup(1);
		tomcat.start();
		tomcat.getServer().await();*/




	}
}
