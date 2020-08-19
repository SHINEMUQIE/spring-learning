package com.muqie.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//@Component
public class UserService {

	//Class clazz;
	@Autowired
	private OrderService orderService;


	/*public UserService(){
		System.out.println("aaaaa");
	}*/

	/*public UserService(OrderService orderService){
		//this.clazz = clazz;
		System.out.println("init--userService");
	}*/

	/*public void setOrderService(OrderService orderService) {
		this.orderService = orderService;
	}*/

	/*public void query(){

		System.out.println("clazz");
	}*/

	/*public void query2(){
		query();
		System.out.println("USER");
	}*/

}
