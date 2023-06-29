package securecompute.combinations;

import com.google.common.collect.ImmutableList;

import java.util.List;

public interface Structure<E> {
    Permutation.Swap SWAP_0_1 = Permutation.swap(0, 1);

    default E select(List<E> elements, int index) {
        return elements.get(index);
    }

    default void conditionalSwap(List<E> elements, Permutation.Swap swap, boolean enable) {
        List<E> elementsToSwap = ImmutableList.of(elements.get(swap.firstIndex()), elements.get(swap.secondIndex()));
        int i = Permutation.zeroInt + (enable ? 1 : 0);
        elements.set(swap.firstIndex(), select(elementsToSwap, i));
        elements.set(swap.secondIndex(), select(elementsToSwap, i ^ 1));
    }
}
