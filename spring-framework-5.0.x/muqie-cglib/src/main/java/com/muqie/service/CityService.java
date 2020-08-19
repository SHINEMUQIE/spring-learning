package com.muqie.service;

import com.muqie.dao.MuqieDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CityService implements Service {

	public CityService(){
		System.out.println("service");
	}

	@Autowired
	public CityService(MuqieDao dao){
		System.out.println("----------" + dao);
	}

	@Override
	public void query() {

	}
}
