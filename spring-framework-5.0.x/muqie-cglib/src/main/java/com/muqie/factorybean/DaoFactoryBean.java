package com.muqie.factorybean;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

/**
 * 如果类实现了 FactoryBean 接口，需实现该接口的三个方法。
 * 并且在 spring 容器中会产生两个对象，一个是这个类对象，既当前对象，用"&" + 当前类名 来描述
 * 另外一个是：getObject() 返回的对象。
 */
//@Component("daoFactoryBean")
public class DaoFactoryBean implements FactoryBean {

	public void testBean(){
		System.out.println("testBean");
	}

	@Override
	public Object getObject() throws Exception {
		return new TempDaoFactoryBean();
	}

	@Override
	public Class<?> getObjectType() {
		return TempDaoFactoryBean.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
