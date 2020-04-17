package org.globsframework.json;

import org.globsframework.json.annottations.JsonDateTimeFormatAnnotation;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.metamodel.fields.BooleanField;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Key;

public class JsonDateTimeFormatType {
    public static GlobType TYPE;

    public static StringField FORMAT;

    public static BooleanField AS_LOCAL;

    public static StringField NULL_VALUE;

    @InitUniqueKey
    public static Key UNIQUE_KEY;

    static {
        GlobTypeLoaderFactory.create(JsonDateTimeFormatType.class, "jsonDateTimeFormat")
              .register(GlobCreateFromAnnotation.class, annotation -> TYPE.instantiate()
                      .set(FORMAT, ((JsonDateTimeFormatAnnotation) annotation).pattern())
                      .set(AS_LOCAL, ((JsonDateTimeFormatAnnotation) annotation).asLocal())
                      .set(NULL_VALUE, ((JsonDateTimeFormatAnnotation) annotation).nullValue())
              )
              .load();
    }

}
