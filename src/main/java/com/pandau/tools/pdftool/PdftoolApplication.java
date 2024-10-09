package com.pandau.tools.pdftool;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.pandau.tools.pdftool.repository.mapper"})
public class PdftoolApplication {

	public static void main(String[] args) {
		SpringApplication.run(PdftoolApplication.class, args);
	}

}
