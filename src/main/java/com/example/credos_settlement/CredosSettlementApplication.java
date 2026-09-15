package com.example.credos_settlement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CredosSettlementApplication {

  public static void main(String[] args) {
    SpringApplication.run(CredosSettlementApplication.class, args);
  }
}
