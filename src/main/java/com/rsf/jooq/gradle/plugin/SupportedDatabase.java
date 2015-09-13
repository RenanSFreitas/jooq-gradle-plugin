package com.rsf.jooq.gradle.plugin;

import org.jooq.util.mysql.MySQLDatabase;

public enum SupportedDatabase
{
    MySQL()
    {
        @Override
        String getName()
        {
            return MySQLDatabase.class.getCanonicalName();
        }

        @Override
        String getDriver()
        {
            return "com.mysql.jdbc.Driver";
        }
    };

    abstract String getName();

    abstract String getDriver();
}
