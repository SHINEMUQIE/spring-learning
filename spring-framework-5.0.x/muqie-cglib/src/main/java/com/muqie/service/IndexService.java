package com.muqie.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

//、@Service
//@Component
public class IndexService {

	private String name;

	public IndexService(){

	}

	public void query(){
		System.out.println("sdadsa");
	}

	public String getName() {
		return name;
	}

	@Required
	public void setName(String name) {
		this.name = name;
	}
}
