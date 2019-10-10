package org.globsframework.json;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueGlob;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Glob;
import org.globsframework.model.Key;

public class JsonDateFormatType {
    public static GlobType TYPE;

    public static StringField FORMAT;

    @InitUniqueKey
    public static Key UNIQUE_KEY;

    static {
        GlobTypeLoaderFactory.create(JsonDateFormatType.class, "jsonDateFormat")
              .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate().set(FORMAT, ((JsonDateFormatAnnotation) annotation).value()))
              .load();
    }

}
