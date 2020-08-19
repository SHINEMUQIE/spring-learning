package com.muqie.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//@Component
public class OrderService {


	@Autowired
	private UserService userService;

	/*public OrderService(UserService userService){
		this.userService = userService;
	}*/

	/*public void setUserService(UserService userService) {
		this.userService = userService;
	}*/

	/*public void query(){
		System.out.println("orderService");
	}*/

	/*public String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void query(){
		System.out.println("order");
	}*/
}
