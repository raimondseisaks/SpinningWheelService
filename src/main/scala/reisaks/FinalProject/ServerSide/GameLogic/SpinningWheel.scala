package reisaks.FinalProject.ServerSide.GameLogic

import akka.actor.ActorRef
import cats.effect.IO
import reisaks.FinalProject.ServerSide.AkkaActors.TableActorMessages._
import scala.concurrent.duration._
import scala.util.Random

object SpinningWheel {

  //Generate random number from 1 to 100
  private def generateWinningNumber(): IO[Int] = IO {
    Random.nextInt(100) + 1
  }

  //Change state of spinning wheel actor and send winning number
  def stateChanger(tableRef: ActorRef): IO[Unit] = {
    def loop: IO[Unit] = for {
      _ <- IO(tableRef ! BetsStart)
      _ <- IO.sleep(10.seconds)
      _ <- IO(tableRef ! BetsEnd)
      _ <- IO.sleep(2.seconds)
      number <- generateWinningNumber()
      _ <- IO(tableRef ! GameStart(number))
      _ <- IO.sleep(5.seconds)
      _ <- IO(tableRef ! GameResult(number))
      _ <- IO.sleep(3.seconds)
      _ <- IO(tableRef ! RoundEnd)
    } yield ()
    loop
  }
}




