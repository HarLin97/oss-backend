package com.berry.oss.config;

import com.berry.oss.security.AuthoritiesConstants;
import com.berry.oss.security.FilterConfigurer;
import com.berry.oss.security.filter.TokenProvider;
import com.berry.oss.security.interceptor.AccessProvider;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * @author xueancao
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Resource
    private AuthenticationManagerBuilder authenticationManagerBuilder;

    @Resource
    private UserDetailsService userDetailsService;

    @Resource
    private CorsFilter corsFilter;

    @Resource
    Environment environment;

    private final TokenProvider tokenProvider;

    private AccessProvider accessProvider;

    public SecurityConfiguration(TokenProvider tokenProvider, AccessProvider accessProvider) {
        this.tokenProvider = tokenProvider;
        this.accessProvider = accessProvider;
    }

    /**
     * 初始化授权管理器构建器
     */
    @PostConstruct
    public void init() {
        try {
            authenticationManagerBuilder
                    .userDetailsService(userDetailsService)
                    .passwordEncoder(new BCryptPasswordEncoder());
        } catch (Exception e) {
            throw new BeanInitializationException("Security configuration failed", e);
        }
    }

    /**
     * 声明 授权管理器 为bean
     *
     * @return
     * @throws Exception
     */
    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }


    /**
     * 1.OPTIONS 请求放行
     * 2.静态资源放行
     *
     * @param web
     * @throws Exception
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .antMatchers(HttpMethod.OPTIONS, "/**")
                .antMatchers("/i18n/**")
                .antMatchers("/static/**")
                .antMatchers("/webjars/**");
        String[] activeProfiles = environment.getActiveProfiles();
        List<String> profiles = Arrays.asList(activeProfiles);
        if (profiles.contains("dev") ) {
            web.ignoring()
                    .antMatchers("/v2/api-docs")
                    .antMatchers("/swagger-resources/**")
                    .antMatchers("/swagger**");
        }
    }

    /**
     * 禁用默认csrf，自定义登录验证过滤器
     * 防止将头信息添加到响应中
     * 设置session创建策略-从不创建session
     * 定义api访问权限
     *
     * @param http
     * @throws Exception
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .csrf()
                .disable()
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling()
                .and()
                .headers()
                .frameOptions()
                .disable()
                .and()
                .sessionManagement()
                // 无状态，不保存任何 session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/auth/login").permitAll()
                // 放开 object get api
                .antMatchers("/ajax/bucket/file/**").permitAll()

                .antMatchers("/ajax/register").permitAll()
                .antMatchers("/ajax/activate").permitAll()
                .antMatchers("/ajax/account/password/init").permitAll()
                .antMatchers("/ajax/account/password/finish").permitAll()
                .antMatchers("/ajax/worm_strategy/**").permitAll()
                .antMatchers("/ajax/**").authenticated()
                .antMatchers("/management/health").permitAll()
                .antMatchers("/management/info").permitAll()
                .antMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
                .and()
                .apply(securityConfigurerAdapter());

    }

    private FilterConfigurer securityConfigurerAdapter() {
        return new FilterConfigurer(this.tokenProvider, this.accessProvider);
    }
}
