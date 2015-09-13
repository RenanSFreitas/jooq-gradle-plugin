package com.rsf.jooq.gradle.plugin;

import groovy.lang.Closure;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;

public class JooqPlugin implements Plugin<Project>
{
    @Override
    public void apply(final Project project)
    {
        project.getExtensions().getExtraProperties().set("jooqClasses", new Closure<ConfigurableFileCollection>(project)
        {

            private static final long serialVersionUID = 4230294368843574725L;

            @Override
            public ConfigurableFileCollection call(Object... args)
            {
                String database = args[0].toString();
                String databaseUrl = args[1].toString();
                String user = args[2].toString();
                String password = args[3].toString();
                String schema = args[4].toString();

                return new LazyConfigurableFileCollection(new JooqClassesJarGenerator(project, database, databaseUrl, user, password, schema));
            }
        });
    }
}
