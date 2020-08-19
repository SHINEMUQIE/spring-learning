package com.muqie.app;

import com.muqie.anno.EnableMuqie;
import com.muqie.dao.IndexDao;
import com.muqie.dao.IndexDao1;
import com.muqie.imports.MyImportSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@ComponentScan("com.muqie")
@EnableMuqie
@Configuration
public class AppConfig {

	@Bean
	public IndexDao IndexDao(){
		indexDao1();
		return new IndexDao();
	}

	@Bean
	public IndexDao1 indexDao1(){return new IndexDao1();}
}
