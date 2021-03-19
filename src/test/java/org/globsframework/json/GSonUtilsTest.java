package org.globsframework.json;

import org.globsframework.json.annottations.AllAnnotations;
import org.globsframework.json.annottations.JsonDateTimeFormatAnnotation;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.FieldNameAnnotationType;
import org.globsframework.metamodel.annotations.KeyAnnotationType;
import org.globsframework.metamodel.annotations.KeyField;
import org.globsframework.metamodel.annotations.Required;
import org.globsframework.metamodel.fields.DateTimeField;
import org.globsframework.metamodel.fields.IntegerField;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Glob;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class GSonUtilsTest {

    @Test
    public void globWriterTest() {
        StringWriter writer = new StringWriter();
        GSonUtils.WriteGlob writeGlob = new GSonUtils.WriteGlob(writer,false);
        writeGlob.push(LocalType.TYPE.instantiate()
                .set(LocalType.id, 23)
                .set(LocalType.name, "TEST")
        );
        ZonedDateTime arrival = ZonedDateTime.of(2019, 9, 13, 13, 15, 21, 0, ZoneId.systemDefault());
        writeGlob.push(LocalType.TYPE.instantiate()
                .set(LocalType.id, 24)
                .set(LocalType.name, "TEST éè")
                .set(LocalType.arrival, arrival)
        );
        writeGlob.end();
        String expected = "[{\"id\":23,\"name\":\"TEST\"},{\"id\":24,\"name\":\"TEST éè\",\"arrival\":\"2019-09-13 13:15:21\"}]";
        Assert.assertEquals(expected, writer.toString());

        {
            Glob decode = GSonUtils.decode(new StringReader("{\"id\":24,\"name\":\"TEST éè\",\"arrival\":\"2019-09-13 13:15:21\"}"), LocalType.TYPE);
            Assert.assertEquals("TEST éè", decode.get(LocalType.name));
            Assert.assertEquals(arrival, decode.get(LocalType.arrival));
        }
        {
            Glob decode = GSonUtils.decode(new StringReader("{\"id\":24,\"name\":\"TEST éè\",\"arrival\":\"0000\"}"), LocalType.TYPE);
            Assert.assertNull(decode.get(LocalType.arrival));
        }
    }

    @Test
    public void encodeDecodeGlobType() {
        String s = GSonUtils.encodeGlobType(LocalType.TYPE);
        System.out.println(s);
        GlobType type = GSonUtils.decodeGlobType(s, AllAnnotations.RESOLVER, false);
        Assert.assertTrue(type.getField("id").isKeyField());
        Assert.assertTrue(type.getField("id").hasAnnotation(KeyAnnotationType.UNIQUE_KEY));
        Assert.assertTrue(type.getField("arrival").hasAnnotation(JsonDateTimeFormatType.UNIQUE_KEY));
        Assert.assertEquals(LocalType.TYPE.getName(), type.getName());
        String s2 = GSonUtils.encodeGlobType(type);
    }


    public static class LocalType {
        @Required
        public static GlobType TYPE;

        @KeyField
        public static IntegerField id;

        public static StringField name;

        @JsonDateTimeFormatAnnotation(pattern = "yyyy-MM-dd HH:mm:ss", asLocal = true, nullValue = "0000")
        public static DateTimeField arrival;


        static {
            GlobTypeLoaderFactory.create(LocalType.class, "test local type", true)
                    .load();
        }
    }

}