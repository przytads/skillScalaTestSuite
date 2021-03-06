package annotation

import org.junit.Assert

import annotation.api.SkillState
import annotation.internal.ParseException
import annotation.internal.PoolSizeMissmatchError
import annotation.internal.TypeMissmatchError
import common.CommonTest

/**
 * Tests the file reading capabilities.
 */
class ParseTest extends CommonTest {
  @inline def read(s: String) = SkillState.read("src/test/resources/"+s)

  test("two dates") {
    val σ = read("date-example.sf")

    val it = σ.Date.all;
    Assert.assertEquals(1L, it.next.date)
    Assert.assertEquals(-1L, it.next.date)
    assert(!it.hasNext, "there shouldn't be any more elemnets!")
  }

  test("simple nodes") { Assert.assertNotNull(read("node.sf")) }
  test("simple test") { Assert.assertNotNull(read("date-example.sf")) }
  /**
   * @see § 6.2.3.Fig.3
   */
  test("two node blocks") { Assert.assertNotNull(read("twoNodeBlocks.sf")) }
  /**
   * @see § 6.2.3.Fig.4
   */
  test("colored nodes") { Assert.assertNotNull(read("coloredNodes.sf")) }
  test("four colored nodes") { Assert.assertNotNull(read("fourColoredNodes.sf")) }
  test("empty blocks") { Assert.assertNotNull(read("emptyBlocks.sf")) }
  test("two types") { Assert.assertNotNull(read("twoTypes.sf")) }
  test("trivial type definition") { Assert.assertNotNull(read("trivialType.sf")) }

  /**
   * a regression test checking correct treatment of types without fields
   */
  test("append without fields")(read("noFieldRegressionTest.sf").Date.all)

  /**
   * a regression test checking correct treatment of types without fields
   */
  test("appended dates without fields")(assert(read("noFieldRegressionDates.sf").Date.all.size === 2))

  /**
   * null pointers are legal in regular fields if restricted to be nullable
   *  (although the behavior is not visible here due to lazyness)
   */
  test("nullable restricted null pointer") { read("nullableNode.sf").Test.all }
  /**
   * null pointers are legal in annotations
   */
  test("null pointer in an annotation") { read("nullAnnotation.sf").Test.all }

  /**
   * null pointers are not legal in regular fields
   *
   * @note this is the lazy case, i.e. the node pointer is never evaluated
   */
  test("null pointer in a nonnull field; lazy case!") {
    read("illformed/nullNode.sf").Test.all
  }

  test("data chunk is too long") {
    val thrown = intercept[PoolSizeMissmatchError] {
      read("illformed/longerDataChunk.sf").Date.all
    }
    assert(thrown.getMessage === "expected: 3, was: 2, field type: v64")
  }
  test("data chunk is too short") {
    val thrown = intercept[PoolSizeMissmatchError] {
      read("illformed/shorterDataChunk.sf").Date.all
    }
    assert(thrown.getMessage === "expected: 1, was: 2, field type: v64")
  }
  test("incompatible field types") {
    val thrown = intercept[TypeMissmatchError] {
      read("illformed/incompatibleType.sf").Date.all
    }
    assert(thrown.getMessage === """During construction of date.date: Encountered incompatible type "date" (expected: v64)""")
  }
  test("reserved type ID") {
    val thrown = intercept[ParseException] {
      read("illformed/illegalTypeID.sf").Date.all
    }
    assert(thrown.getMessage === """In block 1 @0x11: Invalid type ID: 16""")
  }
  test("missing user type") {
    val thrown = intercept[ParseException] {
      read("illformed/missingUserType.sf").Date.all
    }
    assert(thrown.getMessage === "In block 1 @0x13: inexistent user type 1 (user types: 0 -> date)")
  }
  test("illegal string pool offset") {
    val thrown = intercept[ParseException] {
      read("illformed/illegalStringPoolOffsets.sf").Date.all
    }
    assert(thrown.getMessage === "In block 1 @0x5: corrupted string block")
  }
  test("missing field declarations in second block") {
    val thrown = intercept[ParseException] {
      read("illformed/missingFieldInSecondBlock.sf").Date.all
    }
    assert(thrown.getMessage === "In block 2 @0x16: Type a has 0 fields (requires 1)")
  }

  test("duplicate type definition in the first block") {
    val thrown = intercept[ParseException] {
      read("illformed/duplicateDefinition.sf").Date.all
    }
    assert(thrown.getMessage === "In block 1 @0xd: Duplicate definition of type a")
  }
  test("append in the first block") {
    val thrown = intercept[ParseException] {
      read("illformed/duplicateDefinitionMixed.sf").Date.all
    }
    assert(thrown.getMessage === "In block 1 @0xd: Duplicate definition of type a")
  }
  test("duplicate append in the same block") {
    val thrown = intercept[ParseException] {
      read("illformed/duplicateDefinitionSecondBlock.sf").Date.all
    }
    assert(thrown.getMessage === "In block 2 @0x12: Duplicate definition of type a")
  }

  // annotation related tests
  test("read annotation") { read("annotationTest.sf") }

  test("check annotation") {
    val state = read("annotationTest.sf")
    val t = state.Test.all.next
    val d = state.Date.all.next
    assert(t.f === d)
  }

  test("change annotation field") {
    val σ = read("annotationTest.sf")
    val t = σ.Test.all.next
    val d = σ.Date.all.next
    t.f = t
    assert(t.f === t)
  }

  test("annotation type-safety") {
    val σ = read("annotationTest.sf")
    val t = σ.Test.all.next
    val d = σ.Date.all.next
    // no its not
    if (t.f == t)
      fail;
  }
}