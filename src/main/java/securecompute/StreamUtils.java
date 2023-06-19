package securecompute;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Streams;

import java.util.Iterator;
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
}
