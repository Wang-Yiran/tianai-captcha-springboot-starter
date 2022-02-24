package cloud.tianai.captcha.autoconfiguration;


import cloud.tianai.captcha.aop.CaptchaAdvisor;
import cloud.tianai.captcha.aop.CaptchaInterceptor;
import cloud.tianai.captcha.slider.LocalCacheSliderCaptchaApplication;
import cloud.tianai.captcha.slider.RedisCacheSliderCaptchaApplication;
import cloud.tianai.captcha.slider.SliderCaptchaApplication;
import cloud.tianai.captcha.template.slider.*;
import cloud.tianai.captcha.template.slider.validator.BasicCaptchaTrackValidator;
import cloud.tianai.captcha.template.slider.validator.SliderCaptchaValidator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @Author: 天爱有情
 * @Date 2020/5/29 9:49
 * @Description 滑块验证码自动装配
 */
@Order
@Configuration
@EnableConfigurationProperties(SliderCaptchaProperties.class)
public class SliderCaptchaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResourceStore resourceStore() {
        return new DefaultResourceStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public SliderCaptchaResourceManager sliderCaptchaResourceManager(ResourceStore resourceStore) {
        return new DefaultSliderCaptchaResourceManager(resourceStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public SliderCaptchaTemplate sliderCaptchaTemplate(SliderCaptchaProperties prop,
                                                       SliderCaptchaResourceManager captchaResourceManager) {
        SliderCaptchaTemplate template = new StandardSliderCaptchaTemplate(captchaResourceManager, prop.getInitDefaultResource());
        GenerateParam generateParam = GenerateParam.builder()
                .backgroundFormatName(prop.getBackgroundFormatName())
                .sliderFormatName(prop.getSliderFormatName())
                .obfuscate(prop.getObfuscate())
                .build();
        // 增加缓存处理
        return new CacheSliderCaptchaTemplate(template, generateParam, prop.getCacheSize(), prop.getWaitTime(), prop.getPeriod());
    }

    @Bean
    @ConditionalOnMissingBean
    public SliderCaptchaValidator sliderCaptchaValidator() {
        return new BasicCaptchaTrackValidator();
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean
    public CaptchaInterceptor captchaInterceptor() {
        return new CaptchaInterceptor();
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean
    public CaptchaAdvisor captchaAdvisor(CaptchaInterceptor interceptor) {
        return new CaptchaAdvisor(interceptor);
    }


    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean
    public CacheCaptchaTemplateListener captchaTemplateListener() {
        return new CacheCaptchaTemplateListener();
    }


    /**
     * @Author: 天爱有情
     * @date 2020/10/27 14:06
     * @Description RedisCacheSliderCaptchaApplication
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringRedisTemplate.class)
    @Import({RedisAutoConfiguration.class})
    @AutoConfigureAfter({RedisAutoConfiguration.class})
    public static class RedisSliderCaptchaApplication {

        @Bean
        @ConditionalOnBean(StringRedisTemplate.class)
        @ConditionalOnMissingBean(SliderCaptchaApplication.class)
        public SliderCaptchaApplication redis(StringRedisTemplate redisTemplate,
                                              SliderCaptchaTemplate template,
                                              SliderCaptchaValidator sliderCaptchaValidator,
                                              SliderCaptchaProperties properties) {
            return new RedisCacheSliderCaptchaApplication(redisTemplate, template, sliderCaptchaValidator, properties);
        }
    }

    /**
     * @Author: 天爱有情
     * @date 2020/10/27 14:06
     * @Description LocalCacheSliderCaptchaApplication
     */

    @Configuration(proxyBeanMethods = false)
    @AutoConfigureAfter({RedisSliderCaptchaApplication.class})
    @Import({RedisSliderCaptchaApplication.class})
    public static class LocalSliderCaptchaApplication {

        @Bean
        @ConditionalOnMissingBean(SliderCaptchaApplication.class)
        public SliderCaptchaApplication local(SliderCaptchaTemplate template,
                                              SliderCaptchaValidator sliderCaptchaValidator,
                                              SliderCaptchaProperties properties) {
            return new LocalCacheSliderCaptchaApplication(template, sliderCaptchaValidator, properties);
        }
    }

}
