import akka.actor.ActorSystem
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import reisaks.FinalProject.DomainModels._
import reisaks.FinalProject.ServerSide.GameLogic.BetEvaluationService
import reisaks.FinalProject.ServerSide.AkkaActors.PlayerActor

class BetEvaluationServiceSpec extends AnyFlatSpec with Matchers {

  // Setup ActorSystem for testing
  implicit val system: ActorSystem = ActorSystem("test-system")

  // Helper function to create a sample player
  def createPlayer(id: String, name: String): Player = {
    val newActor = system.actorOf(PlayerActor.props(id), s"playerActor-$id")
    Player(name, newActor)
  }

  // Helper function to create a sample bet
  def createBet(betCode: Int, amount: BigDecimal): Bet = Bet(new Id, betCode, amount)

  "evaluateSum" should "return Some(sum) if player has bets" in {
    val player = createPlayer("1", "John")
    var tableOfBets = TableOfBets.create
    val bet1 = createBet(5, 10)
    val bet2 = createBet(6, 20)


      // Add the player's bets
    tableOfBets = tableOfBets.addPlayerBet(player, bet1).getOrElse(tableOfBets)
    tableOfBets = tableOfBets.addPlayerBet(player, bet2).getOrElse(tableOfBets)



    // Evaluating the sum for a winning number
    val result = BetEvaluationService.evaluateSum(player, tableOfBets, 6)


    result shouldBe Some(50)
  }

  it should "return Some(sum) if the player has bets with a mix of wins and losses" in {
    val player = createPlayer("2", "Alice")
    var tableOfBets = TableOfBets.create
    val bet1 = createBet(6, 10)  // Win bet
    val bet2 = createBet(7, 15)  // Lose bet
    val bet3 = createBet(5, 25)  // Lose bet

    // Add the player's bets
    tableOfBets = tableOfBets.addPlayerBet(player, bet1).getOrElse(tableOfBets)
    tableOfBets = tableOfBets.addPlayerBet(player, bet2).getOrElse(tableOfBets)
    tableOfBets = tableOfBets.addPlayerBet(player, bet3).getOrElse(tableOfBets)

    // Evaluating the sum for a winning number
    val result = BetEvaluationService.evaluateSum(player, tableOfBets, 6)

    result shouldBe Some(-10) // 10 * 3 (win) - 15 (lose) - 25 (lose) => 30
  }

  it should "return None if the player has no bets" in {
    val player = createPlayer("3", "Bob")
    val tableOfBets = TableOfBets.create

    // No bets for this player
    val result = BetEvaluationService.evaluateSum(player, tableOfBets, 6)

    result shouldBe None
  }

  it should "return sum if the player has losses" in {
    val player = createPlayer("4", "Eve")
    var tableOfBets = TableOfBets.create
    val bet1 = createBet(7, 20)  // Lose bet
    val bet2 = createBet(8, 10)  // Lose bet

    // Add the player's bets
    tableOfBets = tableOfBets.addPlayerBet(player, bet1).getOrElse(tableOfBets)
    tableOfBets = tableOfBets.addPlayerBet(player, bet2).getOrElse(tableOfBets)

    // Evaluating the sum for a non-matching number
    val result = BetEvaluationService.evaluateSum(player, tableOfBets, 9)

    result shouldBe Some(-30) // -20 - 10 => -30
  }

  it should "apply correct multipliers for winning numbers" in {
    val player = createPlayer("5", "Charlie")
    var tableOfBets = TableOfBets.create
    val bet1 = createBet(100, 50)  // Win bet on 100

    // Add the player's bets
    tableOfBets = tableOfBets.addPlayerBet(player, bet1).getOrElse(tableOfBets)

    // Evaluating the sum for the winning number 100
    val result = BetEvaluationService.evaluateSum(player, tableOfBets, 100)

    result shouldBe Some(2500) // 50 * 50 = 2500 (Multiplier for 100)
  }
}
