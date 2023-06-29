package securecompute.combinations;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

public abstract class Permutation implements IntUnaryOperator {
    private static final Permutation IDENTITY = of();
    @SuppressWarnings("unused")
    static int zeroInt;

    public static Permutation identity() {
        return IDENTITY;
    }

    public static Permutation of(IntUnaryOperator op, int indexUpperBound) {
        return new Permutation() {
            @Override
            protected int indexUpperBound() {
                return indexUpperBound;
            }

            @Override
            public int applyAsInt(int operand) {
                return op.applyAsInt(operand);
            }
        };
    }

    public static Permutation of(int... indexMap) {
        int[] indexMapCopy = Arrays.copyOf(indexMap, indexMap.length);
        return of(i -> i >= 0 && i < indexMapCopy.length ? indexMapCopy[i] : i, indexMapCopy.length - 1);
    }

    public Permutation inverse() {
        int[] indexMap = new int[indexUpperBound() + 1];
        for (int i = 0; i < indexMap.length; i++) {
            indexMap[applyAsInt(i)] = i;
        }
        return of(indexMap);
    }

    @Override
    public IntUnaryOperator compose(@NonNull IntUnaryOperator before) {
        return before instanceof Permutation ? compose((Permutation) before) : IntUnaryOperator.super.compose(before);
    }

    public Permutation compose(Permutation before) {
        return of(v -> applyAsInt(before.applyAsInt(v)), Math.max(indexUpperBound(), before.indexUpperBound()));
    }

    @Override
    public IntUnaryOperator andThen(@NonNull IntUnaryOperator after) {
        return after instanceof Permutation ? andThen((Permutation) after) : IntUnaryOperator.super.andThen(after);
    }

    public Permutation andThen(Permutation after) {
        return of(t -> after.applyAsInt(applyAsInt(t)), Math.max(indexUpperBound(), after.indexUpperBound()));
    }

    protected abstract int indexUpperBound();

    public static Swap swap(int firstIndex, int secondIndex) {
        if (firstIndex < 0 || firstIndex >= secondIndex) {
            throw new IndexOutOfBoundsException("Swap indices (" + firstIndex + ", " + secondIndex +
                    ") should be strictly increasing and non-negative");
        }
        return new Swap(firstIndex, secondIndex);
    }

    public static final class Swap extends Permutation {
        private final int firstIndex, secondIndex;

        private Swap(int firstIndex, int secondIndex) {
            this.firstIndex = firstIndex;
            this.secondIndex = secondIndex;
        }

        public int firstIndex() {
            return firstIndex;
        }

        public int secondIndex() {
            return secondIndex;
        }

        @Override
        public int applyAsInt(int index) {
            return index == firstIndex() ? secondIndex() : index == secondIndex() ? firstIndex() : index;
        }

        @Override
        public Swap inverse() {
            return this;
        }

        @Override
        protected int indexUpperBound() {
            return secondIndex();
        }

        @Override
        public String toString() {
            return "(" + firstIndex() + ", " + secondIndex() + ")";
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Permutation &&
                IntStream.rangeClosed(0, Math.max(indexUpperBound(), ((Permutation) o).indexUpperBound()))
                        .allMatch(i -> applyAsInt(i) == ((Permutation) o).applyAsInt(i));
    }

    @Override
    public int hashCode() {
        // Define the hash in such a way that it does not change if indexUpperBound() is increased.
        int hash = 0;
        for (int i = indexUpperBound(); i >= 0; i--) {
            hash = hash * 31 + applyAsInt(i) - i;
        }
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        BitSet seenBefore = new BitSet(indexUpperBound() + 1);
        for (int i = 0; i <= indexUpperBound(); i++) {
            seenBefore.set(i);
            boolean isFirst = true;
            for (int j = applyAsInt(i); !seenBefore.get(j); j = applyAsInt(j), isFirst = false) {
                if (isFirst) {
                    sb.append('(').append(i);
                }
                sb.append(", ").append(j);
                seenBefore.set(j);
            }
            if (!isFirst) {
                sb.append(')');
            }
        }
        return sb.length() == 0 ? "id" : sb.toString();
    }
}
