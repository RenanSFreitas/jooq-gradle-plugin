package com.rsf.jooq.gradle.plugin;

import static java.util.Objects.requireNonNull;

import java.util.Set;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.internal.file.collections.DefaultConfigurableFileCollection;
import org.gradle.api.internal.file.collections.FileCollectionResolveContext;

final class LazyConfigurableFileCollection extends DefaultConfigurableFileCollection
{
    private static DefaultConfigurableFileCollection delegate;
    private JooqClassesJarGenerator jarGenerator;

    public LazyConfigurableFileCollection(JooqClassesJarGenerator jarGenerator)
    {
        super(null, null, new Object[0]);
        this.jarGenerator = jarGenerator;
    }

    private DefaultConfigurableFileCollection getDelegate()
    {
        if (delegate == null)
        {
            delegate = requireNonNull(jarGenerator.getJooqClassesJarFileCollection());
        }
        return delegate;
    }
    
    @Override
    public String getDisplayName() {
        return getDelegate().getDisplayName();
    }

    @Override
    public Set<Object> getFrom()
    {
        return getDelegate().getFrom();
    }

    @Override
    public void setFrom(Iterable<?> path)
    {
        getDelegate().setFrom(path);
    }

    @Override
    public void setFrom(Object... paths)
    {
        getDelegate().setFrom(paths);
    }

    @Override
    public ConfigurableFileCollection from(Object... paths)
    {
        return getDelegate().from(paths);
    }

    @Override
    public ConfigurableFileCollection builtBy(Object... tasks)
    {
        return getDelegate().builtBy(tasks);
    }

    @Override
    public Set<Object> getBuiltBy()
    {
        return getDelegate().getBuiltBy();
    }

    @Override
    public ConfigurableFileCollection setBuiltBy(Iterable<?> tasks)
    {
        return getDelegate().setBuiltBy(tasks);
    }

    @Override
    public void resolve(FileCollectionResolveContext context)
    {
        getDelegate().resolve(context);
    }
}
