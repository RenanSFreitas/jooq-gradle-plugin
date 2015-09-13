package com.rsf.jooq.gradle.plugin;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;
import org.jooq.util.GenerationTool;
import org.jooq.util.jaxb.Configuration;
import org.jooq.util.jaxb.Database;
import org.jooq.util.jaxb.Generator;
import org.jooq.util.jaxb.Jdbc;
import org.jooq.util.jaxb.Target;

public class JooqTask extends DefaultTask
{
    @TaskAction
    public void generateDatabaseSchemaClasses()
    {
        try
        {
            generatedJooqClassesSources();

            compileJooqGeneratedClasses();

            String jooqGeneratedJarPath = createJooqGeneratedClassesJar();

            getProject().getDependencies().add("jooqclasses", getProject().files(jooqGeneratedJarPath));

        }
        catch (final Exception e)
        {
            throw new TaskExecutionException(this, e);
        }
    }

    private JooqPluginExtension generatedJooqClassesSources() throws Exception
    {
        JooqPluginExtension projectExtension = getProject().getExtensions().getByType(JooqPluginExtension.class);
        SupportedDatabase supportedDatabase = SupportedDatabase.valueOf(projectExtension.getDatabase());

        GenerationTool.generate(new Configuration()
            .withGenerator(getGenerator(projectExtension, supportedDatabase))
            .withJdbc(getJdbc(projectExtension, supportedDatabase)));
        return projectExtension;
    }

    private void compileJooqGeneratedClasses() throws IOException
    {
        File[] sourceFiles = getGeneratedJooqSourceFiles();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Iterable<? extends JavaFileObject> javaFileObjects = fileManager.getJavaFileObjects(sourceFiles);

        Path targetClassesDirectory = Paths.get(getTargetClassesDirectory());
        if (!Files.exists(targetClassesDirectory))
        {
            Files.createDirectory(targetClassesDirectory);
        }
        
        Iterable<String> options = Arrays.asList(
                "-classpath", getJooqClasspath(),
                "-d", targetClassesDirectory.toString());

        CompilationTask task = compiler.getTask(null, fileManager, null, options, null, javaFileObjects);
        Boolean compilationSucceeded = task.call();

        if (!compilationSucceeded)
        {
            throw new TaskExecutionException(this, new IllegalStateException("bad java files"));
        }
    }

    private String getJooqClasspath()
    {
        StringBuilder jooqClasspath = new StringBuilder();

        for (File a : getProject().getConfigurations().getByName("compile").getFiles())
        {
            if (a.getName().contains("jooq"))
            {
                jooqClasspath.append(a.getAbsolutePath() + ";");
            }
        }
        return jooqClasspath.toString();
    }

    private File[] getGeneratedJooqSourceFiles() throws IOException
    {
        final List<File> sourceFiles = new ArrayList<>();
        Files.walkFileTree(Paths.get(new File(getTargetSourcesDirectory()).getAbsolutePath()), new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
            {
                sourceFiles.add(path.toFile());
                return FileVisitResult.CONTINUE;
            }
        });
        return sourceFiles.toArray(new File[sourceFiles.size()]);
    }

    private String createJooqGeneratedClassesJar() throws IOException
    {
        String schema = getProject().getExtensions().getByType(JooqPluginExtension.class).getSchema();
        String outputJarPath = getProject().getBuildDir().getAbsolutePath() + "/jooq." + schema + ".jar";

        try (JarOutputStream stream = new JarOutputStream(Files.newOutputStream(Paths.get(outputJarPath))))
        {
            for (File nestedFile : new File(getTargetClassesDirectory()).listFiles())
            {
                addEntry(stream, nestedFile, null);
            }
        }

        return outputJarPath;
    }

    private void addEntry(JarOutputStream stream, File file, String parent) throws IOException
    {
        String entryName = (parent == null ? "" : parent) + file.getName() + "/";
        if (file.isDirectory())
        {
            JarEntry entry = new JarEntry(entryName);
            entry.setTime(file.lastModified());
            stream.putNextEntry(entry);
            stream.closeEntry();

            for (File nestedFile : file.listFiles())
            {
                addEntry(stream, nestedFile, entryName);
            }
        }
        else
        {

            try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file)))
            {
                JarEntry entry = new JarEntry(entryName.substring(0, entryName.length() - 1));
                entry.setTime(file.lastModified());
                stream.putNextEntry(entry);

                byte[] buffer = new byte[1024];
                while (true)
                {
                    int count = inputStream.read(buffer);
                    if (count == -1)
                        break;
                    stream.write(buffer, 0, count);
                }
                stream.closeEntry();
            }
        }
    }

    private Generator getGenerator(JooqPluginExtension projectExtension, SupportedDatabase supportedDatabase)
    {
        Generator generator = new Generator();
        Database database = new Database();
        database.setName(supportedDatabase.getName());
        // TODO probably turn this 2 properties into a project extension
        // properties as well
        database.setIncludes(".*");
        database.setExcludes("");
        String schema = projectExtension.getSchema();
        database.setInputSchema(schema);
        generator.setDatabase(database);

        Target target = new Target();
        target.setDirectory(getTargetSourcesDirectory());
        target.setPackageName("auto.generated.jooq.classes." + schema);
        generator.setTarget(target);

        return generator;
    }

    private String getTargetSourcesDirectory()
    {
        return getProject().getBuildDir() + "/auto-generated-jooq-sources";
    }

    private String getTargetClassesDirectory()
    {
        return getProject().getBuildDir() + "/auto-generated-jooq-classes";
    }

    private Jdbc getJdbc(JooqPluginExtension projectExtension, SupportedDatabase supportedDatabase)
    {
        Jdbc jdbc = new Jdbc();
        jdbc.setDriver(supportedDatabase.getDriver());
        jdbc.setUrl(projectExtension.getUrl());
        jdbc.setUser(projectExtension.getUser());
        jdbc.setPassword(projectExtension.getPassword());
        return jdbc;
    }
}
