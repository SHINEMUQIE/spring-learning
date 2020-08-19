package com.muqie.springEvent;

import org.springframework.context.ApplicationEvent;

/**
 * Mail事件，继承自ApplicationEvent
 */
public class SpringMailEvent extends ApplicationEvent {

	private String content;

	/**
	 * Create a new ApplicationEvent.
	 *
	 * @param source the object on which the event initially occurred (never {@code null})
	 */
	public SpringMailEvent(Object source) {
		super(source);
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
