package org.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling // 스케줄링 활성화
@EnableJpaAuditing // 메모 생성, 수정일 자동 관리
@SpringBootApplication
public class OjoApplication {
	public static void main(String[] args) {
		SpringApplication.run(OjoApplication.class, args);
	}


}