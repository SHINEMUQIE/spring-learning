package com.muqie.dao;

import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;


public class IndexDao implements Dao{


	/*@PostConstruct
	public void init(){
		System.out.println("init");
	}*/

	public void query(){
		System.out.println("query");
	}
}
