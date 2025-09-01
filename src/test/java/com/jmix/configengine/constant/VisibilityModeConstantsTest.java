package com.jmix.configengine.constant;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * VisibilityModeConstants测试类
 */
public class VisibilityModeConstantsTest {

    @Test
    public void testConstants() {
        assertEquals(0, VisibilityModeConstants.VISIBLE_EDITABLE);
        assertEquals(1, VisibilityModeConstants.VISIBLE_READONLY);
        assertEquals(3, VisibilityModeConstants.HIDDEN_READONLY);
    }

    @Test
    public void testGetDescription() {
        assertEquals("Visible, Editable", VisibilityModeConstants.getDescription(0));
        assertEquals("Visible, Read-only", VisibilityModeConstants.getDescription(1));
        assertEquals("Hidden, Read-only", VisibilityModeConstants.getDescription(3));
        assertEquals("Unknown", VisibilityModeConstants.getDescription(2));
    }

    @Test
    public void testIsVisible() {
        assertTrue(VisibilityModeConstants.isVisible(0));  // VISIBLE_EDITABLE
        assertTrue(VisibilityModeConstants.isVisible(1));  // VISIBLE_READONLY
        assertFalse(VisibilityModeConstants.isVisible(3)); // HIDDEN_READONLY
    }

    @Test
    public void testIsEditable() {
        assertTrue(VisibilityModeConstants.isEditable(0));  // VISIBLE_EDITABLE
        assertFalse(VisibilityModeConstants.isEditable(1)); // VISIBLE_READONLY
        assertFalse(VisibilityModeConstants.isEditable(3)); // HIDDEN_READONLY
    }
} 