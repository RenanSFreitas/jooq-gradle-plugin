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

import org.gradle.api.Project;
import org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection;
import org.jooq.util.GenerationTool;
import org.jooq.util.jaxb.Configuration;
import org.jooq.util.jaxb.Database;
import org.jooq.util.jaxb.Generator;
import org.jooq.util.jaxb.Jdbc;
import org.jooq.util.jaxb.Target;

final class JooqClassesJarGenerator
{
    private Project project;

    private SupportedDatabase supportedDatabase;
    private String databaseUrl;
    private String user;
    private String password;
    private String schema;

    public JooqClassesJarGenerator(Project project, String database, String databaseUrl, String user, String password, String schema)
    {
        this.project = project;
        this.supportedDatabase = SupportedDatabase.valueOf(database);
        this.databaseUrl = databaseUrl;
        this.user = user;
        this.password = password;
        this.schema = schema;
    }

    public DefaultConfigurableFileCollection getJooqClassesJarFileCollection()
    {
        try
        {
            generatedJooqClassesSources();

            compileJooqGeneratedClasses();

            String jooqGeneratedJarPath = createJooqGeneratedClassesJar();

            return (DefaultConfigurableFileCollection) project.files(jooqGeneratedJarPath);
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void generatedJooqClassesSources() throws Exception
    {
        GenerationTool.generate(new Configuration()
            .withGenerator(getGenerator())
            .withJdbc(getJdbc()));
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

        Iterable<String> options = Arrays.asList("-classpath", getJooqClasspath(), "-d", targetClassesDirectory.toString());

        CompilationTask task = compiler.getTask(null, fileManager, null, options, null, javaFileObjects);
        Boolean compilationSucceeded = task.call();

        if (!compilationSucceeded)
        {
            throw new IllegalStateException("bad java files");
        }
    }

    private String getJooqClasspath()
    {
        StringBuilder jooqClasspath = new StringBuilder();

        for (File file : project.getBuildscript().getConfigurations().getByName("classpath").getFiles())
        {
            if (file.getName().contains("jooq"))
            {
                jooqClasspath.append(file.getAbsolutePath() + ";");
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
        String outputJarPath = project.getBuildDir().getAbsolutePath() + "/jooq." + schema + ".jar";

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

    private Generator getGenerator()
    {
        Generator generator = new Generator();
        Database database = new Database();
        database.setName(supportedDatabase.getName());
        // TODO probably turn this 2 properties into a project extension
        // properties as well
        database.setIncludes(".*");
        database.setExcludes("");
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
        return project.getBuildDir() + "/auto-generated-jooq-sources";
    }

    private String getTargetClassesDirectory()
    {
        return project.getBuildDir() + "/auto-generated-jooq-classes";
    }

    private Jdbc getJdbc()
    {
        Jdbc jdbc = new Jdbc();
        jdbc.setDriver(supportedDatabase.getDriver());
        jdbc.setUrl(databaseUrl);
        jdbc.setUser(user);
        jdbc.setPassword(password);
        return jdbc;
    }
}
