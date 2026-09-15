package com.hda.integracion.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Registra como beans de Spring las clases del modulo domain/usecase, que a proposito
 * no tienen ninguna anotacion de framework. Mismo mecanismo que en trabajos-service
 * (ver su propia UseCasesConfig).
 */
@Configuration
@ComponentScan(basePackages = "com.hda.integracion.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {
}
