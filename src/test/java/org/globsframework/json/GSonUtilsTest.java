package org.globsframework.json;

import org.globsframework.json.annottations.JsonDateTimeFormatAnnotation;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
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
                .set(LocalType.ID, 23)
                .set(LocalType.NAME, "TEST")
        );
        ZonedDateTime arrival = ZonedDateTime.of(2019, 9, 13, 13, 15, 21, 0, ZoneId.systemDefault());
        writeGlob.push(LocalType.TYPE.instantiate()
                .set(LocalType.ID, 24)
                .set(LocalType.NAME, "TEST éè")
                .set(LocalType.ARRIVAL, arrival)
        );
        writeGlob.end();
        String expected = "[{\"id\":23,\"name\":\"TEST\"},{\"id\":24,\"name\":\"TEST éè\",\"arrival\":\"2019-09-13 13:15:21\"}]";
        Assert.assertEquals(expected, writer.toString());

        {
            Glob decode = GSonUtils.decode(new StringReader("{\"id\":24,\"name\":\"TEST éè\",\"arrival\":\"2019-09-13 13:15:21\"}"), LocalType.TYPE);
            Assert.assertEquals("TEST éè", decode.get(LocalType.NAME));
            Assert.assertEquals(arrival, decode.get(LocalType.ARRIVAL));
        }
        {
            Glob decode = GSonUtils.decode(new StringReader("{\"id\":24,\"name\":\"TEST éè\",\"arrival\":\"0000\"}"), LocalType.TYPE);
            Assert.assertNull(decode.get(LocalType.ARRIVAL));
        }
    }

    public static class LocalType {
        @Required
        public static GlobType TYPE;

        @KeyField
        public static IntegerField ID;

        public static StringField NAME;

        @JsonDateTimeFormatAnnotation(pattern = "yyyy-MM-dd HH:mm:ss", asLocal = true, nullValue = "0000")
        public static DateTimeField ARRIVAL;


        static {
            GlobTypeLoaderFactory.create(LocalType.class, "test local type")
                    .load();
        }
    }

}