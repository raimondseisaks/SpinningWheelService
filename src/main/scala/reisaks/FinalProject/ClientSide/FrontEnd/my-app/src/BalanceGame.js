import React, { useState } from "react";
import { useWebSocket } from "./WebSocketHandler";
import "./BalanceGameStyle.css";

const BalanceContainer = () => {
    const { userData, sendMessage } = useWebSocket();
    const [showGame, setShowGame] = useState(false);
    const [depositAmount, setDepositAmount] = useState("");
    const [bonusMessage, setBonusMessage] = useState("");
    const [luckyCard, setLuckyCard] = useState(null);
    const [timeRestrictedMessage, setTimeRestrictedMessage] = useState("");
    const [multiplier, setMultiplier] = useState(0);
    const [gameOver, setGameOver] = useState(false);

    // Function to check if the current time is within the allowed time range (12:00 PM to 12:30 PM)
    const isWithinAllowedTime = () => {
        const currentTime = new Date();
        const hours = currentTime.getHours();
        const minutes = currentTime.getMinutes();
        return hours === 12 && minutes >= 0 && minutes <= 56;
    };

    // Function to calculate multiplier based on deposit
    const calculateMultiplier = (deposit) => {
        const cappedDeposit = Math.min(deposit, 25); // Cap the deposit at 25
        const multiplierValue = 3 - (cappedDeposit / 25) * 3;
        return parseFloat(multiplierValue.toFixed(2));
    };

    // Function to start the mini-game
    const startMiniGame = (e) => {
        e.preventDefault();
        if (!isWithinAllowedTime()) {
            setTimeRestrictedMessage("The game is only available between 12:00 PM and 12:30 PM.");
            return;
        } else {
            setTimeRestrictedMessage("");
        }

        if (depositAmount && parseFloat(depositAmount) > 0) {
            const multiplierValue = calculateMultiplier(depositAmount);
            setMultiplier(multiplierValue);
            setShowGame(true); // Show the mini-game
            setBonusMessage("");
            setLuckyCard(Math.floor(Math.random() * 3) + 1); // Random lucky card (1, 2, or 3)
            setGameOver(false);
        } else {
            alert("Please enter a valid deposit amount!");
        }
    };

    const handleCardSelection = (card) => {
        const deposit = parseFloat(depositAmount);

        if (card === luckyCard) {
            const total = deposit * multiplier;
            setBonusMessage(
                `Congratulations! You picked the lucky card! You received a bonus of $${total.toFixed(2)}!`
            );
            userData[2] = (parseFloat(userData[2]) + total).toFixed(2);
            sendMessage(`update-balance-amount ${userData[2]}`);
        } else {
            setBonusMessage(
                `Bad luck! You don't get a bonus.`
            );
        }
        setShowGame(false); // Hide the game after the result
        setGameOver(true); // Set the game as over
    };

    return (
        <div className="balance-container">
            <div className="form-group">
                <h2>Current Balance</h2>
                <h3 className="balance-text">Your current balance: ${userData[2]}</h3>
                {!gameOver && ( // Only show Add Balance button if game is not over
                    <form onSubmit={startMiniGame}>
                        <input
                            type="number"
                            placeholder="Enter deposit amount"
                            className="form-control"
                            value={depositAmount}
                            onChange={(e) => setDepositAmount(e.target.value)}
                            required
                        />
                        <button className="btn btn-outline-light mt-3" type="submit">
                            Calculate Multiplier
                        </button>
                    </form>
                )}
                {timeRestrictedMessage && (
                    <p className="text-light mt-3">{timeRestrictedMessage}</p>
                )}

                {/* Show multiplier only after clicking "Add Balance" */}
                {showGame && depositAmount && (
                    <div className="mt-3">
                        <p className="text-light">Your multiplier: {multiplier}</p>
                    </div>
                )}
            </div>

            {/* Show mini-game when ready */}
            {showGame && (
                <div className="mini-game">
                    <h3 className="text-light">Pick a Card to See Your Bonus!</h3>
                    <div className="cards">
                        <button className="card-game" onClick={() => handleCardSelection(1)}>
                            Card 1
                        </button>
                        <button className="card-game" onClick={() => handleCardSelection(2)}>
                            Card 2
                        </button>
                        <button className="card-game" onClick={() => handleCardSelection(3)}>
                            Card 3
                        </button>
                    </div>
                </div>
            )}

            {/* Show bonus message */}
            {bonusMessage && <p className="text-light fw-bold mt-3">{bonusMessage}</p>}
        </div>
    );
};

export default BalanceContainer;
