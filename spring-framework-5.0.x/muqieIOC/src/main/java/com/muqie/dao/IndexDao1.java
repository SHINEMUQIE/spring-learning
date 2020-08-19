package com.muqie.dao;

import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;


public class IndexDao1 implements Dao{

	public IndexDao1(){
		System.out.println("dao1--init");

	}

	public void query(){
		System.out.println("index1");
	}
}
