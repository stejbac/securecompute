package securecompute;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Streams;
import com.google.common.collect.UnmodifiableIterator;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class StreamUtils {
    private StreamUtils() {
    }

    public static <T> Iterator<T> takeWhile(Iterator<T> iterator, Predicate<? super T> predicate) {
        return new AbstractIterator<T>() {
            @Override
            protected T computeNext() {
                T next;
                return iterator.hasNext() && predicate.test(next = iterator.next()) ? next : endOfData();
            }
        };
    }

    public static <T> Stream<T> takeWhile(Stream<T> stream, Predicate<? super T> predicate) {
        return Streams.stream(takeWhile(stream.iterator(), predicate));
    }

    public static <T> Stream<T> iterate(T seed, Predicate<? super T> hasNext, UnaryOperator<T> next) {
        return takeWhile(Stream.iterate(seed, next), hasNext);
    }

    public static <A, B, R> Iterator<R> zip(Iterator<A> iteratorA, Iterator<B> iteratorB, BiFunction<A, B, R> function) {
        return new UnmodifiableIterator<R>() {
            @Override
            public boolean hasNext() {
                return iteratorA.hasNext() && iteratorB.hasNext();
            }

            @Override
            public R next() {
                return function.apply(iteratorA.next(), iteratorB.next());
            }
        };
    }

    public static <A, B, R> Stream<R> zip(Stream<A> streamA, Stream<B> streamB, BiFunction<A, B, R> function) {
        return Streams.stream(zip(streamA.iterator(), streamB.iterator(), function));
    }
}
