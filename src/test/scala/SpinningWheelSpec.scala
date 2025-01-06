package reisaks.FinalProject.ServerSide.GameLogic

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import reisaks.FinalProject.ServerSide.AkkaActors.TableActorMessages._

import scala.concurrent.duration._

class SpinningWheelSpec
  extends TestKit(ActorSystem("SpinningWheelSpec"))
    with AnyFlatSpecLike
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "SpinningWheel.stateChanger" should "send the correct sequence of messages to the tableRef" in {
    val tableProbe = TestProbe()
    val tableRef = tableProbe.ref

    // Run the stateChanger method
    SpinningWheel.stateChanger(tableRef).unsafeRunAsync(_ => ())

    // Verify the sequence of messages
    tableProbe.expectMsg(1.second, BetsStart)
    tableProbe.expectNoMessage(10.seconds)

    tableProbe.expectMsg(1.second, BetsEnd)
    tableProbe.expectNoMessage(2.seconds)

    tableProbe.expectMsgPF(1.second) {
      case GameStart(number) =>
        assert(number >= 1 && number <= 100)
    }

    tableProbe.expectNoMessage(5.seconds)

    tableProbe.expectMsgPF(1.second) {
      case GameResult(number) =>
        assert(number >= 1 && number <= 100)
    }

    tableProbe.expectNoMessage(3.seconds)

    tableProbe.expectMsg(1.second, RoundEnd)
  }
}

