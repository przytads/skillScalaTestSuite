package date
import org.junit.runner.RunWith
import common.CommonTest
import date.api.SkillState
import org.scalatest.junit.JUnitRunner
import java.io.File

@RunWith(classOf[JUnitRunner])
class AppendTest extends CommonTest {
  import SkillState._

  test("Tr13 §6.2.3 based date example") {
    val path = tmpFile("writetest")

    val σ = create
    σ.Date(1)
    σ.Date(-1)
    σ.write(path)

    assert(sha256(path) === sha256(new File("src/test/resources/date-example.sf").toPath))
    assert(SkillState.read(path).Date.all.map(_.date).toList.sameElements(List(1, -1)))
  }

  test("Tr13 §6.2.3 append example") {
    val path = tmpFile("append.test")

    val σ = SkillState.read("src/test/resources/date-example.sf")
    σ.Date(2)
    σ.Date(3)
    σ.append(path)

    assert(sha256(path) === sha256(new File("src/test/resources/date-example-append.sf").toPath))
    assert(SkillState.read(path).Date.all.map(_.date).toList.sameElements(List(1, -1, 2, 3)))
  }

  test("write 100k dates; append 9x100k; write 1m dates and check them all -- multiple states") {
    val limit = 1e5.toInt
    val path = tmpFile("append")

    // write
    locally {
      val σ = create
      for (i ← 0 until limit)
        σ.Date(i)

      σ.write(path)
    }

    // append
    for (i ← 1 until 10) {
      val σ = SkillState.read(path)
      for (v ← i * limit until limit + i * limit)
        σ.Date(v)

      σ.append
    }

    // read & check & write
    val writePath = tmpFile("write")
    locally {
      val state = SkillState.read(path)
      val d = state.Date.all
      assert(state.Date.size === 10 * limit, s"we somehow lost ${10 * limit - state.Date.size} dates")

      var cond = true
      for (i ← 0 until 10 * limit)
        cond &&= (i == d.next.date)
      assert(cond, "match failed")

      state.write(writePath)
    }

    // check append against write
    locally {
      val s1 = SkillState.read(path)
      val s2 = SkillState.read(writePath)

      val i1 = s1.Date.all
      val i2 = s2.Date.all

      while (i1.hasNext) {
        assert(i1.next.date === i2.next.date)
      }

      assert(i1.hasNext === i2.hasNext, "state1 had less elements!")
    }
  }

  ignore("write 100k dates; append 9x100k; write 1m dates and check them all -- in two states") {
    val limit = 1e5.toInt
    val path = tmpFile("append")

    // write
    val σ = create
    for (i ← 0 until limit)
      σ.Date(i)

    σ.write(path)

    // append
    for (i ← 1 until 10) {
      for (v ← i * limit until limit + i * limit)
        σ.Date(v)

      σ.append
    }

    // read & check & write
    val writePath = tmpFile("write")
    locally {
      val state = read(path)
      val d = state.Date.all
      assert(state.Date.size === 10 * limit, s"we somehow lost ${10 * limit - state.Date.size} dates")

      var cond = true
      for (i ← 0 until 10 * limit)
        cond &&= (i == d.next.date)
      assert(cond, "match failed")

      state.write(writePath)
    }

    // check append against write
    locally {
      val s1 = read(path)
      val s2 = read(writePath)

      val i1 = s1.Date.all
      val i2 = s2.Date.all

      while (i1.hasNext) {
        assert(i1.next.date === i2.next.date)
      }

      assert(i1.hasNext === i2.hasNext, "state1 had less elements!")
    }
  }

  ignore("write 100k dates; append 9x100k; write 1m dates and check them all -- in a single state") {
    val limit = 1e5.toInt
    val path = tmpFile("append")

    // write
    val σ = create
    for (i ← 0 until limit)
      σ.Date(i)

    σ.write(path)

    // append
    for (i ← 1 until 10) {
      for (v ← i * limit until limit + i * limit)
        σ.Date(v)

      σ.append
    }

    // read & check & write
    val writePath = tmpFile("write")
    locally {
      val state = σ
      val d = state.Date.all
      assert(state.Date.size === 10 * limit, s"we somehow lost ${10 * limit - state.Date.size} dates")

      var cond = true
      for (i ← 0 until 10 * limit)
        cond &&= (i == d.next.date)
      assert(cond, "match failed")

      state.write(writePath)
    }

    // check append against write
    locally {
      val s1 = read(path)
      val s2 = read(writePath)

      val i1 = s1.Date.all
      val i2 = s2.Date.all

      while (i1.hasNext) {
        assert(i1.next.date === i2.next.date)
      }

      assert(i1.hasNext === i2.hasNext, "state1 had less elements!")
    }
  }
  
  test("write, append, check") {
    val path = tmpFile("date.write.append.check")

    locally {
      val σ = SkillState.create
      σ.Date(1)
      σ.Date(2)
      σ.Date(3)
      σ.write(path)
    }

    locally {
      val σ = SkillState.read(path)
      σ.Date(1)
      σ.Date(2)
      σ.Date(3)
      σ.append(path)
    }

    assert("123123" === SkillState.read(path).Date.all.map(_.date).mkString(""))
  }
}
