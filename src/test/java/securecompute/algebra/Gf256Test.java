package securecompute.algebra;

import org.junit.jupiter.api.Test;

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

//        Gf65536 quadraticField = new Gf65536(whirlpoolField, 0b11, 0b11);
        Gf65536 quadraticField = new Gf65536(aesField, 0b10, 0b10);
        for (int n = 0; n < 65536; n++) {
            System.out.println(quadraticField.getPrimitiveElement().pow(n));
        }
    }
}
