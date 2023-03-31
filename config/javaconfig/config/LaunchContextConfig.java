package xxxxxx.yyyyyy.zzzzzz.projectName.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.ExitCodeMapper;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.launch.support.SimpleJvmExitCodeMapper;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.validator.SpringValidator;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.terasoluna.batch.converter.JobParametersConverterImpl;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@PropertySource(value = "classpath:batch-application.properties")
public class LaunchContextConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean(destroyMethod = "close")
    public BasicDataSource adminDataSource(@Value("${admin.jdbc.driver:#{null}}") String driverClassName,
                                           @Value("${admin.jdbc.url:#{null}}") String url,
                                           @Value("${admin.jdbc.username:#{null}}") String username,
                                           @Value("${admin.jdbc.password:#{null}}") String password) {
        final BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaxTotal(10);
        dataSource.setMinIdle(1);
        dataSource.setMaxWaitMillis(5000);
        dataSource.setDefaultAutoCommit(false);
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager adminTransactionManager(@Qualifier("adminDataSource") BasicDataSource adminDataSource) {
        final DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
        transactionManager.setDataSource(adminDataSource);
        transactionManager.setRollbackOnCommitFailure(true);
        return transactionManager;
    }

    @Bean
    public JobRepository jobRepository(@Qualifier("adminDataSource") DataSource adminDataSource,
                                       @Qualifier("adminTransactionManager") PlatformTransactionManager adminTransactionManager) throws Exception {
        final JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
        jobRepositoryFactoryBean.setDataSource(adminDataSource);
        jobRepositoryFactoryBean.setTransactionManager(adminTransactionManager);
        jobRepositoryFactoryBean.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        jobRepositoryFactoryBean.afterPropertiesSet();
        return jobRepositoryFactoryBean.getObject();
    }

    @Bean
    public JobRegistry jobRegistry() {
        return new MapJobRegistry();
    }

    @Bean
    public JobExplorer jobExplorer(@Qualifier("adminDataSource") DataSource adminDataSource,
                                   @Qualifier("adminTransactionManager") PlatformTransactionManager adminTransactionManager) throws Exception {
        final JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
        jobExplorerFactoryBean.setDataSource(adminDataSource);
        jobExplorerFactoryBean.setTransactionManager(adminTransactionManager);
        jobExplorerFactoryBean.afterPropertiesSet();
        return jobExplorerFactoryBean.getObject();
    }

    @Bean
    public JobParametersConverter jobParametersConverter(@Qualifier("adminDataSource") DataSource adminDataSource) {
        return new JobParametersConverterImpl(adminDataSource);
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) {
        final SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
        simpleJobLauncher.setJobRepository(jobRepository);
        return simpleJobLauncher;
    }

    @Bean
    public JobOperator jobOperator(JobRepository jobRepository, JobRegistry jobRegistry, JobExplorer jobExplorer,
                                   JobParametersConverter jobParametersConverter, JobLauncher jobLauncher) {
        final SimpleJobOperator simpleJobOperator = new SimpleJobOperator();
        simpleJobOperator.setJobRepository(jobRepository);
        simpleJobOperator.setJobRegistry(jobRegistry);
        simpleJobOperator.setJobExplorer(jobExplorer);
        simpleJobOperator.setJobParametersConverter(jobParametersConverter);
        simpleJobOperator.setJobLauncher(jobLauncher);
        return simpleJobOperator;
    }

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
        final JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor = new JobRegistryBeanPostProcessor();
        jobRegistryBeanPostProcessor.setJobRegistry(jobRegistry);
        return jobRegistryBeanPostProcessor;
    }

    @Bean
    public ExitCodeMapper exitCodeMapper() {
        final SimpleJvmExitCodeMapper simpleJvmExitCodeMapper = new SimpleJvmExitCodeMapper();
        final Map<String, Integer> exitCodeMapper = new HashMap<>();
        // ExitStatus
        exitCodeMapper.put("NOOP", 0);
        exitCodeMapper.put("COMPLETED", 0);
        exitCodeMapper.put("STOPPED", 255);
        exitCodeMapper.put("FAILED", 255);
        exitCodeMapper.put("UNKNOWN", 255);
        // Customed ExitStatus for tests
        exitCodeMapper.put("COMPLETED_CUSTOM", 200);
        exitCodeMapper.put("STOPPED_CUSTOM", 201);
        exitCodeMapper.put("FAILED_CUSTOM", 202);
        simpleJvmExitCodeMapper.setMapping(exitCodeMapper);
        return simpleJvmExitCodeMapper;
    }

    @Bean
    public DataSourceInitializer dataSourceInitializer(@Qualifier("adminDataSource") DataSource adminDataSource,
                                                       @Value("${data-source.initialize.enabled:false}") boolean enabled,
                                                       @Value("${spring-batch.schema.script:#{null}}") Resource script,
                                                       @Value("${terasoluna-batch.commit.script:#{null}}") Resource commitScript) {
        final DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setDataSource(adminDataSource);
        dataSourceInitializer.setEnabled(enabled);
        ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator(script, commitScript);
        resourceDatabasePopulator.setContinueOnError(true);
        dataSourceInitializer.setDatabasePopulator(resourceDatabasePopulator);
        return dataSourceInitializer;
    }

    @Bean(destroyMethod = "close")
    public BasicDataSource jobDataSource(@Value("${jdbc.driver:#{null}}") String driverClassName,
                                         @Value("${jdbc.url:#{null}}") String url,
                                         @Value("${jdbc.username:#{null}}") String username,
                                         @Value("${jdbc.password:#{null}}") String password) {
        final BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(driverClassName);
        basicDataSource.setUrl(url);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
        basicDataSource.setMaxTotal(10);
        return basicDataSource;
    }

    @Bean
    public PlatformTransactionManager jobTransactionManager(@Qualifier("jobDataSource") DataSource jobDataSource) {
        final DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(jobDataSource);
        dataSourceTransactionManager.setRollbackOnCommitFailure(true);
        return dataSourceTransactionManager;
    }

    @Bean
    public MessageSource messageSource() {
        final ResourceBundleMessageSource resourceBundleMessageSource = new ResourceBundleMessageSource();
        resourceBundleMessageSource.setBasename("i18n/application-messages");
        return resourceBundleMessageSource;
    }

    @Bean
    public SpringValidator<?> validator(Validator beanValidator) {
        final SpringValidator<?> springValidator = new SpringValidator<>();
        springValidator.setValidator(beanValidator);
        return springValidator;
    }

    @Bean
    public Validator beanValidator() {
        try (LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean()) {
            localValidatorFactoryBean.afterPropertiesSet();
            return localValidatorFactoryBean;
        }
    }

    @Bean
    public SqlSessionFactory jobSqlSessionFactory(@Qualifier("jobDataSource") DataSource jobDataSource) throws Exception {
        final SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(jobDataSource);

        final org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        configuration.setLazyLoadingEnabled(true);
        configuration.setAggressiveLazyLoading(false);
        configuration.setDefaultFetchSize(1000);
        configuration.setDefaultExecutorType(ExecutorType.REUSE);

        sqlSessionFactoryBean.setConfiguration(configuration);
        return sqlSessionFactoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate batchModeSqlSessionTemplate(@Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory) {
        return new SqlSessionTemplate(jobSqlSessionFactory, ExecutorType.BATCH);
    }

    @Bean
    public PlatformTransactionManager jobResourcelessTransactionManager() {
        return new ResourcelessTransactionManager();
    }
}
