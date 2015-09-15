# jooq-gradle-plugin
jOOQ Code generation Gradle Plugin
=====================

A gradle plugin for DB schema classes generation using JOOQ. 

jOOQ is a Java based framework for interacting with different DBMS's. jOOQ can generate code for classses representing database entities. 

For reference, see http://www.jooq.org/doc/3.4/manual/getting-started/tutorials/jooq-in-7-steps/jooq-in-7-steps-step3/.

This Gradle plugin allows making this code generation a part of the Gradle build process, generating a jar file that can be added as a dependency for a Gradle project.

## Instalation ##

1. Clone this repository
2. Change directory to the cloned repository folder
3. Install the plugin into your local repository
```sh
gradlew uploadArchives
```
4. In the build.gradle of your gradle project add the JOOQ dependencies and apply the plugin
```groovy
buildscript{
    repositories { 
        mavenLocal()
        mavenCentral() 
    }
    dependencies { 
        classpath 'rsf:jooq:0.9.9'
        classpath 'org.jooq:jooq:3.6.1'
        classpath 'org.jooq:jooq-meta:3.6.1'
        classpath 'org.jooq:jooq-codegen:3.6.1'
        classpath 'mysql:mysql-connector-java:5.1.36' 
    } 
}

apply plugin: 'rsf-jooq'
```
5. Declare the jooq generated classes as a dependency in your build.gradle:
```groovy
dependencies {
  compile jooqClasses('MySQL', 'jdbc:mysql://localhost:3306/schemaName', 'root', '', 'schemaName')
}
```
The arguments to jooqClasses are: the DBMS, the database url, the user name and its password, and the database schema name.

The resolution of the dependencies will trigger the plugin execution and, as a consequence, a connection to the runnin database instance will be required.

The generated sources, classes and .jar are stored within the build directory of the project.

In a eclipse project, the dependencies resolution can be triggered by
```sh
gradlew eclipse
```

## Notes ##

Currently, only MySQL is supported.
