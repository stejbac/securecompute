package securecompute.circuit.cryptography;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.junit.jupiter.api.Test;
import securecompute.algebra.Gf256;
import securecompute.circuit.AlgebraicFunction;
import securecompute.circuit.ArithmeticCircuit;
import securecompute.helper.WithDefaultField;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static securecompute.circuit.ArithmeticCircuitTest.checkAlgebraicFunction;
import static securecompute.circuit.cryptography.Aes.AES_FIELD;

class AesTest implements WithDefaultField<Gf256.Element> {

    @Override
    public Gf256 getDefaultStructure() {
        return AES_FIELD;
    }

    private static final List<Gf256.Element> AFFINE_TRANSFORM_MATRIX_COLUMNS = LongStream.of(
            0b00011111,
            0b00111110,
            0b01111100,
            0b11111000,
            0b11110001,
            0b11100011,
            0b11000111,
            0b10001111
    ).mapToObj(AES_FIELD::element).collect(ImmutableList.toImmutableList());

    private static final Gf256.Element AFFINE_TRANSFORM_OFFSET = AES_FIELD.element(0b01100011); // 0x63

    private static final List<Gf256.Element> FROBENIUS_MAP_MULTIPLIERS = LongStream.of(
            0b00101001, // 0x29
            0b10110000, // 0xb0
            0b01011000, // 0x58
            0b00000101, // 0x05
            0b10100110, // 0xa6
            0b01010011, // 0x53
            0b10100100, // 0xa4
            0b01010010  // 0x52
    ).mapToObj(AES_FIELD::element).collect(ImmutableList.toImmutableList());

    @Test
    void testCircuit() {
        ArithmeticCircuit<Gf256.Element> aesSBoxCircuit = Aes.aesSBoxCircuit();

        System.out.println(aesSBoxCircuit);
        System.out.println(aesSBoxCircuit.gatesInTopologicalOrder());

        AlgebraicFunction<Gf256.Element> aesSBox = Aes.aesSBox();

        System.out.println(aesSBox);
        System.out.println(aesSBox.apply(ImmutableList.of(zero())));
        System.out.println(aesSBox.apply(ImmutableList.of(one())));
        System.out.println(aesSBox.apply(ImmutableList.of(AES_FIELD.element(2))));

        checkAlgebraicFunction(Aes.reciprocalOrZeroFn());
        checkAlgebraicFunction(Aes.affineTransformFn());
        checkAlgebraicFunction(aesSBox);

        System.out.println(Aes.aesRoundCircuit());

        System.out.println(Aes.mixColumnFn().apply(
                LongStream.of(0xdb, 0x13, 0x53, 0x45).mapToObj(AES_FIELD::element).collect(ImmutableList.toImmutableList())));

        System.out.println(Aes.aesRoundFn().apply(Collections.nCopies(32, zero())));

        AES_FIELD.getElements().forEach(x ->
                assertEquals(affineTransform(x), Aes.affineTransformFn().apply(ImmutableList.of(x)).get(0))
        );
    }

    private Gf256.Element affineTransform(Gf256.Element x) {
        byte acc = AFFINE_TRANSFORM_OFFSET.getValue();
        for (int i = 0; i < 8; i++) {
            acc ^= (x.getValue() & 1 << i) != 0 ? AFFINE_TRANSFORM_MATRIX_COLUMNS.get(i).getValue() : 0;
        }
        return AES_FIELD.element(acc);
    }

    @Test
    void testFrobenius() {
        for (Gf256.Element c : FROBENIUS_MAP_MULTIPLIERS) {
            for (int i = 0; i < 8; i++) {
                Gf256.Element x = AES_FIELD.element(1 << i).multiply(c);
                Gf256.Element y = x.add(x.pow(2)).add(x.pow(4)).add(x.pow(8))
                        .add(x.pow(16)).add(x.pow(32)).add(x.pow(64)).add(x.pow(128));
                System.out.println(x + " -> " + y);
            }
            System.out.println();
        }
    }

    @Test
    void testLinearTransform() {
        //noinspection UnstableApiUsage
        List<Gf256.Element> coefficients = IntStream.range(0, 8)
                .mapToObj(i -> sum(
                        Streams.zip(AFFINE_TRANSFORM_MATRIX_COLUMNS.stream(), FROBENIUS_MAP_MULTIPLIERS.stream(),
                                (a, b) -> a.multiply(b.pow(1L << i)))))
                .collect(ImmutableList.toImmutableList());

        System.out.println(coefficients);

        for (int i = 0; i < 8; i++) {
            Gf256.Element x = AES_FIELD.element(1 << i);
            Gf256.Element y = zero();
            for (int j = 0; j < 8; j++) {
                y = y.add(coefficients.get(j).multiply(x.pow(1 << j)));
            }
            System.out.println(x + " -> " + y);
        }
    }
}
