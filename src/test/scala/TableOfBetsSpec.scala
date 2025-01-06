package reisaks.FinalProject.DomainModels

import akka.actor.ActorSystem
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import reisaks.FinalProject.DomainModels.SystemMessages._
import reisaks.FinalProject.ServerSide.AkkaActors.PlayerActor

class TableOfBetsSpec extends AnyFlatSpec with Matchers {
  implicit val system: ActorSystem = ActorSystem("test-system")

  // Helper function to create a sample player
  def createPlayer(id: String, name: String): Player = {
    val newActor = system.actorOf(PlayerActor.props(id), s"playerActor-$id")
    Player(name, newActor)
  }

  "addPlayerBet" should "add a new bet for a player without existing bets" in {
    val table = TableOfBets.create
    val player = createPlayer("1", "John1")
    val bet = Bet(new Id, 1, 100)

    val result = table.addPlayerBet(player, bet)

    result shouldBe Right(TableOfBets(table.tableId, Map(player -> List(bet))))
  }

  it should "add a new bet for a player with existing bets" in {
    val player = createPlayer("2", "John2")
    val existingBet = Bet(new Id, 1, 100)
    val newBet = Bet(new Id, 2, 200)
    val table = TableOfBets(new Id, Map(player -> List(existingBet)))

    val result = table.addPlayerBet(player, newBet)

    result shouldBe Right(TableOfBets(table.tableId, Map(player -> List(newBet, existingBet))))
  }

  it should "not add a bet with an existing bet code for the same player" in {
    val player = createPlayer("3", "John3")
    val existingBet = Bet(new Id, 1, 100)
    val duplicateBet = Bet(new Id, 1, 200)
    val table = TableOfBets(new Id, Map(player -> List(existingBet)))

    val result = table.addPlayerBet(player, duplicateBet)

    result shouldBe Left(ExistingBetCode)
  }

  "deletePlayerBet" should "remove a bet with the specified bet code" in {
    val player = createPlayer("4", "John4")
    val bet1 = Bet(new Id, 1, 100)
    val bet2 = Bet(new Id, 2, 200)
    val table = TableOfBets(new Id, Map(player -> List(bet1, bet2)))

    val result = table.deletePlayerBet(player, 1)

    result shouldBe Right(TableOfBets(table.tableId, Map(player -> List(bet2))))
  }

  it should "do nothing if the bet code does not exist" in {
    val player = createPlayer("5", "John5")
    val bet1 = Bet(new Id, 1, 100)
    val bet2 = Bet(new Id, 2, 200)
    val table = TableOfBets(new Id, Map(player -> List(bet1, bet2)))

    val result = table.deletePlayerBet(player, 3)

    result shouldBe Right(TableOfBets(table.tableId, Map(player -> List(bet1, bet2))))
  }

  "cleanTable" should "remove all bets from the table" in {
    val player1 = createPlayer("6", "John6")
    val player2 = createPlayer("7", "Doe")
    val table = TableOfBets(
      new Id,
      Map(
        player1 -> List(Bet(new Id, 1, 100)),
        player2 -> List(Bet(new Id, 2, 200))
      )
    )

    val result = table.cleanTable()

    result shouldBe TableOfBets(table.tableId, Map.empty)
  }

  "TableOfBets.create" should "initialize a table with an empty bet map" in {
    val table = TableOfBets.create

    table.playerBets shouldBe empty
  }
}

