package eu.telecomsudparis.jnvm;

import eu.telecomsudparis.jnvm.offheap.OffHeap;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.offheap.OffHeapString;
import eu.telecomsudparis.jnvm.util.persistent.RecoverableHashMap;
import eu.telecomsudparis.jnvm.util.persistent.RecoverableMap;
import org.junit.Test;

import static eu.telecomsudparis.jnvm.PetStoreBenchmark.OffHeapCat;


public class PetStoreTest {

    static {
        OffHeap.finishInit();
    }

    @Test
    public void simple() {
        RecoverableMap<OffHeapString, OffHeapObject> root = OffHeap.rootInstances;
        OffHeapString name1 = new OffHeapString("cat1");
        OffHeapCat cat1 = new OffHeapCat(1L, 1f, null, null);
        root.put(name1, cat1);
        OffHeapString name1b = new OffHeapString("cat1");
        OffHeapCat cat1b = (OffHeapCat) root.get(name1b);
        assert cat1.equals(cat1b);

        RecoverableHashMap<OffHeapString, OffHeapCat> kittens = new RecoverableHashMap<>();
        kittens.put(name1,cat1);
        OffHeapCat cat2 = new OffHeapCat(2L, 1f, null, kittens);
        OffHeapString name2 = new OffHeapString("cat2");
        kittens.put(name2, cat2);
        OffHeapCat cat1t = cat2.getKittens().get(name1);
        assert cat1t.equals(cat1);
    }

}
