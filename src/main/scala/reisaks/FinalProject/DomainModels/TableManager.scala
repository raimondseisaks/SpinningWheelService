package reisaks.FinalProject.DomainModels

import akka.util.Timeout
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import cats.effect.unsafe.implicits.global
import reisaks.FinalProject.DomainModels.SystemMessages._
import reisaks.FinalProject.ServerSide.AkkaActors.TableActorRef._
import reisaks.FinalProject.ServerSide.AkkaActors.TableActorMessages._
import reisaks.FinalProject.ServerSide.AkkaActors.PlayerActorMessages._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.Success

object TableManager {
  import AllTables._
  implicit val timeout: Timeout = 5.seconds
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  def joinPublicTable(player: Player, tableName: String, playerManager: PLayerManagerTrait): Unit = {
    allTableRef.find(_.path.name == tableName) match {
      case Some(table) =>
        val isPlaying = playerManager.isPlayerPlaying(player).unsafeRunSync()

        if (isPlaying) {
          player.actorRef ! MessageToPlayer(AlreadyJoinedToTable.message)
        }
        else {
          table ! JoinTable(player, playerManager, table)
        }
      case None =>
        player.actorRef ! MessageToPlayer(TableNotExist.message)
    }
  }

  def showAvailableTables(player: Player): Unit =
    allTableRef.foreach {
      w =>
        val freePlace = (w ? ShowAvailablePlaces).mapTo[Int]
        freePlace.onComplete {
          case Success(value) =>
            if (value > 0) player.actorRef ! MessageToPlayer(s"${w.path.name} has $value seats")
        }
    }

  def createPrivateTable(tableId: String, password: String, player: Player, playerManager: PLayerManagerTrait): Unit = {
    if (privateTableRef.keys.exists(_.path.name == s"Private-$tableId")) {
      player.actorRef ! MessageToPlayer("incorrect-tableName")
    } else {
      val privateId = s"Private-$tableId"
      val newTableActor = system.actorOf(tableProps, privateId)
      println("we")
      privateTableRef = privateTableRef + (newTableActor -> password)
      println("d")
      player.actorRef ! MessageToPlayer("table-created-successfully")
      joinPrivateTable(player, tableId, password, playerManager)
    }
  }

  def joinPrivateTable (player: Player, tableName: String, password: String, playerManager: PLayerManagerTrait): Unit = {
    val tableEntry = privateTableRef.find { case (actorRef, _) => actorRef.path.name == s"Private-$tableName" }
    println(tableEntry)
    tableEntry match {
      case Some((tableActorRef, tablePassword)) =>
        if (tablePassword == password) {
          tableActorRef ! JoinTable(player, playerManager, tableActorRef)
          player.actorRef ! MessageToPlayer("private-table-joined")
        } else {
          player.actorRef ! MessageToPlayer("incorrect-table-password")
        }
      case None =>
        player.actorRef ! MessageToPlayer(s"private-table-not-exist")
    }
  }
}

object AllTables {
  val system: ActorSystem = ActorSystem("MyActorSystem")
  val tableOneActor: ActorRef = system.actorOf(tableProps, "Table-1")
  val tableTwoActor: ActorRef = system.actorOf(tableProps, "Table-2")
  val tableThreeActor: ActorRef = system.actorOf(tableProps, "Table-3")
  val tableFourActor: ActorRef = system.actorOf(tableProps, "Table-4")
  val tableFiveActor: ActorRef = system.actorOf(tableProps, "Table-5")
  val tableSixActor: ActorRef = system.actorOf(tableProps, "Table-6")
  val tableSevenActor: ActorRef = system.actorOf(tableProps, "Table-7")
  val tableEightActor: ActorRef = system.actorOf(tableProps, "Table-8")
  val tableNineActor: ActorRef = system.actorOf(tableProps, "Table-9")
  val allTableRef: Set[ActorRef] = Set(
                                  tableOneActor,
                                  tableTwoActor,
                                  tableThreeActor,
                                  tableFourActor,
                                  tableFiveActor,
                                  tableSixActor,
                                  tableSevenActor,
                                  tableEightActor,
                                  tableNineActor)

  var privateTableRef: Map[ActorRef, String] = Map.empty
}


