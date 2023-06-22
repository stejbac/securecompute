package securecompute.algebra;

import org.junit.jupiter.api.Test;

import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class Gf256Test {
    @Test
    void testNew() {
        for (int n = 256; n < 511; n++) {
            try {
//                new Gf256(n, 3);
                new Gf256(n);
                System.out.println("Primitive: 0b" + Integer.toBinaryString(n));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }

        Gf256 aesField = new Gf256(0b100011011, 0b11);
        Gf256 whirlpoolField = new Gf256(0b100011101);

        System.out.println(aesField.element(0b110).pow(2));
        System.out.println(whirlpoolField.element(0b110).pow(2));
        System.out.println(aesField.exp(51));
        System.out.println(whirlpoolField.exp(51));
        System.out.println(aesField.exp(102));
        System.out.println(whirlpoolField.exp(102));
        System.out.println(aesField.exp(153));
        System.out.println(whirlpoolField.exp(153));
        System.out.println(aesField.exp(204));
        System.out.println(whirlpoolField.exp(204));
        System.out.println();

        assertEquals(0b100001, findValidQuadraticExtension(aesField, "aesField"));
        System.out.println();
        assertEquals(0b100010, findValidQuadraticExtension(whirlpoolField, "whirlpoolField"));

        Gf65536 quadraticField = new Gf65536(aesField, 0b100001, 0b1);
        assertEquals("(0b00000000, 0b00000000)", quadraticField.zero().toString());
        assertEquals("(0b00000001, 0b00000000)", quadraticField.one().toString());
        assertEquals("(0b00000000, 0b00000001)", quadraticField.getPrimitiveElement().toString());

        long primitiveOrder = LongStream.range(0, 65535)
                .mapToObj(quadraticField.getPrimitiveElement()::pow)
                .distinct()
                .count();
        assertEquals(65535, primitiveOrder);
        assertEquals(quadraticField.one(), quadraticField.getPrimitiveElement().pow(65535));
    }

    private static int findValidQuadraticExtension(Gf256 baseField, String baseFieldName) {
        for (int i = 0; i < 256; i++) {
            String fieldName = "Gf65536(" + baseFieldName + ", 0b" + Integer.toBinaryString(i) + ", 0b1)";
            try {
                new Gf65536(baseField, i, 0b1);
            } catch (IllegalArgumentException e) {
                System.out.println(fieldName + ": " + e.getMessage());
                continue;
            }
            System.out.println("Got solution: " + fieldName);
            return i;
        }
        return fail("Could not find valid quadratic extension of " + baseFieldName);
    }
}
