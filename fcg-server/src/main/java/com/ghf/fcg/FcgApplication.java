package com.ghf.fcg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FcgApplication {

    public static void main(String[] args) {
        SpringApplication.run(FcgApplication.class, args);
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  🔗 Knife4j 文档: http://localhost:8080/doc.html");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println();
    }

}
