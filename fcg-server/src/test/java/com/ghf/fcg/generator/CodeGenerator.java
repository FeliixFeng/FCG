package com.ghf.fcg.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

public class CodeGenerator {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/fcg?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "your_password";

    public static void main(String[] args) {
        generateSystemModule();
        generateMedicineModule();
        generateHealthModule();
    }

    public static void generateSystemModule() {
        String projectPath = System.getProperty("user.dir");

        FastAutoGenerator.create(DB_URL, DB_USERNAME, DB_PASSWORD)
                .globalConfig(builder -> builder
                        .author("FeliixFeng")
                        .outputDir(projectPath + "/src/main/java")
                        .disableOpenDir())
                .packageConfig(builder -> builder
                        .parent("com.ghf.fcg")
                        .moduleName("system")
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .xml("mapper"))
                .strategyConfig(builder -> builder
                        .addInclude("sys_family", "sys_user")
                        .addTablePrefix("sys_")
                        .entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .logicDeleteColumnName("deleted"))
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }

    public static void generateMedicineModule() {
        String projectPath = System.getProperty("user.dir");

        FastAutoGenerator.create(DB_URL, DB_USERNAME, DB_PASSWORD)
                .globalConfig(builder -> builder
                        .author("FeliixFeng")
                        .outputDir(projectPath + "/src/main/java")
                        .disableOpenDir())
                .packageConfig(builder -> builder
                        .parent("com.ghf.fcg")
                        .moduleName("medicine")
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .xml("mapper"))
                .strategyConfig(builder -> builder
                        .addInclude("med_medicine", "med_plan", "med_record")
                        .addTablePrefix("med_")
                        .entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .logicDeleteColumnName("deleted"))
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }

    public static void generateHealthModule() {
        String projectPath = System.getProperty("user.dir");

        FastAutoGenerator.create(DB_URL, DB_USERNAME, DB_PASSWORD)
                .globalConfig(builder -> builder
                        .author("FeliixFeng")
                        .outputDir(projectPath + "/src/main/java")
                        .disableOpenDir())
                .packageConfig(builder -> builder
                        .parent("com.ghf.fcg")
                        .moduleName("health")
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .xml("mapper"))
                .strategyConfig(builder -> builder
                        .addInclude("health_vital", "health_report")
                        .addTablePrefix("health_")
                        .entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .logicDeleteColumnName("deleted"))
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }
}
