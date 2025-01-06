package reisaks.FinalProject.DomainModels

import org.scalatest.funsuite.AnyFunSuite
import reisaks.FinalProject.DomainModels.SystemMessages._

class BetSpec extends AnyFunSuite {

  test("Bet.create should successfully create a Bet with valid code and amount") {
    val result = Bet.create("10", "50")
    assert(result.isRight)
    result.foreach { bet =>
      assert(bet.betCode == 10)
      assert(bet.amount == BigDecimal(50))
    }
  }

  test("Bet.create should fail if betCode is not a valid integer") {
    val result = Bet.create("invalidCode", "50")
    assert(result.isLeft)
    assert(result == Left(IncorrectBetCode))
  }

  test("Bet.create should fail if amount is not a valid BigDecimal") {
    val result = Bet.create("10", "invalidAmount")
    assert(result.isLeft)
    assert(result == Left(IncorrectBetAmountType))
  }

  test("Bet.create should fail if amount is not greater than 0") {
    val result = Bet.create("10", "-10")
    assert(result.isLeft)
    assert(result == Left(IncorrectBetAmountInt))
  }

  test("Bet.create should fail if amount is exactly 0") {
    val result = Bet.create("10", "0")
    assert(result.isLeft)
    assert(result == Left(IncorrectBetAmountInt))
  }

  test("Bet.create should fail if betCode is less than 1") {
    val result = Bet.create("0", "50")
    assert(result.isLeft)
    assert(result == Left(IncorrectBetCode))
  }

  test("Bet.create should fail if betCode is greater than 100") {
    val result = Bet.create("101", "50")
    assert(result.isLeft)
    assert(result == Left(IncorrectBetCode))
  }

  test("Bet.create should fail for multiple errors, returning the first one") {
    val result = Bet.create("invalidCode", "invalidAmount")
    assert(result.isLeft)
    assert(result == Left(IncorrectBetCode)) // Ensures the first error is reported
  }

  test("Bet.create should handle edge cases for valid inputs (min and max values)") {
    val resultMin = Bet.create("1", "1")
    assert(resultMin.isRight)
    resultMin.foreach { bet =>
      assert(bet.betCode == 1)
      assert(bet.amount == BigDecimal(1))
    }

    val resultMax = Bet.create("100", "1000000")
    assert(resultMax.isRight)
    resultMax.foreach { bet =>
      assert(bet.betCode == 100)
      assert(bet.amount == BigDecimal(1000000))
    }
  }
}
