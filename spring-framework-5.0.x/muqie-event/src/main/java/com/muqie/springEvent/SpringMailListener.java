package com.muqie.springEvent;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Mail监听类,用于监听SpringMailEvent时间
 * ApplicationEvent 为事件
 */
@Component
public class SpringMailListener implements ApplicationListener<SpringMailEvent> {


	@Override
	public void onApplicationEvent(SpringMailEvent event) {
		System.out.println("mail  send");
	}
}
