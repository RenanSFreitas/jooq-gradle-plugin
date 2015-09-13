package com.rsf.jooq.gradle.plugin;

public class JooqPluginExtension
{
    private String database;

    private String url;
    private String user;
    private String password;
    private String schema;

    public void setSchema(String schema)
    {
        this.schema = schema;
    }

    public String getSchema()
    {
        return schema;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getDatabase()
    {
        return database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
    }
}
