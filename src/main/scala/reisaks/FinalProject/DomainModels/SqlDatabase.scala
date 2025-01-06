package reisaks.FinalProject.DomainModels
import reisaks.FinalProject.ServerSide.AkkaActors.PlayerActorMessages.MessageToPlayer

import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet, SQLException}
import scala.math.BigDecimal.RoundingMode

object SqlDatabase {

  private val dbUrl = "jdbc:derby:SpinningWheelDb;create=true" // Derby database URL
  private var connection: Connection = _

  def initialize(): Unit = {
    try {
      connection = DriverManager.getConnection(dbUrl)
      val stmt = connection.createStatement()

      stmt.close()
      println("Database initialized successfully.")
    } catch {
      case e: SQLException =>
        println(s"Database initialization error: ${e.getMessage}")
        throw e
    }
  }

  def addAllTables(): Unit = {
    val stmt = connection.createStatement()

    // Create 'Users' table
    stmt.executeUpdate(
      """CREATE TABLE Users (
      UserID INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY,
      UserName VARCHAR(255) UNIQUE,
      Email VARCHAR(255) UNIQUE,
      Password VARCHAR(255)
    )"""
    )
    println("Table 'Users' created successfully.")

    // Create 'Bet' table
    stmt.executeUpdate(
      """CREATE TABLE Bet (
      BetID INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY,
      BetSector INT,
      BetAmount FLOAT,
      UserID INT NOT NULL,
      GameRoundID INT NOT NULL,
      FOREIGN KEY (UserID) REFERENCES Users(UserID),
      FOREIGN KEY (GameRoundID) REFERENCES GameRound(GameRoundID)
    )"""
    )
    println("Table 'Bet' created successfully.")

    // Create 'GameRound' table
    stmt.executeUpdate(
      """CREATE TABLE GameRound (
      GameRoundID INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY,
      TableName VARCHAR(255) NOT NULL,
      PlayerCount INT NOT NULL,
      KafkaRoundID VARCHAR(255) NOT NULL DEFAULT 'kafka-producer-error'
      Timestamp TIMESTAMP SET DEFAULT CURRENT_TIMESTAMP NOT NULL
    )"""
    )
    println("Table 'GameRound' created successfully.")

    // Create 'BetRoundResult' table
    stmt.executeUpdate(
      """CREATE TABLE BetRoundResult (
      RoundResultID INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY,
      WinningAmount FLOAT,
      UserID INT,
      GameRoundID INT,
      FOREIGN KEY (UserID) REFERENCES Users(UserID),
      FOREIGN KEY (GameRoundID) REFERENCES GameRound(GameRoundID)
    )"""
    )
    println("Table 'BetRoundResult' created successfully.")

    // Create 'Message' table
    stmt.executeUpdate(
      """CREATE TABLE Message (
      MessageID INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY,
      Message VARCHAR(255),
      UserID INT,
      GameRoundID INT,
      FOREIGN KEY (GameRoundID) REFERENCES GameRound(GameRoundID),
      FOREIGN KEY (UserID) REFERENCES Users(UserID)
    )"""
    )
    println("Table 'Message' created successfully.")

    // Create 'Balance' table
    stmt.executeUpdate(
      """CREATE TABLE Balance (
      BalanceID INT GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY,
      Amount FLOAT,
      UserID INT,
      FOREIGN KEY (UserID) REFERENCES Users(UserID)
    )"""
    )
    println("Table 'Balance' created successfully.")
    // Close the statement
    stmt.close()
  }

  def createUserDB(playerId: String, email: String, password: String): Boolean = {
    val insertSql = "INSERT INTO Users (UserName, Email, Password) VALUES (?, ?, ?)"
    try {
      val preparedStatement: PreparedStatement = connection.prepareStatement(insertSql)
      preparedStatement.setString(1, playerId)
      preparedStatement.setString(2, email)
      preparedStatement.setString(3, password)
      preparedStatement.executeUpdate()
      true
    } catch {
      case e: Exception =>
        false
    }
  }

  def isPlayerIdExist(playerId: String, password: String): Boolean = {
    val selectSql = "SELECT * FROM Users WHERE UserName = ? AND Password = ?"
    val preparedStatement: PreparedStatement = connection.prepareStatement(selectSql)
    preparedStatement.setString(1, playerId)
    preparedStatement.setString(2, password)
    val resultSet: ResultSet = preparedStatement.executeQuery()
    if (resultSet.next()) true
    else false
  }

  def getPlayerData(playerName: String): String = {
    val selectSql = "SELECT * FROM Users WHERE UserName = ?"
    val selectSqlBalance = "SELECT * FROM Balance WHERE UserID = ?"
    val preparedStatement: PreparedStatement = connection.prepareStatement(selectSql)
    val preparedStatementBalance: PreparedStatement = connection.prepareStatement(selectSqlBalance)
    preparedStatement.setString(1, playerName)
    getPlayerId(playerName) match {
      case Some(userID) =>
        val resultSet: ResultSet = preparedStatement.executeQuery()
        if (resultSet.next()) {
          val playerId = resultSet.getString("UserName")
          val email = resultSet.getString("Email")
          preparedStatementBalance.setInt(1, userID)
          val resBal = preparedStatementBalance.executeQuery()
          if (resBal.next()) {
            val balance = resBal.getFloat("Amount")
            s"$playerId $email $balance"
          }
          else ""
        }
        else ""
    }
  }

  def changePasswordDb(player: Player, currentPassword: String, newPassword: String): Unit = {
    val selectSql = "SELECT Password FROM Users WHERE UserName = ?"
    val updateSql = "UPDATE Users SET Password = ? WHERE UserName = ?"

    try {
      val preparedStatementSelect = connection.prepareStatement(selectSql)
      preparedStatementSelect.setString(1, player.playerId)
      val resultSet = preparedStatementSelect.executeQuery()

      if (resultSet.next()) {
        val storedPassword = resultSet.getString("password")
        if (storedPassword != currentPassword) {
          player.actorRef ! MessageToPlayer("incorrect-password")
        }
      }

      val preparedStatementUpdate = connection.prepareStatement(updateSql)
      preparedStatementUpdate.setString(1, newPassword)
      preparedStatementUpdate.setString(2, player.playerId)
      val rowsUpdated = preparedStatementUpdate.executeUpdate()

      player.actorRef ! MessageToPlayer("password-updated")
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }

  def changeEmailDb(player: Player, email: String): Unit = {
    val updateSql = "UPDATE Users SET Email = ? WHERE UserName = ?"
    var preparedStatement: PreparedStatement = null

    try {
      preparedStatement = connection.prepareStatement(updateSql)
      preparedStatement.setString(1, email)
      preparedStatement.setString(2, player.playerId)

      val rowsUpdated = preparedStatement.executeUpdate()

      if (rowsUpdated > 0) {
        player.actorRef ! MessageToPlayer(s"email-updated $email")
      } else {
        player.actorRef ! MessageToPlayer("email-exist")
      }
    }
    finally {
      if (preparedStatement != null) {
        try {
          preparedStatement.close()
        }
      }
    }
  }

  def changePlayerNameDb(player: Player, name: String): Unit = {
    val checkNameSql = "SELECT COUNT(*) FROM Users WHERE UserName = ?"
    val updateSql = "UPDATE Users SET UserName = ? WHERE UserID = ?"

    var preparedStatementCheckName: PreparedStatement = null
    var preparedStatementUpdate: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatementCheckName = connection.prepareStatement(checkNameSql)
      preparedStatementCheckName.setString(1, name)
      resultSet = preparedStatementCheckName.executeQuery()

      if (resultSet.next() && resultSet.getInt(1) > 0) {
        player.actorRef ! MessageToPlayer("name-exist")
        return
      }

      getPlayerId(player.playerId) match {
        case Some(userId) =>
          preparedStatementUpdate = connection.prepareStatement(updateSql)
          preparedStatementUpdate.setString(1, name)
          preparedStatementUpdate.setInt(2, userId)

          val rowsUpdated = preparedStatementUpdate.executeUpdate()
          if (rowsUpdated > 0) {
            player.actorRef ! MessageToPlayer(s"name-updated $name")
            player.playerId = name
          } else {
            player.actorRef ! MessageToPlayer("update-failed")
          }

        case None =>
          player.actorRef ! MessageToPlayer("player-not-found")
      }
    } catch {
      case e: SQLException =>
        println(s"Database error: ${e.getMessage}")
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatementCheckName != null) preparedStatementCheckName.close()
      if (preparedStatementUpdate != null) preparedStatementUpdate.close()
    }
  }

  def addBetDb(player: Player, bet: Bet, tableName: String): Unit = {
    val insertSql = "INSERT INTO Bet (BetSector, BetAmount, UserID, GameRoundID) VALUES (?, ?, ?, ?)"

    try {
      val userID = getPlayerId(player.playerId)
      val gameRoundID = getLatestGameRoundId(tableName)

      (userID, gameRoundID) match {
        case (Some(userID), Some(roundID)) =>
          val preparedStatement: PreparedStatement = connection.prepareStatement(insertSql)
          preparedStatement.setInt(1, bet.betCode)
          preparedStatement.setFloat(2, bet.amount.toFloat)
          preparedStatement.setInt(3, userID)
          preparedStatement.setInt(4, roundID)
          preparedStatement.executeUpdate()
      }

    }
  }

  def addGameRoundDb(tableName: String, playerCount: Int, kafkaRoundId: String): Unit = {
    val insertSql = "INSERT INTO GameRound (TableName, PlayerCount, KafkaRoundID) VALUES (?, ?, ?)"
    try {
      val preparedStatement: PreparedStatement = connection.prepareStatement(insertSql)
      preparedStatement.setString(1, tableName)
      preparedStatement.setInt(2, playerCount)
      preparedStatement.setString(3, kafkaRoundId)
      preparedStatement.executeUpdate()
      }
  }

  def addGameRoundResult(player: Player, tableName: String, winningAmount: Float): Unit = {
    val insertSql = "INSERT INTO BetRoundResult (WinningAmount, UserID, GameRoundID) VALUES (?, ?, ?)"
    try {
      val userId = getPlayerId(player.playerId)
      val gameRoundId = getLatestGameRoundId(tableName)
      (userId, gameRoundId) match {
        case (Some(userId), Some(gameRoundId)) =>
          val preparedStatement: PreparedStatement = connection.prepareStatement(insertSql)
          preparedStatement.setFloat(1, winningAmount)
          preparedStatement.setInt(2, userId)
          preparedStatement.setInt(3, gameRoundId)
          preparedStatement.executeUpdate()
      }
    }
  }

  def addInitBalanceDb(playerName: String): Unit = {
    val insertSql = "INSERT INTO Balance (Amount, UserID) VALUES (?, ?)"
    try {
      getPlayerId(playerName) match {
        case Some(userId) =>
          val preparedStatement: PreparedStatement = connection.prepareStatement(insertSql)
          preparedStatement.setFloat(1, 100)
          preparedStatement.setInt(2, userId)
          preparedStatement.executeUpdate()
      }
    }
  }

  def updateBalanceDb(player: Player, amount: Float): Unit = {
    val updateSql = "UPDATE Balance SET Amount = ? WHERE UserID= ?"
    var preparedStatement: PreparedStatement = null

    try {
      preparedStatement = connection.prepareStatement(updateSql)
      preparedStatement.setFloat(1, amount)
      getPlayerId(player.playerId) match {
        case Some(userId) =>
          preparedStatement.setInt(2, userId)
          preparedStatement.executeUpdate()
      }
    }
  }

  def addMessageDb(player: Player, tableName: String, message: String): Unit = {
    val insertSql = "INSERT INTO Message (Message, UserID, GameRoundID) VALUES (?, ?, ?)"
    try {
      val userId = getPlayerId(player.playerId)
      val gameRoundId = getLatestGameRoundId(tableName)
      (userId, gameRoundId) match {
        case (Some(userId), Some(gameRoundId)) =>
          val preparedStatement: PreparedStatement = connection.prepareStatement(insertSql)
          preparedStatement.setString(1, message)
          preparedStatement.setInt(2, userId)
          preparedStatement.setInt(3, gameRoundId)
          preparedStatement.executeUpdate()
      }
    }
  }

  private def getPlayerId(playerName: String): Option[Int] = {
    val selectSql = "SELECT UserID FROM Users WHERE UserName = ?"
    var preparedStatementSelect: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatementSelect = connection.prepareStatement(selectSql)
      preparedStatementSelect.setString(1, playerName)
      resultSet = preparedStatementSelect.executeQuery()

      if (resultSet.next()) {
        val userId = resultSet.getInt("UserID")
        Some(userId)
      }
      else None

    }
  }

  private def getLatestGameRoundId(tableName: String): Option[Int] = {
    val selectSql = "SELECT GameRoundID FROM GameRound WHERE TableName = ? ORDER BY Timestamp DESC FETCH FIRST ROW ONLY"
    var preparedStatementSelect: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatementSelect = connection.prepareStatement(selectSql)
      preparedStatementSelect.setString(1, tableName)
      resultSet = preparedStatementSelect.executeQuery()

      if (resultSet.next()) {
        val gameRoundId = resultSet.getInt("GameRoundID")
        Some(gameRoundId)
      } else {
        None
      }
    }
  }

  ///Stats queries

  private def getTotalBetsByPlayer(userId: Int): Option[Int] = {
    val query = "SELECT COUNT(BetID) AS TotalBets FROM Bet WHERE UserID = ?"
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatement = connection.prepareStatement(query)
      preparedStatement.setInt(1, userId)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next()) {
        Some(resultSet.getInt("TotalBets"))
      } else {
        None
      }
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }

  private def getTotalWinningsByPlayer(userId: Int): Option[Double] = {
    val query = "SELECT COALESCE(SUM(CASE WHEN WinningAmount > 0 THEN WinningAmount ELSE 0 END), 0) AS TotalWinnings FROM BetRoundResult WHERE UserID = ?"
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatement = connection.prepareStatement(query)
      preparedStatement.setInt(1, userId)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next()) {
        Some(resultSet.getDouble("TotalWinnings"))
      } else {
        None
      }
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }

  private def getAverageBetByPlayer(userId: Int): Option[BigDecimal] = {
    val query = "SELECT COALESCE(AVG(BetAmount), 0) AS AverageBet FROM Bet WHERE UserID = ?"
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatement = connection.prepareStatement(query)
      preparedStatement.setInt(1, userId)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next()) {
        Some(BigDecimal(resultSet.getDouble("AverageBet")).setScale(2, RoundingMode.HALF_UP))
      } else {
        None
      }
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }

  private def getTotalGamesPlayedByPlayer(userId: Int): Option[Int] = {
    val query = "SELECT COUNT(DISTINCT GameRoundID) AS GamesPlayed FROM Bet WHERE UserID = ?"
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatement = connection.prepareStatement(query)
      preparedStatement.setInt(1, userId)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next()) {
        Some(resultSet.getInt("GamesPlayed"))
      } else {
        None
      }
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }

  private def getBiggestBetByPlayer(userId: Int): Option[Double] = {
    val query = "SELECT MAX(BetAmount) AS BiggestBet FROM Bet WHERE UserID = ?"
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatement = connection.prepareStatement(query)
      preparedStatement.setInt(1, userId)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next()) {
        Some(resultSet.getDouble("BiggestBet"))
      } else {
        None
      }
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }

  private def getTotalWinsByPlayer(userId: Int): Option[Int] = {
    val query = """
    SELECT COUNT(*) AS TotalWins
    FROM BetRoundResult
    WHERE UserID = ? AND WinningAmount > 0
  """
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatement = connection.prepareStatement(query)
      preparedStatement.setInt(1, userId)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next()) {
        Some(resultSet.getInt("TotalWins"))
      } else {
        None
      }
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }


  //ServerStats

  private def getTotalNumberOfPlayers(): Option[Int] = {
    val query = "SELECT COUNT(UserID) AS TotalPlayers FROM Users WHERE UserName IS NOT NULL AND Email IS NOT NULL AND Password IS NOT NULL"
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatement = connection.prepareStatement(query)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next()) {
        Some(resultSet.getInt("TotalPlayers"))
      } else {
        None
      }
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }

  private def getMostActivePlayer(): Option[String] = {
    val query =
      """SELECT u.UserName, COUNT(b.BetID) AS TotalBets
        |FROM Bet b
        |JOIN Users u ON b.UserID = u.UserID
        |WHERE u.UserName IS NOT NULL
        |GROUP BY u.UserName
        |ORDER BY TotalBets DESC
        |FETCH FIRST ROW ONLY""".stripMargin
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatement = connection.prepareStatement(query)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next()) {
        Some(s"${resultSet.getString("UserName")}")
      } else {
        None
      }
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }

  private def getTotalNumberOfBets(): Option[Int] = {
    val query = "SELECT COUNT(BetID) AS TotalBets FROM Bet"
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatement = connection.prepareStatement(query)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next()) {
        Some(resultSet.getInt("TotalBets"))
      } else {
        None
      }
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }

  private def getAverageBetAcrossAllPlayers(): Option[BigDecimal] = {
    val query = "SELECT COALESCE(AVG(BetAmount), 0) AS AverageBet FROM Bet"
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatement = connection.prepareStatement(query)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next()) {
        Some(BigDecimal(resultSet.getDouble("AverageBet")).setScale(2, RoundingMode.HALF_UP))

      } else {
        None
      }
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }

  private def getPlayerWithHighestWinnings(): Option[String] = {
    val query =
      """SELECT
        |  CASE
        |    WHEN COALESCE(SUM(WinningAmount), 0) <= 0 THEN 'No-one-won'
        |    ELSE u.UserName
        |  END AS UserName,
        |  COALESCE(SUM(WinningAmount), 0) AS TotalWinnings
        |FROM BetRoundResult b
        |JOIN Users u ON b.UserID = u.UserID
        |WHERE u.UserName IS NOT NULL
        |  AND u.Email IS NOT NULL
        |  AND u.Password IS NOT NULL
        |GROUP BY u.UserName
        |ORDER BY TotalWinnings DESC
        |FETCH FIRST ROW ONLY""".stripMargin

    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatement = connection.prepareStatement(query)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next()) {
        Some(resultSet.getString("UserName"))
      } else {
        None
      }
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }

  private def getBiggestBet(): Option[Double] = {
    val query = "SELECT MAX(BetAmount) AS BiggestBet FROM Bet"
    var preparedStatement: PreparedStatement = null
    var resultSet: ResultSet = null

    try {
      preparedStatement = connection.prepareStatement(query)
      resultSet = preparedStatement.executeQuery()

      if (resultSet.next()) {
        Some(resultSet.getDouble("BiggestBet"))
      } else {
        None
      }
    } finally {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }

  def getCurrentPlayerStats(playerName: String): String = {
    getPlayerId(playerName) match {
      case Some(userId) =>
        val totalBets = getTotalBetsByPlayer(userId).getOrElse(0)
        val totalWinnings = getTotalWinningsByPlayer(userId).getOrElse(0.0)
        val averageBet = getAverageBetByPlayer(userId).getOrElse(0.0)
        val totalGames = getTotalGamesPlayedByPlayer(userId).getOrElse(0)
        val biggestBet = getBiggestBetByPlayer(userId).getOrElse(0.0)
        val totalWins = getTotalWinsByPlayer(userId).getOrElse()

        s"Player-Stats $totalBets $totalWinnings" +
          s" $averageBet $totalGames" +
          s" $biggestBet $totalWins"
      case None =>
        "Error-Catching-Data: Player not found"
    }
  }

  def getCurrentServerStats(): String = {
        val totalPlayers = getTotalNumberOfPlayers().getOrElse(0)
        val mostActivePlayer = getMostActivePlayer().getOrElse(0.0)
        val totalBets = getTotalNumberOfBets().getOrElse(0.0)
        val averageBet = getAverageBetAcrossAllPlayers().getOrElse(0)
        val highestWinning = getPlayerWithHighestWinnings().getOrElse(0.0)
        val biggestBet = getBiggestBet().getOrElse(0.0)

        s"Server-Stats $totalPlayers $mostActivePlayer" +
          s" $averageBet $totalBets" +
          s" $highestWinning $biggestBet"
  }

  def deleteUserById(player: Player, password: String): Unit = {
    val updateSql = "UPDATE Users SET UserName = NULL, Password = NULL, Email = NULL WHERE UserID = ?"
    try {
      getPlayerId(player.playerId) match {
        case Some(userID) =>
          if (isPlayerIdExist(player.playerId, password)) {
            val preparedStatement: PreparedStatement = connection.prepareStatement(updateSql)
            preparedStatement.setInt(1, userID)
            val rowsUpdated = preparedStatement.executeUpdate()
            if (rowsUpdated > 0) {
              player.actorRef ! MessageToPlayer("User-Deleted")
            } else {
              player.actorRef ! MessageToPlayer("No user found to delete.")
            }
          } else {
            player.actorRef ! MessageToPlayer("Incorrect-Delete-Password")
          }
        case None =>
          player.actorRef ! MessageToPlayer("User not found.")
      }
    } catch {
      case e: SQLException =>
        player.actorRef ! MessageToPlayer(s"An error occurred: ${e.getMessage}")
    }
  }
}
