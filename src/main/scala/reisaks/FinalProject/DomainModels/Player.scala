package reisaks.FinalProject.DomainModels

import akka.actor.TypedActor.dispatcher
import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import reisaks.FinalProject.DomainModels.SystemMessages.{ExistingID, GameError}
import reisaks.FinalProject.ServerSide.AkkaActors.PlayerActor
import reisaks.FinalProject.ServerSide.AkkaActors.TableActorMessages.{AddBetToTable, DeleteBet, LeaveTable, SendChatMessage}
import reisaks.FinalProject.DomainModels.SqlDatabase._
import reisaks.FinalProject.ServerSide.AkkaActors.PlayerActorMessages.MessageToPlayer
import scala.util.Success
import java.util.Properties
import javax.mail._
import javax.mail.internet._

case class Player(var playerId: String, actorRef: ActorRef)

trait PLayerManagerTrait {
  def createSession(id: String, password: String): IO[Either[GameError, Player]]
  def endPlayerSession(player: Player): IO[Unit]
  def isPlayerPlaying(player: Player): IO[Boolean]
  def addToTable(player: Player, actorRef: ActorRef): IO[Unit]
  def leftTable(player: Player): IO[Unit]
  def requestAddBet(player: Player, betCode: String, amount: String): IO[Unit]
  def deleteBet(player: Player, betCode: String): IO[Unit]
  def sendChatMessage(player: Player, message: String): IO[Unit]
  def deleteUser(player: Player, password: String): Unit
  def sendSupportMessage(player: Player, playerEmail: String, messageText: String): Unit
}

class PlayerManager(system: ActorSystem, ref: Ref[IO, Map[Player, Option[ActorRef]]]) extends PLayerManagerTrait {

  def createSession(id: String, password: String): IO[Either[GameError, Player]] = {
    ref.modify { existingPlayerIds =>
      if (existingPlayerIds.keys.exists(_.playerId == id) || !isPlayerIdExist(id, password)) {
        (existingPlayerIds, Left(ExistingID))
      } else {
        val newActor = system.actorOf(PlayerActor.props(id), s"playerActor-$id")
        val newPlayer = Player(id, newActor)
        val updatedIds = existingPlayerIds + (newPlayer -> None)

        (updatedIds, Right(newPlayer))
      }
    }
  }

  def endPlayerSession(player: Player): IO[Unit] = {
    isPlayerPlaying(player).flatMap { result =>
      if (result) {
        IO(player.actorRef ! MessageToPlayer("Please leave the table"))
      } else {
        for {
          _ <- IO(player.actorRef ! PoisonPill)
          _ <- ref.update(existingPlayerIds => existingPlayerIds - player)
        } yield ()
      }
      }
    }

  def isPlayerPlaying(player: Player): IO[Boolean] =
    ref.get.map { playerMap =>
      playerMap.get(player) match {
        case Some(None) => false
        case Some(_) => true
      }
    }

  def leftTable(player: Player): IO[Unit] = {
    ref.update { playersMap =>
      playersMap.get(player) match {
        case Some(Some(table)) =>
          table ! LeaveTable(player)
          playersMap + (player -> None)
      }
    }
  }

  def addToTable(player: Player, actorRef: ActorRef): IO[Unit] = {
    ref.update { playersMap =>
      playersMap.get(player) match {
        case Some(None) =>
          val updated = playersMap + (player -> Some(actorRef))
          updated
      }
    }
  }

  def requestAddBet(player: Player, betCode: String, amount: String): IO[Unit] =
    ref.get.flatMap(w => w.get(player) match {
      case Some(Some(table)) => IO {
        Bet.create(betCode, amount) match {
          case Right(bet) => table ! AddBetToTable(player, bet)
          case Left(error) => player.actorRef ! MessageToPlayer(error.message)
        }
      }
    })

  def deleteBet(player: Player, betCode: String): IO[Unit] = {
    ref.get.flatMap(w => w.get(player) match {
      case Some(Some(table)) => IO {
        table ! DeleteBet(player, betCode)
      }
    })
  }

  def sendChatMessage(player: Player, message: String): IO[Unit] =
    ref.get.flatMap(w => w.get(player) match {
      case Some(Some(table)) => IO {
        table ! SendChatMessage(message, player)
      }
      case _ => IO.unit
    })

  def deleteUser(player: Player, password: String): Unit = {
    deleteUserById(player, password)
    endPlayerSession(player).unsafeToFuture()
  }

  def sendSupportMessage(player: Player, playerEmail: String, messageText: String): Unit = {
      val supportEmail = "eisaks83@gmail.com"       // Support email
      val host = "smtp.gmail.com"                   // Gmail SMTP server
      val username = "eisaks83@gmail.com"
      val password = "xmqe opci phpi jrsm"

      val properties = new Properties()
      properties.put("mail.smtp.auth", "true")
      properties.put("mail.smtp.starttls.enable", "true")
      properties.put("mail.smtp.host", host)
      properties.put("mail.smtp.port", "587")

      // Authenticate and get the session
      val session = Session.getInstance(properties, new Authenticator() {
        override protected def getPasswordAuthentication: PasswordAuthentication = {
          new PasswordAuthentication(username, password)
        }
      })

      try {
        val message = new MimeMessage(session)
        message.setFrom(new InternetAddress(playerEmail))
        message.setRecipients(Message.RecipientType.TO, supportEmail)
        message.setSubject("Player Support Request")
        message.setText(s"Player Email: $playerEmail\n\n$messageText")

        Transport.send(message)
        player.actorRef ! MessageToPlayer("email-sent")
      } catch {
        case e: MessagingException =>
          e.printStackTrace()
          player.actorRef ! MessageToPlayer("email-not-sent")
      }
    }
}
