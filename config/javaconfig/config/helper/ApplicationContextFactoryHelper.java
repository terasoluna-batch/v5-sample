package xxxxxx.yyyyyy.zzzzzz.projectName.config.helper;

import org.springframework.batch.core.configuration.support.ApplicationContextFactory;
import org.springframework.batch.core.configuration.support.GenericApplicationContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.stream.Stream;

public class ApplicationContextFactoryHelper {

    private final PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();

    private final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(
            pathMatchingResourcePatternResolver);

    private final ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

    private final ApplicationContext applicationContext;

    public ApplicationContextFactoryHelper(
            ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ApplicationContextFactory[] load(String... locations) {
        final Stream<String> locationStream = Stream.of(locations);
        return locationStream
                .flatMap(loc -> loc.endsWith(".xml") ?
                        createXmlConfigFactories(loc) :
                        createJavaConfigFactories(loc))
                .toArray(ApplicationContextFactory[]::new);
    }

    private Stream<ApplicationContextFactory> createXmlConfigFactories(
            final String xmlConfigPathPattern) {
        return resolvePatternedResource(xmlConfigPathPattern)
                .map(this::createFactory);
    }

    private Stream<ApplicationContextFactory> createJavaConfigFactories(
            String javaConfigResourcePattern) {
        return resolvePatternedResource(javaConfigResourcePattern)
                .map(this::loadConfigClassCandidate)
                .filter(this::hasAnnotatedConfigClass)
                .map(this::createFactory);
    }

    Stream<Resource> resolvePatternedResource(String resourcePattern) {
        try {
            return Stream.of(pathMatchingResourcePatternResolver.getResources(
                            resourcePattern))
                    .filter(Resource::isReadable);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    Class<?> loadConfigClassCandidate(Resource javaConfigResource) {
        final MetadataReader reader;
        try {
            reader = metadataReaderFactory.getMetadataReader(
                    javaConfigResource);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ClassUtils.resolveClassName(
                reader.getClassMetadata().getClassName(), classLoader);
    }

    boolean hasAnnotatedConfigClass(Class<?> candidate) {
        return AnnotationMetadata
                .introspect(candidate)
                .getAnnotations()
                .isPresent(Configuration.class);
    }

    private ApplicationContextFactory createFactory(Object resource) {
        final GenericApplicationContextFactory factory = new GenericApplicationContextFactory(
                resource);
        factory.setApplicationContext(applicationContext);
        return factory;
    }
}
