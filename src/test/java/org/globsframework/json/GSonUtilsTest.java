package org.globsframework.json;

import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.KeyField;
import org.globsframework.metamodel.annotations.Required;
import org.globsframework.metamodel.fields.IntegerField;
import org.globsframework.metamodel.fields.StringField;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;

public class GSonUtilsTest {

    @Test
    public void globWriterTest() {
        StringWriter writer = new StringWriter();
        GSonUtils.WriteGlob writeGlob = new GSonUtils.WriteGlob(writer,false);
        writeGlob.push(LocalType.TYPE.instantiate()
                .set(LocalType.ID, 23)
                .set(LocalType.NAME, "TEST")
        );
        writeGlob.push(LocalType.TYPE.instantiate()
                .set(LocalType.ID, 24)
                .set(LocalType.NAME, "TEST éè")
        );
        writeGlob.end();
        Assert.assertEquals("[{\"id\":23,\"name\":\"TEST\"},{\"id\":24,\"name\":\"TEST éè\"}]", writer.toString());
    }

    public static class LocalType {
        @Required
        public static GlobType TYPE;

        @KeyField
        public static IntegerField ID;

        public static StringField NAME;


        static {
            GlobTypeLoaderFactory.create(LocalType.class, "test local type")
                    .load();
        }
    }

}