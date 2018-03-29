package org.globsframework.json;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.GlobCreateFromAnnotation;
import org.globsframework.metamodel.annotations.InitUniqueGlob;
import org.globsframework.metamodel.annotations.InitUniqueKey;
import org.globsframework.model.Glob;
import org.globsframework.model.Key;

public class IsJsonContentType {
    public static GlobType TYPE;

    @InitUniqueKey
    public static Key UNIQUE_KEY;

    @InitUniqueGlob
    public static Glob UNIQUE_GLOB;

    static {
        GlobTypeLoaderFactory.create(IsJsonContentType.class, "isJsonContent")
              .register(GlobCreateFromAnnotation.class, annotation -> UNIQUE_GLOB)
              .load();
    }

}
