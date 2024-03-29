package xxxxxx.yyyyyy.zzzzzz.projectName.jobs;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.PlatformTransactionManager;

import org.springframework.util.ClassUtils;
import xxxxxx.yyyyyy.zzzzzz.projectName.config.JobBaseContextConfig;
import xxxxxx.yyyyyy.zzzzzz.projectName.job01.Employee;
import xxxxxx.yyyyyy.zzzzzz.projectName.job01.EmployeeProcessor;

@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan("xxxxxx.yyyyyy.zzzzzz.projectName.job01")
@MapperScan(basePackages = "xxxxxx.yyyyyy.zzzzzz.projectName.job01", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class Job01Config {

    @Bean
    @StepScope
    public ListItemReader<Employee> employeeReader() {
        Employee employee1 = new Employee();
        employee1.setEmpId(1);
        employee1.setEmpName("scott");

        Employee employee2 = new Employee();
        employee2.setEmpId(2);
        employee2.setEmpName("virgil");

        Employee employee3 = new Employee();
        employee3.setEmpId(3);
        employee3.setEmpName("gordon");

        Employee employee4 = new Employee();
        employee4.setEmpId(4);
        employee4.setEmpName("john");

        Employee employee5 = new Employee();
        employee5.setEmpId(5);
        employee5.setEmpName("alan");

        return new ListItemReader<>(
                List.of(employee1, employee2, employee3, employee4, employee5));
    }

    @Bean
    public FlatFileItemWriter<Employee> employeeWriter() {
        DelimitedLineAggregator<Employee> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        BeanWrapperFieldExtractor<Employee> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] { "empId", "empName" });
        lineAggregator.setFieldExtractor(fieldExtractor);
        FileSystemResourceLoader loader = new FileSystemResourceLoader();
        return new FlatFileItemWriterBuilder<Employee>()
                .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                .lineAggregator(lineAggregator)
                .resource((WritableResource) loader.getResource(
                        "file:target/output.csv"))
                .build();
    }

    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobTransactionManager") PlatformTransactionManager transactionManager,
                       ListItemReader<Employee> employeeReader,
                       EmployeeProcessor employeeProcessor,
                       FlatFileItemWriter<Employee> employeeWriter) {
        return new StepBuilder("job01.step01",
                jobRepository)
                .<Employee, Employee> chunk(10, transactionManager)
                .reader(employeeReader)
                .processor(employeeProcessor)
                .writer(employeeWriter)
                .build();
    }

    @Bean
    public Job job01(JobRepository jobRepository,
                     Step step01) {
        return new JobBuilder("job01", jobRepository)
                .start(step01)
                .build();
    }

}
