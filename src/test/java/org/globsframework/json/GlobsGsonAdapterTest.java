package org.globsframework.json;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.globsframework.metamodel.*;
import org.globsframework.metamodel.annotations.AnnotationModel;
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
            GlobTypeLoaderFactory.create(LocalType.class, "test local type")
                    .load();
        }
    }

    @Test
    public void write() throws Exception {
        Gson gson = init();
        String json = gson.toJson(LocalType.TYPE);

        assertEquivalent("{\n" +
                "  \"kind\": \"test local type\",\n" +
                "  \"fields\": {\n" +
                "    \"id\": {\n" +
                "      \"type\": \"int\",\n" +
                "      \"annotations\": [\n" +
                "        {\n" +
                "          \"_kind\": \"KeyAnnotation\",\n" +
                "          \"index\": 0\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"a different name\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"annotations\": [\n" +
                "        {\n" +
                "          \"_kind\": \"KeyAnnotation\",\n" +
                "          \"index\": 1\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"data\": {\n" +
                "      \"type\": \"string\",\n" +
                "      \"annotations\": [\n" +
                "        {\n" +
                "          \"_kind\": \"isJsonContent\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"value\": {\n" +
                "      \"type\": \"double\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"annotations\":[" +
                "     {\"_kind\":\"requiredAnnotationType\"}" +
                "  ]" +
                "}" +
                "\n", json);
    }

    private Gson init(GlobType... types) {
        GlobModel globTypes = new DefaultGlobModel(new DefaultGlobModel(AnnotationModel.MODEL, types), LocalType.TYPE, IsJsonContentType.TYPE);
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
                .set(LocalType.NAME, "name 1")
                .set(LocalType.VALUE, 2);
        Gson gson = init();

        String toJson = gson.toJson(glob);
        Assert.assertEquals("{\"_kind\":\"test local type\",\"id\":1,\"a different name\":\"name 1\",\"value\":2.0}", toJson);
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
        JsonParser jsonParser = new JsonParser();
        JsonElement expectedTree = jsonParser.parse(expected);
        JsonElement actualTree = jsonParser.parse(actual);
        Gson gson = new Gson();
        Assert.assertEquals(gson.toJson(expectedTree), gson.toJson(actualTree));
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
            "  \"blob\": \"AwQ\\u003d\"\n" +
            "}";

    @Test
    public void readAllFieldType() {
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
        LongArrayField longArray = globTypeBuilder.declareArrayLongField("longArray");
        BigDecimalField bigDecimal = globTypeBuilder.declareBigDecimalField("bigDecimal");
        BigDecimalArrayField bigDecimalArray = globTypeBuilder.declareBigDecimalArrayField("bigDecimalArray");
        DoubleField aDouble = globTypeBuilder.declareDoubleField("double");
        DoubleArrayField doubleArray = globTypeBuilder.declareDoubleArrayField("doubleArray");
        DateField date = globTypeBuilder.declareDateField("date");
        DateTimeField dateTime = globTypeBuilder.declareDateTimeField("dateTime");
        BlobField blob = globTypeBuilder.declareBlobField("blob");
        GlobType globType = globTypeBuilder.get();

        Gson gson = init(globType);

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
        ;

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
                            field.toString(glob.getValue(field)) + "->" + field.toString(instantiate.getValue(field)),
                    field.valueEqual(glob.getValue(field), instantiate.getValue(field)));
        }
    }
}
