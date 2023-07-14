package com.jtprince.coordinateoffset.provider;

import com.jtprince.coordinateoffset.provider.util.WorldAlignmentContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestWorldAlignmentContainer {
    @Test
    void testFromConfigThrowsBadlyFormattedString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> WorldAlignmentContainer.fromConfig(List.of("no_colons")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> WorldAlignmentContainer.fromConfig(List.of("one:too:many:colons")));
    }

    @Test
    void testFromConfigThrowsBadScaleFactor() {
        Assertions.assertThrows(NumberFormatException.class, () -> WorldAlignmentContainer.fromConfig(List.of("abc:def:notnumber")));
    }

    @Test
    void testFromConfigThrowsNotPowerOfTwoScaling() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> WorldAlignmentContainer.fromConfig(List.of("abc:def:0")));
        Assertions.assertDoesNotThrow(() -> WorldAlignmentContainer.fromConfig(List.of("abc:def:1")));
        Assertions.assertThrows(IllegalArgumentException.class, () -> WorldAlignmentContainer.fromConfig(List.of("abc:def:24")));
        Assertions.assertDoesNotThrow(() -> WorldAlignmentContainer.fromConfig(List.of("abc:def:64")));
    }

    @Test
    void testFindAlignments() {
        WorldAlignmentContainer container = WorldAlignmentContainer.fromConfig(List.of("world:world_nether:8"));

        WorldAlignmentContainer.QueryResult result1 = container.findAlignment("world");
        Assertions.assertNotNull(result1);
        Assertions.assertEquals("world_nether", result1.targetWorldName());
        Assertions.assertEquals(-3, result1.rightShiftAmount());

        WorldAlignmentContainer.QueryResult result2 = container.findAlignment("world_nether");
        Assertions.assertNotNull(result2);
        Assertions.assertEquals("world", result2.targetWorldName());
        Assertions.assertEquals(3, result2.rightShiftAmount());
    }

    @Test
    void testFindAlignmentsNoMatch() {
        WorldAlignmentContainer container = WorldAlignmentContainer.fromConfig(List.of("w0:w1:8", "w2:w3:16"));

        WorldAlignmentContainer.QueryResult result = container.findAlignment("world");
        Assertions.assertNull(result);
    }

    @Test
    void testFindAlignmentsFirstMatchFirst() {
        WorldAlignmentContainer container = WorldAlignmentContainer.fromConfig(List.of("w0:w1:8", "w2:w0:16"));

        WorldAlignmentContainer.QueryResult result = container.findAlignment("w0");
        Assertions.assertNotNull(result);
        Assertions.assertEquals("w1", result.targetWorldName());
        Assertions.assertEquals(-3, result.rightShiftAmount());
    }

    @Test
    void testGreatestRightShift() {
        WorldAlignmentContainer container = WorldAlignmentContainer.fromConfig(List.of("w0:w1:8", "w2:w0:16"));

        Assertions.assertEquals(3, container.greatestPossibleRightShiftForWorld("w0"));
        Assertions.assertEquals(0, container.greatestPossibleRightShiftForWorld("w1"));
        Assertions.assertEquals(4, container.greatestPossibleRightShiftForWorld("w2"));
    }
}
