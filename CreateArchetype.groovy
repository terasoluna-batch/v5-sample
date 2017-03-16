import groovy.xml.XmlUtil

import java.nio.file.Files
import java.nio.file.StandardCopyOption

final def ROOT_DIR = 'terasoluna-batch-blankproject/'
final def ARCHETYPE_DIR = 'target/generated-sources/archetype/'
final def PROJECT_DIR = 'src/main/resources/archetype-resources/'
final def OLD_PACKAGE = 'xxxxxx/yyyyyy/zzzzzz/'

def rootDir = new File(ROOT_DIR)
def archetypeDir = new File(rootDir, ARCHETYPE_DIR)
def projectDir = new File(archetypeDir, PROJECT_DIR)

if (!projectDir.exists() || !projectDir.directory) {
    println '** do not build archetype project yet.'
    System.exit 2
}

println '### Ph-1. move to __packageInPathFormat__ in resources.'

def metaPackage = new File(projectDir, 'src/main/resources/__packageInPathFormat__')

def moveToMetaPackage() {
  if (! metaPackage.mkdir()) {
      println '** fail to create __packageInPathFormat__ directory.'
      System.exit 3
  }

  Files.move(
          new File(projectDir, "src/main/resources/${OLD_PACKAGE}__artifactId__").toPath(),
          new File(projectDir, 'src/main/resources/__packageInPathFormat__').toPath(),
          StandardCopyOption.REPLACE_EXISTING)

  if (! new File(projectDir, 'src/main/resources/xxxxxx').deleteDir()) {
      println "** fail to delete old-package directory."
      System.exit 4
  }
}

if (! metaPackage) {
  moveToMetaPackage()
} else {
  println "** exists ${metaPackage.toPath()}, skipping."
}

println '### Ph-2. edit pom.xml to deploy Maven central repository.'

def pom = new File(archetypeDir, 'pom.xml')

if (! pom.exists() || ! pom.file) {
    println "** pom.xml does not exist in ${pom.path}"
    System.exit 5
}


def orgPath = Files.move(pom.toPath(), new File(archetypeDir, 'pom.xml.org').toPath(),
        StandardCopyOption.REPLACE_EXISTING)
def doc = new XmlSlurper(false, false).parse(orgPath.toFile())

doc.with {
    // replace blank project to archetype.
    groupId = 'org.terasoluna.batch'
    artifactId = 'terasoluna-batch-archetype'
    name = 'terasoluna-batch-archetype'
    description = 'Archetype project for TERASOLUNA Batch Framework for Java (5.x)'
    build.with {
        extensions.extension.version = '${archetype-packaging.version}'
        pluginManagement.plugins.plugin.version = '${maven-archetype-plugin.version}'
    }
}

// append some insufficient node.
doc.appendNode {
        url 'http://terasoluna.org'
        inceptionYear '2017'
        organization {
            name 'terasoluna.org'
            url 'http://terasoluna.org'
        }
        developers {
            developer {
                name 'NTT DATA'
                organization 'NTT DATA Corporation'
                organizationUrl 'http://terasoluna-batch.github.io/'
            }
        }
        scm {
            connection 'scm:git:git@github.com:terasoluna-batch/v5-sample.git'
            developerConnection 'scm:git:git@github.com:terasoluna-batch/v5-sample.git'
            url 'scm:git:git@github.com:terasoluna-batch/v5-sample.git'
        }
        repositories {
            repository {
                snapshots {
                    enabled 'false'
                }
                id 'central'
                name 'Maven Central repository'
                url 'http://repo1.maven.org/maven2/'
            }

            repository {
                releases {
                    enabled 'false'
                }
                snapshots {
                    enabled 'true'
                }
                id 'terasoluna-batch-snapshots'
                url 'http://repo.terasoluna.org/nexus/content/repositories/terasoluna-batch-snapshots/'
            }
        }
        profiles {
            profile {
                id 'default'
                activation {
                    activeByDefault 'true'
                }
                distributionManagement {
                    snapshotRepository {
                        id 'terasoluna-batch-snapshots'
                        url 'http://repo.terasoluna.org/nexus/content/repositories/terasoluna-batch-snapshots/'
                    }
                }
            }
            profile {
                id 'central'
                distributionManagement {
                    repository {
                        id 'ossrh'
                        url 'https://oss.sonatype.org/service/local/staging/deploy/maven2/'
                    }
                    snapshotRepository {
                        id 'ossrh'
                        url 'https://oss.sonatype.org/content/repositories/snapshots'
                    }
                }
                build {
                    plugins {
                        plugin {
                            groupId 'org.apache.maven.plugins'
                            artifactId 'maven-gpg-plugin'
                            version '${maven-gpg-plugin.version}'
                            executions {
                                execution {
                                    id 'sign-artifacts'
                                    phase 'verify'
                                    goals {
                                        goal 'sign'
                                    }
                                }
                            }

                        }
                        plugin {
                            groupId 'org.sonatype.plugins'
                            artifactId 'nexus-staging-maven-plugin'
                            version '${nexus-staging-maven-plugin.version}'
                            extensions 'true'
                            configuration {
                                serverId 'ossrh'
                                nexusUrl 'https://oss.sonatype.org/'
                                autoReleaseAfterClose 'true'
                            }
                        }
                    }
                }
            }
        }
        properties {
            'maven-gpg-plugin.version' '1.6'
            'nexus-staging-maven-plugin.version' '1.6.8'
            'archetype-packaging.version' '2.4'
            'maven-archetype-plugin.version' '2.4'
        }
}

// write to pom.xml
new BufferedWriter(new FileWriter(new File(archetypeDir, 'pom.xml'))).withWriter { writer ->
    writer.write(XmlUtil.serialize(doc))
}

Files.delete(orgPath)

println "normal end."
