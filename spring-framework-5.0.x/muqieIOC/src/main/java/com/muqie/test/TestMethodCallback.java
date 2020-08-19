package com.muqie.test;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class TestMethodCallback implements MethodInterceptor {
	/**
	 *
	 * @param o		代理对象
	 * @param method	需要执行的方法
	 * @param objects	参数
	 * @param methodProxy
	 * @return
	 * @throws Throwable
	 */
	@Override
	public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
		System.out.println("method---");

		return methodProxy.invokeSuper(o, objects);
	}
}
