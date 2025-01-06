package reisaks.FinalProject.ServerSide.WebSocket

import akka.NotUsed
import akka.http.scaladsl.model.StatusCodes
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import cats.effect.{IO, IOApp, Ref}
import cats.effect.unsafe.implicits.global
import reisaks.FinalProject.DomainModels._

import scala.io.StdIn
import akka.actor.ActorRef
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Sink, Source}
import reisaks.FinalProject.ServerSide.AkkaActors.PlayerActorMessages._
import reisaks.FinalProject.DomainModels.TableManager._
import reisaks.FinalProject.ServerSide.Kafka.{EmbeddedKafka, EventConsumer}
import reisaks.FinalProject.DomainModels.SqlDatabase._

import scala.concurrent.duration.DurationInt
import scala.util.Success

object WebSocketServer extends IOApp.Simple {

  def run: IO[Unit] = {
    for {
      system <- IO(ActorSystem("MyActorSystem"))
      playerIdsRef <- Ref.of[IO, Map[Player, Option[ActorRef]]](Map.empty)
      playerManager = new PlayerManager(system, playerIdsRef)
      serverFiber <- startServer(playerManager, system).start
      sqlFiber <- IO(initialize()).attempt.start
      kafkaFiber <- EmbeddedKafka.run().start
      _ <- serverFiber.join
      _ <- sqlFiber.join
      _ <- kafkaFiber.join
    } yield ()
  }

  private def startServer(playerManager: PLayerManagerTrait, system: ActorSystem): IO[Unit] = {

    IO {
      implicit val sys: ActorSystem = system
      implicit val materializer: ActorMaterializer = ActorMaterializer()

      val route = {
        pathPrefix("register" / Segment / Segment / Segment) { (playerId, email, password) =>
          if (!isPlayerIdExist(playerId, password)) {
            createUserDB(playerId, email, password)
            addInitBalanceDb(playerId)
            onSuccess(playerManager.createSession(playerId, password).unsafeToFuture()) {
              case Right(player) =>
                handleWebSocketMessages(webSocketFlow(player, playerManager))
              case Left(error) =>
                complete(StatusCodes.Forbidden, s"Registration failed: $error")
            }
          }
          else {
            complete(StatusCodes.Forbidden, s"User already exists")
          }
        } ~
          pathPrefix("login" / Segment / Segment) { (playerId, password) =>
            onSuccess(playerManager.createSession(playerId, password).unsafeToFuture()) {
              case Right(player) =>
                handleWebSocketMessages(webSocketFlow(player, playerManager))
              case Left(error) =>
                complete(StatusCodes.Forbidden, s"Login failed: $error")
            }
          }
      }

      val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

      println(s"Server is now online at http://localhost:8080\nPress RETURN to stop...")
      StdIn.readLine()
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(
          _ => system.terminate()
        )
    }
  }

  private def webSocketFlow(player: Player, playerManager: PLayerManagerTrait)
                           (implicit system: ActorSystem): Flow[Message, Message, Any] = {

    val sourceWithActorRef: Source[Message, ActorRef] =
      Source.actorRef[Message](bufferSize = 100, OverflowStrategy.fail)

    val (playerActorRef, source): (ActorRef, Source[Message, NotUsed]) = sourceWithActorRef.preMaterialize()

    player.actorRef ! RegisterWebSocket(playerActorRef)
    val userData = getPlayerData(player.playerId)
    player.actorRef ! MessageToPlayer(s"user-data $userData")

    val incoming: Sink[Message, Any] =
      Flow[Message]
      .collect {
        case TextMessage.Strict(text) => text
      }
      .map { msg =>
        msg.split("\\s+") match {
          case Array("Join-Table", tableName) => joinPublicTable(player, tableName, playerManager)

          case Array("Exit-Table") => playerManager.leftTable(player).unsafeToFuture()

          case Array("Add-Bet", betCode, amount) => playerManager.requestAddBet(player, betCode, amount).unsafeToFuture()

          case Array("chat-message", message @ _*) =>
            val fullMessage = s"${player.playerId}: ${message.mkString(" ")}"
            playerManager.sendChatMessage(player, fullMessage).unsafeToFuture()

          case Array("delete-bet", betCode) => playerManager.deleteBet(player, betCode).unsafeToFuture()

          case Array("update-password", currentPassword, newPassword) => changePasswordDb(player, currentPassword, newPassword)

          case Array("Show-Available-Tables") =>
            showAvailableTables(player)

          case Array("Join-Private-Table", tableId, tablePassword) => joinPrivateTable(player, tableId, tablePassword, playerManager)

          case Array("Create-Private-Table", tableId, tablePassword) =>
            createPrivateTable(tableId, tablePassword, player, playerManager)

          case Array("update-email", email) => changeEmailDb(player, email)

          case Array("update-player-name", name) =>
            changePlayerNameDb(player, name)

          case Array("Exit-Server") => playerManager.endPlayerSession(player).unsafeToFuture()

          case Array("update-balance-amount", amount) => updateBalanceDb(player, amount.toFloat)

          case Array("Get-Current-Server-Stats") => player.actorRef ! MessageToPlayer(getCurrentServerStats())

          case Array("Get-Current-Player-Stats") => player.actorRef ! MessageToPlayer(getCurrentPlayerStats(player.playerId))

          case Array("Delete-User", password) => playerManager.deleteUser(player, password)

          case Array("support", email, message @ _*) =>
              val question = message.mkString(" ")
              playerManager.sendSupportMessage(player,email, question)

          case _ => println(s"Unsupported message sent $msg")
        }
      }.to(Sink.ignore)
    Flow.fromSinkAndSource(incoming, source)

    val keepAliveFlow: Flow[Message, Message, NotUsed] = Flow[Message]
      .merge(Source.tick(30.seconds, 30.seconds, TextMessage("ping")))

    Flow.fromSinkAndSource(incoming, source.via(keepAliveFlow))
  }
}


