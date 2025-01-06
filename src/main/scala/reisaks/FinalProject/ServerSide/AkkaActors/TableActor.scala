package reisaks.FinalProject.ServerSide.AkkaActors

import akka.actor.TypedActor.dispatcher
import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.pattern.ask
import cats.effect.unsafe.implicits.global
import io.circe.syntax.EncoderOps
import org.apache.kafka.clients.producer.KafkaProducer
import reisaks.FinalProject.DomainModels.{Bet, Player, TableOfBets}
import reisaks.FinalProject.DomainModels._
import reisaks.FinalProject.ServerSide.AkkaActors.PlayerActorMessages._
import reisaks.FinalProject.ServerSide.GameLogic.BetEvaluationService._
import reisaks.FinalProject.DomainModels.PLayerManagerTrait
import reisaks.FinalProject.ServerSide.GameLogic.SpinningWheel
import reisaks.FinalProject.DomainModels.SystemMessages._
import reisaks.FinalProject.ServerSide.Kafka.EventProducer
import reisaks.FinalProject.ServerSide.Json._
import reisaks.FinalProject.DomainModels.SqlDatabase._
import reisaks.FinalProject.DomainModels.AllTables.privateTableRef
import reisaks.FinalProject.DomainModels.TableManager.timeout

import scala.math.Numeric.BigDecimalAsIfIntegral.abs

sealed trait GameState
case object BetsStartState extends GameState
case object BetsEndState extends GameState
case object GameStartState extends GameState
case object GameResultsState extends GameState
case object RoundEndState extends GameState

sealed trait BetResult
case class Won(amount: BigDecimal) extends BetResult
case class Lose(amount: BigDecimal) extends BetResult

class TableActor extends Actor {
  import TableActorMessages._

  val topicName = "Spinning-Wheel-Game-Round"
  val producer: KafkaProducer[String, String] = EventProducer.initProducer
  var roundState: GameState = BetsStartState
  var tableOfBets: TableOfBets = TableOfBets.create
  var playingPlayers: Set[Player] = Set()
  var roundId = new Id

  def receive: Receive = {
    case JoinTable(player, playerManager, actorRef) =>
      if (playingPlayers.contains(player)) {
        player.actorRef ! MessageToPlayer(AlreadyJoinedToTable.message)
      }
      else if (playingPlayers.size >= 100) {
        player.actorRef ! MessageToPlayer(TooMuchPlayers.message)
      }
      else {
        if (playingPlayers.isEmpty) {
          SpinningWheel.stateChanger(self).unsafeToFuture()
        }
        playingPlayers += player
        playerManager.addToTable(player, actorRef).unsafeToFuture()
        player.actorRef ! MessageToPlayer(SuccessfullyJoinedToTable.message)
        broadcastAvailability()
      }

    case BetsStart =>
      roundId = new Id
      playingPlayers.foreach {
        player => player.actorRef ! MessageToPlayer(RoundStarted.message)
      }
      roundState = BetsStartState
      EventProducer.producerSend(producer, topicName, s"${self.path.name}", GameStarted(s"${self.path.name}", roundId.getStringId, s"$roundState").asJson.spaces4)

    case AddBetToTable(player, bet) =>
      if (playingPlayers.contains(player)) {
        if (roundState == BetsStartState)
            tableOfBets.addPlayerBet(player, bet) match {
              case Right(value) =>
                tableOfBets = value
                player.actorRef ! MessageToPlayer(s"You bet on ${bet.betCode} with amount ${bet.amount}")
              case Left(error) => player.actorRef ! MessageToPlayer(error.message)
            }
        else player.actorRef ! MessageToPlayer(BetRoundEnd.message)
        }
      else {
        player.actorRef ! MessageToPlayer("Please join the table!")
      }

    case DeleteBet(player, betCode) =>
      if(playingPlayers.contains(player)) {
        if (roundState == BetsStartState) {
          tableOfBets.deletePlayerBet(player, betCode.toInt) match {
            case Right(value) =>
              tableOfBets = value
              player.actorRef ! MessageToPlayer(s"deleted-bet $betCode")
            case Left(error) => player.actorRef ! MessageToPlayer(error.message)
          }
        }
      }


    case BetsEnd =>
      addGameRoundDb(self.path.name, playingPlayers.size, roundId.getStringId)
      playingPlayers.foreach {
        player => player.actorRef ! MessageToPlayer(BetHasEnded.message)
          tableOfBets.playerBets.map(player => player._2.map(bet => addBetDb(player._1, bet, self.path.name))
          )
      }
      roundState = BetsEndState

    case GameStart(number) =>
      playingPlayers.foreach {
        player =>
          player.actorRef ! MessageToPlayer(GameIsStarted.message)
          player.actorRef ! MessageToPlayer(number.toString)
      }
      roundState = GameStartState

    case GameResult(winningNumber) =>
      playingPlayers.foreach { w =>
        val sum = evaluateSum(w, tableOfBets, winningNumber)
        sum match {
          case Some(value) =>
            if (value > 0) {
              addGameRoundResult(w, self.path.name, value.toFloat)
              w.actorRef ! MessageToPlayer(s"Winning number $winningNumber! You won $value euro")
            }
            else {
              addGameRoundResult(w, self.path.name, value.toFloat)
              w.actorRef ! MessageToPlayer(s"Winning number $winningNumber! You lose ${abs(value)} euro")
            }
          case None => w.actorRef ! MessageToPlayer(s"Winning number $winningNumber!")
        }
      }
      val playerResults: Map[String, BetResult] = tableOfBets.playerBets.map { w =>
        val result = evaluateSum(w._1, tableOfBets, winningNumber)
        result match {
          case Some(value) =>
            if (value >= 0) w._1.playerId -> Won(value)
          else w._1.playerId -> Lose(value)
        }
      }
      roundState = GameResultsState
      EventProducer.producerSend( producer,
                                  topicName,
                                  s"${self.path.name}",
                                  GameEnded(s"${self.path.name}",
                                    roundId.getStringId ,
                                  s"$roundState",
                                  playerResults)
                                  .asJson.spaces4)
      tableOfBets = tableOfBets.cleanTable()

    case SendChatMessage(message, player) =>
      addMessageDb(player, self.path.name, message)
      playingPlayers.foreach { w  =>
        w.actorRef ! MessageToPlayer(s"chatMessage $message")
      }

    case RoundEnd =>
      playingPlayers.foreach {
        player => player.actorRef ! MessageToPlayer("Round has ended")
      }
      roundState = RoundEndState
      if (playingPlayers.nonEmpty) {
        SpinningWheel.stateChanger(self).unsafeToFuture()
      }
      else {
        if (self.path.name.startsWith("Private")) {
          self ! PoisonPill
          privateTableRef = privateTableRef.filterNot {
            case (actorRef, _) => actorRef.path.name == self.path.name
          }
        }
      }

    case LeaveTable(player) =>
      if (playingPlayers.contains(player)) {
        playingPlayers -= player
        player.actorRef ! MessageToPlayer("You left the table")
        broadcastAvailability()
        }
      else {
        player.actorRef ! MessageToPlayer("You can't leave the table without logging in")
      }

    case ShowAvailablePlaces =>
      sender() ! (100 - playingPlayers.size)
  }

  private def broadcastAvailability(): Unit = {
    val availablePlaces = playingPlayers.size
    playingPlayers.foreach { player =>
      player.actorRef ! MessageToPlayer(s"availability $availablePlaces")
    }
  }

}

object TableActorMessages {
  case object BetsStart
  case object BetsEnd
  case class GameStart(number: Int)
  case class GameResult(winningNumber: Int)
  case object RoundEnd
  case class AddBetToTable(player: Player, bet: Bet)
  case class JoinTable(player: Player, playerManager: PLayerManagerTrait, actorRef: ActorRef)
  case class LeaveTable(player: Player)
  case object ShowAvailablePlaces
  case class SendChatMessage(message: String, player: Player)
  case class DeleteBet(player: Player, betCode: String)

}

object TableActorRef {
  def tableProps: Props = Props(new TableActor)
}



