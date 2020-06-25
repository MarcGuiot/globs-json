package org.globsframework.json.annottations;

import org.globsframework.json.JsonDateTimeFormatType;
import org.globsframework.metamodel.GlobModel;
import org.globsframework.metamodel.impl.DefaultGlobModel;

public class AllAnnotations {
    public final static GlobModel MODEL =
            new DefaultGlobModel(IsJsonContentType.TYPE, JsonDateTimeFormatType.TYPE, JsonDateFormatType.TYPE, UnknownAnnotation.TYPE);
}
