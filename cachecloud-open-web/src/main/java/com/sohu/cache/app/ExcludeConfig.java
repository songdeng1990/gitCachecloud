package com.sohu.cache.app;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ExcludeConfig {

	@Bean(name="cacheCloudDB")
	@Primary
	@ConfigurationProperties(prefix = "cachecloud.db")
	public DataSource cachecloudDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name="quartzDB")
	@ConfigurationProperties(prefix = "quartz.db")
	public DataSource quartzDataSource() {
		return DataSourceBuilder.create().build();
	}

}
