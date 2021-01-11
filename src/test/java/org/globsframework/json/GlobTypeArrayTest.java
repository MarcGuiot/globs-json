package org.globsframework.json;

import com.google.gson.Gson;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.annotations.FieldNameAnnotationType;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class GlobTypeArrayTest {

    @Test
    public void name() {
        String name = "[{\"kind\":\"root\",\"fields\":[{\"name\":\"__children__\",\"type\":\"globArray\",\"kind\":\"Csv\"}]}," +
                "{\"kind\":\"fieldNameAnnotation\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}," +
                "{\"kind\":\"Csv\",\"fields\":[{\"name\":\"Csv:EAN\",\"type\":\"string\",\"annotations\":[{\"_kind\":\"fieldNameAnnotation\",\"name\":\"EAN\"}]}]}]\n";

        Gson gson = GlobsGson.create(GlobTypeResolver.from(FieldNameAnnotationType.TYPE));
        GlobType[] globTypes = gson.fromJson(name, GlobTypeSet.class).globType;
        Assert.assertEquals(3, globTypes.length);
        GlobTypeSet export = GlobTypeSet.export(globTypes[0]);
        Assert.assertEquals("root", globTypes[0].getName());
//        String jsonTypes = gson.toJson(export);
//        Assert.assertEquals(GSonUtils.normalize(name), GSonUtils.normalize(jsonTypes));
    }


    @Test
    public void recursiveType() {
        String name = "[\n" +
                "  {\n" +
                "    \"kind\": \"root\",\n" +
                "    \"fields\": [\n" +
                "      {\n" +
                "        \"name\": \"__children__\",\n" +
                "        \"type\": \"globArray\",\n" +
                "        \"kind\": \"Node\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  " +
                "{\n" +
                "    \"kind\": \"fieldNameAnnotation\",\n" +
                "    \"fields\": [\n" +
                "      {\n" +
                "        \"name\": \"name\",\n" +
                "        \"type\": \"string\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  " +
                "{\n" +
                "    \"kind\": \"Node\",\n" +
                "    \"fields\": [\n" +
                "      {\n" +
                "        \"name\": \"name\",\n" +
                "        \"type\": \"string\",\n" +
                "        \"annotations\": [\n" +
                "          {\n" +
                "            \"_kind\": \"fieldNameAnnotation\",\n" +
                "            \"name\": \"EAN\"\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"name\": \"__children__\",\n" +
                "        \"type\": \"globArray\",\n" +
                "        \"kind\": \"Node\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]\n";

        Gson gson = GlobsGson.create(GlobTypeResolver.from(FieldNameAnnotationType.TYPE));
        GlobType[] globTypes = gson.fromJson(name, GlobTypeSet.class).globType;
        Assert.assertEquals(3, globTypes.length);
        GlobTypeSet export = GlobTypeSet.export(globTypes[0]);
        Assert.assertEquals("root", globTypes[0].getName());

    }
}
