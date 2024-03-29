package xxxxxx.yyyyyy.zzzzzz.projectName.config;

import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.terasoluna.batch.converter.JobParametersConverterImpl;

import javax.sql.DataSource;

@Configuration
public class TerasolunaBatchConfiguration extends DefaultBatchConfiguration {

    // DefaultBatchConfiguration searches for the data source bean name using "dataSource",
    // so override getDataSource and modify it to search using "adminDataSource".
    @Override
    protected DataSource getDataSource() {
        String errorMessage =
                " To use the default configuration, a data source bean named 'adminDataSource'"
                        + " should be defined in the application context but none was found. Override getDataSource()"
                        + " to provide the data source to use for Batch meta-data.";
        if (this.applicationContext.getBeansOfType(DataSource.class)
                .isEmpty()) {
            throw new BatchConfigurationException(
                    "Unable to find a DataSource bean in the application context."
                            + errorMessage);
        } else {
            if (!this.applicationContext.containsBean("adminDataSource")) {
                throw new BatchConfigurationException(errorMessage);
            }
        }
        return this.applicationContext.getBean("adminDataSource",
                DataSource.class);
    }

    // DefaultBatchConfiguration searches for the data source bean name using "transactionManager",
    // so override getTransactionManager and modify it to search using "adminTransactionManager".
    @Override
    protected PlatformTransactionManager getTransactionManager() {
        String errorMessage =
                " To use the default configuration, a transaction manager bean named 'adminTransactionManager'"
                        + " should be defined in the application context but none was found. Override getTransactionManager()"
                        + " to provide the transaction manager to use for the job repository.";
        if (this.applicationContext.getBeansOfType(
                PlatformTransactionManager.class).isEmpty()) {
            throw new BatchConfigurationException(
                    "Unable to find a PlatformTransactionManager bean in the application context."
                            + errorMessage);
        } else {
            if (!this.applicationContext.containsBean(
                    "adminTransactionManager")) {
                throw new BatchConfigurationException(errorMessage);
            }
        }
        return this.applicationContext.getBean("adminTransactionManager",
                PlatformTransactionManager.class);
    }

    // The default transaction isolation level of JobRepository is "SERIALIZABLE",
    // so override the bean definition and correct it to "READ COMMITTED"
    @Override
    @Bean
    public JobRepository jobRepository() throws BatchConfigurationException {
        JobRepositoryFactoryBean jobRepositoryFactoryBean = new JobRepositoryFactoryBean();
        jobRepositoryFactoryBean.setDataSource(
                getDataSource()); // get and set adminDataSource
        jobRepositoryFactoryBean.setTransactionManager(
                getTransactionManager()); // get and set adminTransactionManager
        jobRepositoryFactoryBean.setIncrementerFactory(getIncrementerFactory());
        jobRepositoryFactoryBean.setClobType(getClobType());
        jobRepositoryFactoryBean.setTablePrefix(getTablePrefix());
        jobRepositoryFactoryBean.setSerializer(getExecutionContextSerializer());
        jobRepositoryFactoryBean.setConversionService(getConversionService());
        jobRepositoryFactoryBean.setJdbcOperations(getJdbcOperations());
        jobRepositoryFactoryBean.setLobHandler(getLobHandler());
        jobRepositoryFactoryBean.setCharset(getCharset());
        jobRepositoryFactoryBean.setMaxVarCharLength(getMaxVarCharLength());
        jobRepositoryFactoryBean.setIsolationLevelForCreateEnum(
                Isolation.READ_COMMITTED); // changed from SERIALIZABLE
        jobRepositoryFactoryBean.setValidateTransactionState(
                getValidateTransactionState());

        try {
            jobRepositoryFactoryBean.setDatabaseType(getDatabaseType());
            jobRepositoryFactoryBean.afterPropertiesSet();
            return jobRepositoryFactoryBean.getObject();

        } catch (BatchConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new BatchConfigurationException(
                    "Unable to configure the customized job repository", e);
        }
    }

    // The implementation class of JobOperator's default JobParametersConverter is "DefaultJobParametersConverter",
    // so override the bean definition and correct it to "JobParametersConverterImpl"
    @Override
    @Bean
    public JobOperator jobOperator() throws BatchConfigurationException {
        JobOperatorFactoryBean jobOperatorFactoryBean = new JobOperatorFactoryBean();
        jobOperatorFactoryBean.setTransactionManager(
                getTransactionManager());  // get and set adminTransactionManager
        jobOperatorFactoryBean.setJobRepository(jobRepository());
        jobOperatorFactoryBean.setJobExplorer(jobExplorer());
        jobOperatorFactoryBean.setJobRegistry(jobRegistry());
        jobOperatorFactoryBean.setJobLauncher(jobLauncher());
        JobParametersConverterImpl jobParametersConverter = new JobParametersConverterImpl(
                getDataSource());
        jobOperatorFactoryBean.setJobParametersConverter(
                jobParametersConverter); // changed from JobParametersConverterImpl

        try {
            jobParametersConverter.afterPropertiesSet();
            jobOperatorFactoryBean.afterPropertiesSet();
            return jobOperatorFactoryBean.getObject();

        } catch (BatchConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new BatchConfigurationException(
                    "Unable to configure the customized job operator", e);
        }
    }
}
