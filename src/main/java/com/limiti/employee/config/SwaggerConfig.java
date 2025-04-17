package com.limiti.employee.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger配置类
 * @author Limiti
 * @date 2024/11/20
 */
@Configuration
public class SwaggerConfig {
    
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.limiti.employee.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("企业员工管理系统接口文档")
                .description("提供员工管理、部门管理、考勤管理等接口")
                .contact(new Contact("Limiti", "http://github.com/Limiti", "limiti@example.com"))
                .version("1.0")
                .build();
    }
} 