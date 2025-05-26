package kr.hhplus.be.server.infrastructure.config;

import kr.hhplus.be.server.infrastructure.interceptor.PerformanceInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PerformanceInterceptor performanceInterceptor;

    @Autowired
    public WebConfig(PerformanceInterceptor performanceInterceptor) {
        this.performanceInterceptor = performanceInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(performanceInterceptor)
                .addPathPatterns("/**");  // 모든 엔드포인트에 적용
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
