import groovy.xml.XmlUtil
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.nio.file.Files
import java.nio.file.Path
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

def moveToMetaPackage(File metaPackage, File projectDir, String oldPackage) {
  if (! metaPackage.mkdir()) {
      println '** fail to create __packageInPathFormat__ directory.'
      System.exit 3
  }

  Files.move(
          new File(projectDir, "src/main/resources/${oldPackage}__artifactId__").toPath(),
          new File(projectDir, 'src/main/resources/__packageInPathFormat__').toPath(),
          StandardCopyOption.REPLACE_EXISTING)

  if (! new File(projectDir, 'src/main/resources/xxxxxx').deleteDir()) {
      println "** fail to delete old-package directory."
      System.exit 4
  }
}

if (!metaPackage.exists()) {
  moveToMetaPackage(metaPackage, projectDir, OLD_PACKAGE)
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
                url 'https://repo1.maven.org/maven2/'
            }

            repository {
                releases {
                    enabled 'false'
                }
                snapshots {
                    enabled 'true'
                }
                id 'terasoluna-batch-snapshots'
                url 'https://nexus.func.test:8443/repository/maven-snapshots/'
            }
        }
        profiles {
            profile {
                id 'nexus'
                activation {
                    property{
                       name 'nexus'
                    }
                }
                distributionManagement {
                    repository {
                        id 'terasoluna-batch-releases'
                        url 'https://nexus.func.test:8443/repository/maven-releases/'
                    }
                    snapshotRepository {
                        id 'terasoluna-batch-snapshots'
                        url 'https://nexus.func.test:8443/repository/maven-snapshots'
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
                                    configuration {
                                        gpgArguments {
                                            arg '--pinentry-mode'
                                            arg 'loopback'
                                        }
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
            'maven-gpg-plugin.version' '3.0.1'
            'nexus-staging-maven-plugin.version' '1.6.8'
            'archetype-packaging.version' '3.2.0'
            'maven-archetype-plugin.version' '3.2.0'
        }
}

// write to pom.xml
new BufferedWriter(new FileWriter(new File(archetypeDir, 'pom.xml'))).withWriter { writer ->
    writer.write(XmlUtil.serialize(doc))
}

Files.delete(orgPath)

println "### Ph-3. re-create empty pom's tag."

static void outputXML(File f, Document doc) {
    def out = new ByteArrayOutputStream()
    TransformerFactory.newInstance().newTransformer()
            .transform(new DOMSource(doc), new StreamResult(out))

    def xml = out.toString("UTF-8")
            .replaceFirst('(<project xmlns)',
            '\r\n$1')

    new FileWriter(f).withWriter { w ->
        w.write(xml)
        w.flush()
    }
}

Path projectPom = new File(projectDir, 'pom.xml').toPath()
Path orgProjectPom = new File(projectDir, 'pom.xml.org').toPath()

Files.move(projectPom, orgProjectPom)

Document projectDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse orgProjectPom.toFile()

outputXML projectPom.toFile(), projectDoc

Files.delete orgProjectPom

println "normal end."
