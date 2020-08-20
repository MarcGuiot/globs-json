package org.globsframework.json;

import com.google.gson.Gson;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.annotations.FieldNameAnnotationType;
import org.junit.Ignore;
import org.junit.Test;

public class GlobTypeArrayTest {

    @Test
//    @Ignore
    // comment faire?
    public void name() {
        String name = "[{\"kind\":\"root\",\"fields\":[{\"name\":\"__children__\",\"type\":\"globArray\",\"kind\":\"Csv\"}]}," +
                "{\"kind\":\"fieldNameAnnotation\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}," +
                "{\"kind\":\"Csv\",\"fields\":[{\"name\":\"Csv:EAN\",\"type\":\"string\",\"annotations\":[{\"_kind\":\"fieldNameAnnotation\",\"name\":\"EAN\"}]}]}]\n";

        Gson gson = GlobsGson.create(GlobTypeResolver.from(FieldNameAnnotationType.TYPE));
        GlobType[] globTypes = gson.fromJson(name, GlobTypeSet.class).globType;
    }
}
