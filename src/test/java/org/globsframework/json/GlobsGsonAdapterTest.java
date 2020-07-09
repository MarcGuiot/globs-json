package org.globsframework.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.globsframework.json.annottations.IsJsonContentAnnotation;
import org.globsframework.json.annottations.IsJsonContentType;
import org.globsframework.metamodel.*;
import org.globsframework.metamodel.annotations.AllAnnotations;
import org.globsframework.metamodel.annotations.FieldNameAnnotation;
import org.globsframework.metamodel.annotations.KeyField;
import org.globsframework.metamodel.annotations.Required;
import org.globsframework.metamodel.fields.*;
import org.globsframework.metamodel.impl.DefaultGlobModel;
import org.globsframework.metamodel.impl.DefaultGlobTypeBuilder;
import org.globsframework.model.Glob;
import org.globsframework.model.Key;
import org.globsframework.model.MutableGlob;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class GlobsGsonAdapterTest {

    static {
        System.setProperty("org.globsframework.builder", "org.globsframework.model.generator.GeneratorGlobFactoryService");
    }

    public static final String A_GLOB = "{\"_kind\":\"test local type\",\"id\":1,\"a different name\":\"name 1\",\"data\":{\"sub\":\"aaa\"},\"value\":3.14159}";

    public static class LocalType {
        @Required
        public static GlobType TYPE;

        @KeyField
        public static IntegerField ID;

        @KeyField
        @FieldNameAnnotation("a different name")
        public static StringField NAME;

        @IsJsonContentAnnotation()
        public static StringField DATA;

        public static DoubleField VALUE;

        static {
            GlobTypeLoaderFactory.create(LocalType.class, "test local type", true)
                    .load();
        }
    }

    @Test
    public void write() throws Exception {
        Gson gson = init();
        String json = gson.toJson(LocalType.TYPE);

        assertEquivalent("{\n" +
                "  \"kind\": \"test local type\",\n" +
                "  \"fields\": [\n" +
                "    {\n" +
                "      \"name\": \"id\",\n" +
                "      \"type\": \"int\",\n" +
                "      \"annotations\": [\n" +
                "        {\n" +
                "          \"_kind\": \"KeyAnnotation\",\n" +
                "          \"index\": 0\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"a different name\",\n" +
                "      \"type\": \"string\",\n" +
                "      \"annotations\": [\n" +
                "        {\n" +
                "          \"_kind\": \"KeyAnnotation\",\n" +
                "          \"index\": 1\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"data\",\n" +
                "      \"type\": \"string\",\n" +
                "      \"annotations\": [\n" +
                "        {\n" +
                "          \"_kind\": \"isJsonContent\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"value\",\n" +
                "      \"type\": \"double\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"annotations\": [\n" +
                "    {\n" +
                "      \"_kind\": \"requiredAnnotationType\"\n" +
                "    }\n" +
                "  ]\n" +
                "}", json);
    }

    private Gson init(GlobType... types) {
        GlobModel globTypes = new DefaultGlobModel(new DefaultGlobModel(AllAnnotations.MODEL, types), LocalType.TYPE, IsJsonContentType.TYPE);
        return GlobsGson.create(globTypes::getType);
    }

    @Test
    public void readWriteGlobType() throws Exception {
        Gson gson = init();
        String json = gson.toJson(LocalType.TYPE);
        GlobType type = gson.fromJson(json, GlobType.class);
        Assert.assertEquals(LocalType.TYPE.getName(), type.getName());
        Assert.assertEquals(LocalType.TYPE.getFieldCount(), type.getFieldCount());
        Assert.assertEquals(json, gson.toJson(type));
    }

    @Test
    public void readWriteGlobTypeUsingJsonTree() throws Exception {
        Gson gson = init();
        JsonElement json = gson.toJsonTree(LocalType.TYPE);
        GlobType type = gson.fromJson(json, GlobType.class);
        Assert.assertEquals(LocalType.TYPE.getName(), type.getName());
        Assert.assertEquals(LocalType.TYPE.getFieldCount(), type.getFieldCount());
        Assert.assertEquals(gson.toJson(LocalType.TYPE), gson.toJson(type));
    }

    @Test
    public void readWriteGlob() throws Exception {
        MutableGlob instantiate = LocalType.TYPE.instantiate();
        MutableGlob glob = instantiate.set(LocalType.ID, 1)
                .set(LocalType.NAME, "name 1")
                .set(LocalType.DATA, "{\"sub\":\"aaa\"}")
                .set(LocalType.VALUE, 3.14159);
        Gson gson = init();

        String toJson = gson.toJson(glob);
        Assert.assertEquals(A_GLOB, toJson);
        Glob glob1 = gson.fromJson(A_GLOB, Glob.class);
        Assert.assertEquals("name 1", glob1.get(LocalType.NAME));
        Assert.assertEquals("{\"sub\":\"aaa\"}", glob1.get(LocalType.DATA));
        Assert.assertEquals(3.14159, glob1.get(LocalType.VALUE), 0.001);
    }

    @Test
    public void writeDoubleWithoutDecimalGlob() throws Exception {
        MutableGlob instantiate = LocalType.TYPE.instantiate();
        MutableGlob glob = instantiate.set(LocalType.ID, 1)
                .set(LocalType.NAME, "name \" \n 1")
                .set(LocalType.VALUE, 2);
        Gson gson = init();

        String toJson = gson.toJson(glob);
        Assert.assertEquals("{\"_kind\":\"test local type\",\"id\":1,\"a different name\":\"name \\\" \\n 1\",\"value\":2.0}", toJson);
        Glob decoded = gson.fromJson(toJson, Glob.class);
        Assert.assertEquals("name \" \n 1", decoded.get(LocalType.NAME));
        Assert.assertEquals(2., decoded.get(LocalType.VALUE), 00001);
    }

    @Test
    public void withNullValue() throws Exception {
        Gson gson = init();

        String toJson = "{\n" +
                "  \"_kind\": \"test local type\",\n" +
                "  \"id\": 1,\n" +
                "  \"a different name\": null,\n" +
                "  \"value\": null\n" +
                "}";
        Glob decoded = gson.fromJson(toJson, Glob.class);
        Assert.assertNull( decoded.get(LocalType.NAME));
        Assert.assertNull( decoded.get(LocalType.VALUE));
    }



    static class Data {
        Glob glob;
        GlobType globType;
        Key key;
    }

    @Test
    public void GlobInStruct() throws Exception {
        Data data = new Data();
        Gson gson = init();

        String toJson = gson.toJson(data);
        Data decoded = gson.fromJson(toJson, Data.class);
        Assert.assertNull(decoded.glob);
        Assert.assertNull(decoded.globType);
        Assert.assertNull(decoded.key);
    }

    public static void assertEquivalent(String expected, String actual) {
        Assert.assertEquals(GSonUtils.normalize(expected), GSonUtils.normalize(actual));
    }

    @Test
    public void checkGlobAttributeInDifferentOrderOrder() {
        final String DATA = "{\"id\":1,\"a different name\":\"name 1\",\"data\":{\"sub\":\"aaa\"},\"_kind\":\"test local type\",\"value\":3.14159}";
        Gson gson = init();
        Glob glob1 = gson.fromJson(DATA, Glob.class);
        Assert.assertEquals("name 1", glob1.get(LocalType.NAME));
        Assert.assertEquals("{\"sub\":\"aaa\"}", glob1.get(LocalType.DATA));
        Assert.assertEquals(3.14159, glob1.get(LocalType.VALUE), 0.001);
    }

    @Test
    public void emptyGlob() {
        final String DATA = "{}";
        Gson gson = init();
        Glob glob1 = gson.fromJson(DATA, Glob.class);
        Assert.assertNull(glob1);
    }

    @Test
    public void emptyWithOnlyKindGlob() {
        final String DATA = "{\"_kind\":\"test local type\"}";
        Gson gson = init();
        Glob glob1 = gson.fromJson(DATA, Glob.class);
        Assert.assertNotNull(glob1);
        Assert.assertEquals(LocalType.TYPE, glob1.getType());
    }

    public static final String ALL_WITH_KIND_IN_THE_MIDDLE = "{\n" +
            "  \"int\": 12,\n" +
            "  \"intArray\": [\n" +
            "    0,\n" +
            "    1,\n" +
            "    2,\n" +
            "    3,\n" +
            "    4,\n" +
            "    5,\n" +
            "    6,\n" +
            "    7,\n" +
            "    8,\n" +
            "    9,\n" +
            "    10,\n" +
            "    11,\n" +
            "    12,\n" +
            "    13,\n" +
            "    14,\n" +
            "    15,\n" +
            "    16,\n" +
            "    17,\n" +
            "    18,\n" +
            "    19,\n" +
            "    20,\n" +
            "    21,\n" +
            "    22,\n" +
            "    23,\n" +
            "    24,\n" +
            "    25,\n" +
            "    26,\n" +
            "    27,\n" +
            "    28,\n" +
            "    29,\n" +
            "    30,\n" +
            "    31,\n" +
            "    32,\n" +
            "    33\n" +
            "  ],\n" +
            "  \"boolean\": true,\n" +
            "  \"booleanArray\": [\n" +
            "    true,\n" +
            "    false,\n" +
            "    true\n" +
            "  ],\n" +
            "  \"String\": \"a string\",\n" +
            "  \"StringArray\": [\n" +
            "    \"un\",\n" +
            "    \"deux\",\n" +
            "    \"trois\"\n" +
            "  ],\n" +
            "  \"JsonObjectString\": {\n" +
            "    \"arg1\": \"vale1\",\n" +
            "    \"v\": 2\n" +
            "  },\n" +
            "  \"_kind\": \"AllType\",\n" +
            "  \"JsonObjectStringArray\": [\n" +
            "    {\n" +
            "      \"arg1\": \"value1\",\n" +
            "      \"v\": 2\n" +
            "    },\n" +
            "    {\n" +
            "      \"arg1\": \"value3\",\n" +
            "      \"v\": 3\n" +
            "    }\n" +
            "  ],\n" +
            "  \"JsonArrayString\": [\n" +
            "    \"a\",\n" +
            "    \"b\"\n" +
            "  ],\n" +
            "  \"JsonArrayStringArray\": [\n" +
            "    [\n" +
            "      \"a\",\n" +
            "      \"b\"\n" +
            "    ],\n" +
            "    [\n" +
            "      \"a\",\n" +
            "      \"b\"\n" +
            "    ]\n" +
            "  ],\n" +
            "  \"long\": 2,\n" +
            "  \"longArray\": [\n" +
            "    0,\n" +
            "    1,\n" +
            "    2,\n" +
            "    3,\n" +
            "    4,\n" +
            "    5,\n" +
            "    6,\n" +
            "    7,\n" +
            "    8,\n" +
            "    9,\n" +
            "    10,\n" +
            "    11,\n" +
            "    12,\n" +
            "    13,\n" +
            "    14,\n" +
            "    15,\n" +
            "    16,\n" +
            "    17,\n" +
            "    18,\n" +
            "    19,\n" +
            "    20,\n" +
            "    21,\n" +
            "    22,\n" +
            "    23,\n" +
            "    24,\n" +
            "    25,\n" +
            "    26,\n" +
            "    27,\n" +
            "    28,\n" +
            "    29,\n" +
            "    30,\n" +
            "    31,\n" +
            "    32,\n" +
            "    33,\n" +
            "    34,\n" +
            "    35,\n" +
            "    36,\n" +
            "    37,\n" +
            "    38,\n" +
            "    39,\n" +
            "    40,\n" +
            "    41,\n" +
            "    42,\n" +
            "    43,\n" +
            "    44,\n" +
            "    45,\n" +
            "    46,\n" +
            "    47,\n" +
            "    48,\n" +
            "    49\n" +
            "  ],\n" +
            "  \"bigDecimal\": 2.2,\n" +
            "  \"bigDecimalArray\": [\n" +
            "    2.2,\n" +
            "    4.2\n" +
            "  ],\n" +
            "  \"double\": 3.1415,\n" +
            "  \"doubleArray\": [\n" +
            "    3.3,\n" +
            "    4.4,\n" +
            "    5.5\n" +
            "  ],\n" +
            "  \"date\": \"2018-02-04\",\n" +
            "  \"dateTime\": \"2018-02-04T15:45:34.000001+01:00[Europe/Paris]\",\n" +
            "  \"blob\": \"AwQ\\u003d\"\n," +
            "  \"glob\":{\"value\":3.0,\"date\":17000}," +
            "  \"globArray\":[{\"value\":3.0,\"date\":17000},{\"value\":2.8,\"date\":17001},{\"value\":2.7,\"date\":17002}]" +
            "}";

    @Test
    public void readAllFieldType() {
        GlobTypeBuilder innerGlobTypeBuilder = DefaultGlobTypeBuilder.init("histo");
        DoubleField valueField = innerGlobTypeBuilder.declareDoubleField("value");
        IntegerField dateField = innerGlobTypeBuilder.declareIntegerField("date");
        GlobType innerType = innerGlobTypeBuilder.get();

        GlobTypeBuilder globTypeBuilder = DefaultGlobTypeBuilder.init("AllType");
        IntegerField anInt = globTypeBuilder.declareIntegerField("int");
        IntegerArrayField intArray = globTypeBuilder.declareIntegerArrayField("intArray");
        BooleanField aBoolean = globTypeBuilder.declareBooleanField("boolean");
        BooleanArrayField booleanArray = globTypeBuilder.declareBooleanArrayField("booleanArray");
        StringField string = globTypeBuilder.declareStringField("String");
        StringArrayField stringArray = globTypeBuilder.declareStringArrayField("StringArray");
        StringField jsonObjectString = globTypeBuilder.declareStringField("JsonObjectString", IsJsonContentType.UNIQUE_GLOB);
        StringArrayField jsonObjectStringArray = globTypeBuilder.declareStringArrayField("JsonObjectStringArray", IsJsonContentType.UNIQUE_GLOB);
        StringField jsonArrayString = globTypeBuilder.declareStringField("JsonArrayString", IsJsonContentType.UNIQUE_GLOB);
        StringArrayField jsonArrayStringArray = globTypeBuilder.declareStringArrayField("JsonArrayStringArray", IsJsonContentType.UNIQUE_GLOB);
        LongField aLong = globTypeBuilder.declareLongField("long");
        LongArrayField longArray = globTypeBuilder.declareLongArrayField("longArray");
        BigDecimalField bigDecimal = globTypeBuilder.declareBigDecimalField("bigDecimal");
        BigDecimalArrayField bigDecimalArray = globTypeBuilder.declareBigDecimalArrayField("bigDecimalArray");
        DoubleField aDouble = globTypeBuilder.declareDoubleField("double");
        DoubleArrayField doubleArray = globTypeBuilder.declareDoubleArrayField("doubleArray");
        DateField date = globTypeBuilder.declareDateField("date");
        DateTimeField dateTime = globTypeBuilder.declareDateTimeField("dateTime");
        BlobField blob = globTypeBuilder.declareBlobField("blob");
        GlobField globField = globTypeBuilder.declareGlobField("glob", innerType);
        GlobArrayField globArrayField = globTypeBuilder.declareGlobArrayField("globArray", innerType);
        GlobType globType = globTypeBuilder.get();

        Gson gson = init(globType, innerType);

        //write-read globType
        String s = gson.toJson(globType);
        GlobType readType = gson.fromJson(s, GlobType.class);

        for (Field field : globType.getFields()) {
            Field readTypeField = readType.getField(field.getName());
            Assert.assertTrue(readTypeField.getClass() == field.getClass());
        }

        MutableGlob instantiate = globType.instantiate();
        instantiate
                .set(anInt, 12)
                .set(intArray, IntStream.range(0, 34).toArray())
                .set(aBoolean, true)
                .set(booleanArray, new boolean[]{true, false, true})
                .set(string, "a string")
                .set(stringArray, new String[]{"un", "deux", "trois"})
                .set(jsonObjectString, "{\"arg1\":\"vale1\",\"v\":2}")
                .set(jsonObjectStringArray, new String[]{"{\"arg1\":\"value1\",\"v\":2}", "{\"arg1\":\"value3\",\"v\":3}"})
                .set(jsonArrayString, "[\"a\",\"b\"]")
                .set(jsonArrayStringArray, new String[]{"[\"a\",\"b\"]", "[\"a\",\"b\"]"})
                .set(aLong, 2L)
                .set(longArray, LongStream.range(0, 50).toArray())
                .set(bigDecimal, BigDecimal.valueOf(2.2))
                .set(bigDecimalArray, new BigDecimal[]{BigDecimal.valueOf(2.2), BigDecimal.valueOf(4.2)})
                .set(aDouble, 3.1415)
                .set(doubleArray, new double[]{3.3, 4.4, 5.5})
                .set(date, LocalDate.of(2018, 2, 4))
                .set(dateTime, ZonedDateTime.of(2018, 2, 4, 15, 45, 34, 1000, ZoneId.of("Europe/Paris")))
                .set(blob, new byte[]{3, 4})
                .set(globField, innerType.instantiate().set(valueField, 3).set(dateField, 17000))
                .set(globArrayField, new Glob[] {innerType.instantiate().set(valueField, 3).set(dateField, 17000),
                        innerType.instantiate().set(valueField, 2.8).set(dateField, 17001),
                        innerType.instantiate().set(valueField, 2.7).set(dateField, 17002)});

        String jsonOutput = gson.toJson(instantiate);
        System.out.println("GlobsGsonAdapterTest.readAllFieldType " + jsonOutput);

        Glob glob = gson.fromJson(jsonOutput, Glob.class);

        Field[] fields = globType.getFields();
        for (Field field : fields) {
            Assert.assertTrue(field.getName() + " : " + glob.getValue(field) + "->" + instantiate.getValue(field),
                    field.valueEqual(glob.getValue(field), instantiate.getValue(field)));
        }

        glob = gson.fromJson(ALL_WITH_KIND_IN_THE_MIDDLE, Glob.class);

        fields = globType.getFields();
        for (Field field : fields) {
            Assert.assertTrue(field.getName() + " : " +
                            field.toString(glob.getValue(field), "") + "->" + field.toString(instantiate.getValue(field), ""),
                    field.valueEqual(glob.getValue(field), instantiate.getValue(field)));
        }
    }

    @Test
    public void withUnknownField() {
        Gson gson = init();
        Glob glob = gson.fromJson("{\"_kind\": \"test local type\", \"a different name\": \"my name\", \"PI\": 3.3}", Glob.class);
        Assert.assertEquals(glob.get(LocalType.NAME),"my name");

        glob = gson.fromJson("{\"_kind\": \"test local type\", \"a different name\": \"my name\", \"PI\": { \"O\":3.3 }}", Glob.class);
        Assert.assertEquals(glob.get(LocalType.NAME),"my name");

        glob = gson.fromJson("{\"_kind\": \"test local type\", \"a different name\": \"my name\", \"PI\": []}", Glob.class);
        Assert.assertEquals(glob.get(LocalType.NAME),"my name");

    }

    public static class ComplexClassName {
        public static GlobType TYPE;

        public static StringField DATA;

        static {
            GlobTypeLoaderFactory.create(ComplexClassName.class, "#$\"\\é&à@", true).load();
        }
    }

    @Test
    public void withCharacterToEscape() {
        Gson gson = GlobsGson.create(name -> null);
        String jsonType = gson.toJson(ComplexClassName.TYPE);
        Assert.assertEquals("{\"kind\":\"#$\\\"\\\\é\\u0026à@\",\"fields\":[{\"name\":\"data\",\"type\":\"string\"}]}", jsonType);

        GlobType globType = gson.fromJson(jsonType, GlobType.class);
        Assert.assertEquals(ComplexClassName.TYPE.getName(),globType.getName());
    }

    @Test
    public void failParse() throws UnsupportedEncodingException {
        Gson gson = GlobsGson.create(name -> null);
        String jsonType = "{\"kind\":\"with \\u0026éè\",\"fields\":{\"name\":{\"type\":\"string\"}}}";
        JsonElement root = new JsonParser().parse(new InputStreamReader(new ByteArrayInputStream(jsonType.getBytes("UTF-8")), "UTF-8"));

        root.getAsJsonObject();
        GlobType globType = gson.fromJson(jsonType, GlobType.class);
        Assert.assertEquals("with &éè", globType.getName());
    }

}
