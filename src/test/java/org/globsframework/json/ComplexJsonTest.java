package org.globsframework.json;

import junit.framework.Assert;
import org.globsframework.json.annottations.JsonAsObject;
import org.globsframework.json.annottations.JsonValueAsField;
import org.globsframework.metamodel.GlobType;
import org.globsframework.metamodel.GlobTypeLoaderFactory;
import org.globsframework.metamodel.annotations.Target;
import org.globsframework.metamodel.fields.GlobArrayField;
import org.globsframework.metamodel.fields.StringField;
import org.globsframework.model.Glob;
import org.globsframework.model.MutableGlob;
import org.junit.Test;

public class ComplexJsonTest {

    @Test
    public void name() {
        MutableGlob a = A.TYPE.instantiate()
                .set(A.a, "a")
                .set(A.b, new Glob[]{
                        B.TYPE.instantiate()
                                .set(B.name, "b1")
                                .set(B.otherField, "o1"),
                        B.TYPE.instantiate()
                                .set(B.name, "b2")
                                .set(B.otherField, "o2")
                });
        String encode = GSonUtils.encode(a, true);
        Assert.assertEquals("{\"_kind\":\"a\",\"a\":\"a\",\"b\":{\"b1\":{\"otherField\":\"o1\"},\"b2\":{\"otherField\":\"o2\"}}}", encode);


        Glob newA = GSonUtils.decode(encode, A.TYPE);
        Assert.assertEquals(2, newA.get(A.b).length);
        Assert.assertEquals("b1", newA.get(A.b)[0].get(B.name));
        Assert.assertEquals("o1", newA.get(A.b)[0].get(B.otherField));
        Assert.assertEquals("b2", newA.get(A.b)[1].get(B.name));
        Assert.assertEquals("o2", newA.get(A.b)[1].get(B.otherField));
    }



    public static class A {
        public static GlobType TYPE;

        public static StringField a;

        @Target(B.class)
        @JsonAsObject
        public static GlobArrayField b;

        static {
            GlobTypeLoaderFactory.create(A.class).load();
        }
    }

    public static class B {
        public static GlobType TYPE;

        @JsonValueAsField
        public static StringField name;

        public static StringField otherField;

        static {
            GlobTypeLoaderFactory.create(B.class).load();
        }
    }
}
