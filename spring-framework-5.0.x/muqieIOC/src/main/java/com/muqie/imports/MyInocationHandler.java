package com.muqie.imports;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class MyInocationHandler implements InvocationHandler {
	Object target;

	public MyInocationHandler(Object target){
		this.target = target;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("我是代理方法！");
		return method.invoke(target,args);
	}
}
