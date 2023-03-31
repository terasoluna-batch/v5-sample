package xxxxxx.yyyyyy.zzzzzz.projectName.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!async")
@Import(LaunchContextConfig.class)
public class JobBaseContextConfig {

}
