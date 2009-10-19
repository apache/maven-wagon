package org.apache.maven.wagon.tck.http.util;

public class ValueHolder<T>
{
    private T value;

    public ValueHolder()
    {
    }

    public ValueHolder( final T initial )
    {
        this.value = initial;
    }

    public void setValue( final T value )
    {
        this.value = value;
    }

    public T getValue()
    {
        return value;
    }
}
