import Example

package com.evolveum.midpoint.testing.systest

/**
 * Tests for the {@link Example} class.
 */
class ExampleTest
    extends GroovyTestCase
{
    void testShow() {
        new Example().show()
    }
}
