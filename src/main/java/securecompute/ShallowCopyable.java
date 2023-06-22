package securecompute;

public abstract class ShallowCopyable {
    protected abstract Object shallowCopy();

    @SuppressWarnings("unchecked")
    public static <E> E tryClone(E original) {
        Object clone;
        return original instanceof ShallowCopyable &&
                original.equals(clone = ((ShallowCopyable) original).shallowCopy()) ? (E) clone : original;
    }
}
