package eu.telecomsudparis.jnvm;

import java.util.Map;
import eu.telecomsudparis.jnvm.offheap.OffHeap;
import eu.telecomsudparis.jnvm.offheap.OffHeapObject;
import eu.telecomsudparis.jnvm.offheap.OffHeapObjectHandle;
import eu.telecomsudparis.jnvm.offheap.OffHeapString;
import eu.telecomsudparis.jnvm.util.persistent.RecoverableHashMap;
import eu.telecomsudparis.jnvm.util.persistent.RecoverableMap;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Set;

/**
 * Loosely based on https://docs.jboss.org/hibernate/core/3.2/reference/en/html/persistent-classes.html
 *
 */
public class PetStoreBenchmark {

    static {
        OffHeap.finishInit();
    }

    @Benchmark
    public void store() {

        RecoverableMap<OffHeapString, OffHeapObject> root = OffHeap.rootInstances;

        OffHeap.startRecording();
        OffHeapString name1 = new OffHeapString("cat1"), name2 = new OffHeapString("cat2");
        if (!root.containsKey(name1)) {
            RecoverableHashMap<OffHeapString, OffHeapCat> kittens = new RecoverableHashMap<>();
            OffHeapCat cat1 = new OffHeapCat(1L, 1f, null, kittens);
            OffHeapCat cat2 = new OffHeapCat(2L, 1f, cat1, null);
            kittens.put(name2,cat2);
            root.put(name1, cat1);
            OffHeapString name1a = new OffHeapString("cat1");
            kittens = ((OffHeapCat)root.get(name1a)).getKittens();
            OffHeapCat cat3 = kittens.get(name2);
            assert cat3.equals(cat2);
            name1a.destroy();
        } else {
            OffHeapCat cat = ((OffHeapCat)root.get(name1));
            RecoverableHashMap<OffHeapString, OffHeapCat> kittens = cat.getKittens();
            assert !kittens.isEmpty();
            assert kittens.get(name2).equals(name2);
            name1.destroy();
            name2.destroy();
        }
        OffHeap.stopRecording();

    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PetStoreBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(opt).run();
    }


    // business class
    // original
    public static class Cat {
        private Long id;
        private double weight;
        private Cat mother;
        private Map<String, Cat> kittens;

        public Cat(Long id, double weight, Cat mother, Map<String, Cat> kittens) {
            this.id = id;
            this.weight = weight;
            this.mother = mother;
            this.kittens = kittens;
        }

        public void setId(Long id) {
            this.id=id;
        }

        public Long getId() {
            return id;
        }

        public void setWeight(float weight) {
            this.weight = weight;
        }

        public double getWeight() {
            return weight;
        }

        public void setMother(Cat mother) {
            this.mother = mother;
        }

        public Cat getMother() {
            return mother;
        }

        public void setKittens(Map<String, Cat> kittens) {
            this.kittens = kittens;
        }

        public Map<String, Cat> getKittens() {
            return kittens;
        }

        public boolean equals(Object other) {
            if (this == other) return true;
            if ( !(other instanceof Cat) ) return false;
            return ((Cat) other).id == this.id;
        }

        public int hashCode() {
            int result;
            result = getMother().hashCode();
            result = (int) (29 * result + this.id);
            return result;
        }

    }

    // jnvm
    public static class OffHeapCat extends OffHeapObjectHandle {

        private static final long CLASS_ID = OffHeap.Klass.registerUserKlass(OffHeapCat.class);

        /* PMEM Layout :
         *  | Index | Offset | Bytes | Name    |
         *  |-------+--------+-------+---------|
         *  | 0     | 0      | 8     | id      |
         *  | 1     | 8      | 4     | weight  |
         *  | 2     | 12     | 8     | mother  |
         *  | 3     | 20     | 8     | kittens |
         *  end: 8 bytes
         */
        private static final long[] offsets = { 0L, 8L, 12L, 20L};
        private static final long SIZE = Integer.SIZE + Float.SIZE + 2*Long.SIZE;

        public OffHeapCat(Long id, float weight, OffHeapCat mother, RecoverableHashMap<OffHeapString, OffHeapCat> kittens) {
            super();
            this.setId(id);
            this.setWeight(weight);
            if (mother!= null) this.setMother(mother);
            if (kittens != null) this.setKittens(kittens);
        }

        // resurrector
        public OffHeapCat(Void __unused, long offset){
            super(offset);
        }

        public void setId(Long id) {
            this.setLongField(offsets[0],id);
        }

        public Long getId() {
            return this.getLongField(offsets[0]);
        }

        public void setWeight(float weight) {
            this.setDoubleField(offsets[1],weight);
        }

        public double getWeight() {
            return this.getDoubleField(offsets[1]);
        }

        public void setMother(OffHeapCat mother) {
            this.setHandleField(offsets[2],mother);
        }

        public OffHeapCat getMother() {
            return (OffHeapCat) this.getHandleField(offsets[2]);
        }

        public void setKittens(RecoverableHashMap<OffHeapString, OffHeapCat> kittens) {
            if (kittens != null) this.setHandleField(offsets[3], kittens);
        }

        public RecoverableHashMap<OffHeapString, OffHeapCat> getKittens() {
            return (RecoverableHashMap<OffHeapString, OffHeapCat>) this.getHandleField(offsets[3]);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if ( !(other instanceof OffHeapCat) ) return false;
            return ((OffHeapCat) other).getId() == this.getId();
        }

        @Override
        public long size() {
            return SIZE;
        }

        @Override
        public long classId() {
            return CLASS_ID;
        }

        @Override
        public void descend() {

        }

        @Override
        public void destroy() {
            super.destroy();
        }

        public int hashCode() {
            int result;
            result = getMother().hashCode();
            result = (int) (29 * result + this.getId());
            return result;
        }

    }

}
