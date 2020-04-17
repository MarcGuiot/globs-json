package org.globsframework.json.helper;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeBuilderFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class LoadingGlobTypeResolverTest {

    @Test
    public void name() {

        GlobType globType3 = GlobTypeBuilderFactory.create("type3").addStringField("field3").get();

        LoadingGlobTypeResolver.Builder builder = LoadingGlobTypeResolver.builder(name -> {
            return name.equals("type3") ? globType3 : null;
        });
        builder.read(new StringReader("{ \n" +
                "  \"kind\": \"type2\",\n" +
                "  \"fields\": [\n" +
                "    {\n" +
                "      \"name\": \"f1\",\n" +
                "      \"kind\": \"type1\",\n" +
                "      \"type\": \"glob\"\n" +
                "    }\n" +
                "  ]\n" +
                "}"));

        builder.read(new StringReader("[\n" +
                "  {\n" +
                "    \"kind\": \"type1\",\n" +
                "    \"fields\": [\n" +
                "      {\n" +
                "        \"name\": \"field1\",\n" +
                "        \"type\": \"string\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  {\n" +
                "    \"kind\": \"type3\",\n" +
                "    \"fields\": [\n" +
                "      {\n" +
                "        \"name\": \"field1\",\n" +
                "        \"type\": \"string\"\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "]"));

        Map<String, GlobType> read = new HashMap<>();
        builder.read().forEach(globType -> read.put(globType.getName(), globType));
        Assert.assertEquals(3, read.size());
        Assert.assertNotNull(read.get("type1"));
        Assert.assertSame(globType3, read.get("type3"));
    }
}