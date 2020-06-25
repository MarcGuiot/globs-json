package org.globsframework.json.annottations;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.KeyField;
import org.globsframework.metamodel.fields.StringField;

public class UnknownAnnotation {
    public static GlobType TYPE;

    @KeyField
    public static StringField uuid;

    @IsJsonContentAnnotation
    public static StringField CONTENT;

    static {
        GlobTypeLoaderFactory.create(UnknownAnnotation.class).load();
    }
}
