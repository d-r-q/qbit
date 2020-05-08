package qbit.collections

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals

class AnObject(var state: Int)

data class AnDataObject(var state: Int)

class IdentityMapTest {

    @JsName("Simple_objects_with_same_state_should_map_to_different_keys")
    @Test
    fun `Simple objects with same state should map to different keys`() {
        val map = IdentityMap<AnObject, Int>()
        val state = 1
        val anObj1 = AnObject(state)
        val anObj2 = AnObject(state)
        map[anObj1] = 1
        map[anObj2] = 2

        assertEquals(2, map.size, "Objects should act as different keys")
        assertEquals(1, map[anObj1])
        assertEquals(2, map[anObj2])
    }

    @JsName("Data_objects_with_same_state_should_map_to_different_keys")
    @Test
    fun `Data objects with same state should map to different keys`() {
        val map = IdentityMap<AnDataObject, Int>()
        val state = 1
        val anObj1 = AnDataObject(state)
        val anObj2 = AnDataObject(state)
        map[anObj1] = 1
        map[anObj2] = 2

        assertEquals(2, map.size, "Objects should act as different keys")
        assertEquals(1, map[anObj1])
        assertEquals(2, map[anObj2])
    }

    @JsName("The_same_simple_object_with_changed_state_should_map_to_the_same_value")
    @Test
    fun `The same simple object with changed state should map to the same value`() {
        val map = IdentityMap<AnObject, Int>()
        val state = 1
        val anObj1 = AnObject(state)
        map[anObj1] = 1
        anObj1.state = 2

        assertEquals(1, map.size, "Object should be added to map")
        assertEquals(1, map[anObj1])
    }

    @JsName("The_same_data_object_with_changed_state_should_map_to_the_same_value")
    @Test
    fun `The same data object with changed state should map to the same value`() {
        val map = IdentityMap<AnDataObject, Int>()
        val state = 1
        val anObj1 = AnDataObject(state)
        map[anObj1] = 1
        anObj1.state = 2

        assertEquals(1, map.size, "Object should be added to map")
        assertEquals(1, map[anObj1])
    }

}