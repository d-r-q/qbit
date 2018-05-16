package qbit

import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import qbit.collections.BTree
import qbit.collections.assertDegreeInv
import qbit.collections.heights
import java.util.*

class BTreeSetTest {

    @Test
    fun testAdd() {
        val inc = BTree<Int>(naturalOrder())
                .add(1)
                .add(2)
                .add(3)
                .add(4)
        assertEquals(4, inc.size)

        val dec = BTree<Int>(naturalOrder())
                .add(4)
                .add(3)
                .add(2)
                .add(1)
        assertEquals(4, dec.size)

        val mid = BTree<Int>(naturalOrder())
                .add(1)
                .add(3)
                .add(2)
                .add(4)
        assertEquals(4, mid.size)

        for (i in 1..4) {
            assertTrue(inc.contains(i))
            assertTrue(dec.contains(i))
            assertTrue(mid.contains(i))
        }

        val overflow = BTree<Int>(naturalOrder())
                .add(1)
                .add(3)
                .add(2)
        assertEquals(3, overflow.size)
        assertTrue(overflow.contains(1))
        assertTrue(overflow.contains(2))
        assertTrue(overflow.contains(3))
    }

    @Test
    fun smokeTest() {
        testEls((0..100).toList())
        testEls((100 downTo 0).toList())
        testEls((0..100).shuffled())
    }

    private fun testEls(els: List<Int>) {
        var set = BTree<Int>(naturalOrder(), 4)
        var ref = TreeSet<Int>()
        for (i in 0 until els.size) {
            set = set.add(els[i])
            ref.add(els[i])
            assertEquals(ref.size, set.size)
            for (j in ref) {
                assertTrue(set.contains(j))
            }
            assertDegreeInv(set, true)
        }
        val heights = heights(set)
        assertEquals(1, heights.toSet().size)
        val expected = els.sorted().asSequence()
        assertTrue(set.iterator().asSequence().zip(expected).all { it.first == it.second })
        val iter = set.select({ e: Int -> if (e < 10) -1 else 0 })
        val subels = iter.asSequence().take(80).toList()
        assertArrayEquals((10 until 90).toList().toIntArray(), subels.toIntArray())
        assertFalse(set.select { it - 101 }.hasNext())

        var dSet = set
        ref = TreeSet(dSet)
        for (i in 0 until els.size) {
            dSet = dSet.remove(i)
            ref.remove(i)
            validateSet(dSet, ref)
        }

        var rSet = set
        ref = TreeSet(rSet)
        for (i in els.size downTo 0) {
            rSet = rSet.remove(i)
            ref.remove(i)
            validateSet(rSet, ref)
        }

        for (seed in 0..10) {
            var rndSet = set
            ref = TreeSet(rndSet)
            val random = Random(seed.toLong())
            for (i in els.shuffled(random)) {
                rndSet = rndSet.remove(i)
                ref.remove(i)
                validateSet(rndSet, ref)
            }

            set = BTree(naturalOrder())
            ref = TreeSet(naturalOrder<Int>())
            for (i in 0..1000) {
                val v = random.nextInt(100 * (seed + 1))
                if (random.nextInt(100) < 75) {
                    set = set.add(v)
                    ref.add(v)
                } else {
                    set = set.remove(v)
                    ref.remove(v)
                }
                validateSet(set, ref)
            }
        }
    }

    @Ignore
    @Test
    fun longSmokeTest() {
        var set: BTree<Int>
        var ref: TreeSet<Int>
        for (seed in 0..50) {
            val random = Random(seed.toLong())
            set = BTree(naturalOrder())
            ref = TreeSet()
            for (i in 0..10000) {
                val v = random.nextInt(100 * (seed + 1))
                if (random.nextInt(100) < 75) {
                    set = set.add(v)
                    ref.add(v)
                } else {
                    set = set.remove(v)
                    ref.remove(v)
                }
                validateSet(set, ref)
            }
        }
    }

    private fun validateSet(bSet: BTree<Int>, ref: TreeSet<Int>) {
        assertEquals(ref.size, bSet.size)
        for (j in ref) {
            assertTrue("BTree does not contain $j", bSet.contains(j))
        }
        assertDegreeInv(bSet, true)
    }

}