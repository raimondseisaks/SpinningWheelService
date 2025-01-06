import React, {useEffect, useRef, useState} from 'react';
import {useWebSocket} from './WebSocketHandler';
import './tableStyle.css';
import {useNavigate} from "react-router-dom";
import Footer from "./Footer";

const TablePage = () => {
    const { socket, sendMessage, userData } = useWebSocket();
    const [betCode, setBetCode] = useState('');
    const [chatMessage, setChatMessage] = useState('');
    const [amount, setAmount] = useState('');
    const wheelRef = useRef(null);
    const navigate = useNavigate();
    const [error, setError] = useState('');
    const [BettingPhase, setBettingPhase] = useState(false);
    const [currentBalance, setCurrentBalance] = useState(userData[2]);
    const [availability, setAvailability] = useState('')

    let stateMessages = ["Game is started! Wheel is spinning.......", "Round has ended"]
    let bets = [];

    useEffect(() => {
        if (socket) {
            socket.onmessage = (event) => {
                const message = event.data;

                if (!isNaN(message)) {
                    spinWheel(message)
                }

                else if (message.startsWith("Betting has ended!")) {
                    let logText = document.getElementById("tableState")
                    logText.textContent = message
                    setBettingPhase(false)
                    const deleteButtons = document.querySelectorAll(".delete-bet-button");
                    deleteButtons.forEach(button => {
                        button.remove();
                    });
                }

                else if (stateMessages.includes(message)) {
                    let logText = document.getElementById("tableState")
                    logText.textContent = message
                }

                else if (message === "Round started. Place your bets!") {
                    let logText = document.getElementById("tableState")
                    logText.textContent = message
                    const betList = document.getElementById("listOfBets");
                    setBettingPhase(true)

                    bets = [];

                    while (betList.firstChild) {
                        betList.removeChild(betList.firstChild);
                    }

                    const newRoundMessage = document.createElement("div");
                    newRoundMessage.textContent = "New round started. Place your bets!";
                    newRoundMessage.className = "new-round-message";
                    betList.appendChild(newRoundMessage);

                }

                else if (message.startsWith("You bet on")) {
                    const betRegex = /You bet on (\w+) with amount (\d+\.\d*)/;
                    const match = message.match(betRegex);

                    if (match) {
                        const betCode = match[1];
                        const amount = match[2];
                        updateBetLog(betCode, amount);
                    }
                }

                else if (message.startsWith("Winning number")) {
                    const betRegex = message.match(/Winning number (\d+)!(?: You (won|lose) (\d+\.\d*) euro)?/i);
                    const winningNumber = parseInt(betRegex[1]);
                    const result = betRegex[2];
                    const value = betRegex[3] ? betRegex[3] : null;


                    if (result === "won") {
                        userData[2] = parseFloat(userData[2]).toFixed(2) + parseFloat(value).toFixed(2)
                        updateDashBoard(winningNumber,result,value)
                    }
                    else if (result === "lose") {
                        updateDashBoard(winningNumber, value,result,)
                    }
                }

                else if (message.startsWith("deleted-bet")) {
                    const logText = document.getElementById("listOfBets");
                    const betRegex = message.match(/deleted-bet (\w+)/i);
                    const betCode = betRegex[1];


                    const betIndex = bets.findIndex((bet) => bet.betCode === betCode);
                    if (betIndex !== -1) {
                        bets.splice(betIndex, 1);

                        const betDivs = logText.querySelectorAll(".bet");
                        betDivs.forEach((div) => {
                            console.log(div)
                            if (div.textContent.includes(`You bet on ${betCode}`)) {
                                logText.removeChild(div);
                            }
                        });

                        if (bets.length === 0) {
                            const placeholder = document.createElement("div");
                            placeholder.textContent = "No bets placed.";
                            placeholder.className = "placeholder-message";
                            logText.appendChild(placeholder);
                        }
                    }
                }

                else if (message.startsWith("chatMessage")) {
                    const chatRegex = message.match(/chatMessage (\S.*)/i);
                    const chatMessage = chatRegex[1];
                    const chatList = document.getElementById("chat-list");
                    const chatItem = document.createElement("div");
                    chatItem.className = "chat-item";
                    chatItem.textContent = chatMessage;
                    chatList.appendChild(chatItem);
                    scrollToLastChild("chat-list")
                }

                else if (message.startsWith("availability")) {
                    setAvailability(message.split(" ").slice(1))
                }

            };
        }
    }, [socket]);

    const validateBetForm = (betCode, amount) => {
        if (!BettingPhase) {
            setError("You can't add bet when game round is finished");
            return false;
        }

        else if (isNaN(amount) || amount <= 0) {
            setError("Bet amount must be greater than 0 and be a number");
            return false;
        }

        else if (isNaN(betCode) || betCode < 1 || betCode > 100) {
            setError("Bet code must be a number from 1 to 100");
            return false;
        }

        // Check if bet with the same code already exists
        else if (bets.some((bet) => bet.betCode === betCode)) {
            setError("Bet with this code already exists");
            return false;
        }

        else if (parseFloat(amount) > parseFloat(userData[2])) {
            setError("You dont have enough amount in balance");
            return false;
        }


        setError('');
        return true;
    };

    const sendChatMessage = (message) => {
        const command = `chat-message ${message}`;
        setChatMessage("")
        sendMessage(command);
    }

    const updateBetLog = (betCode, amount) => {
        const logText = document.getElementById("listOfBets");
        const newItem = document.createElement("div");
        newItem.className = "bet";

        const deleteButton = document.createElement("button");
        deleteButton.textContent = "Delete";
        deleteButton.className = "delete-bet-button btn btn-light btn-sm";
        deleteButton.style.marginLeft = "10px";

        // Remove placeholder if it exists
        if (bets.length === 0 && logText.firstChild) {
            logText.removeChild(logText.firstChild);
        }

        // Add new bet to the array and the DOM
        bets.push({ betCode, amount });
        newItem.textContent = `You bet on ${betCode} with amount ${amount}`;
        newItem.appendChild(deleteButton);
        logText.appendChild(newItem);
        scrollToLastChild("listOfBets")
        // Add event listener to delete button
        deleteButton.addEventListener("click", () => {
           handleDeleteBet(betCode)
        });
    };

    const updateDashBoard = (winningNumber, state,  amount) => {
        const logText = document.getElementById("result-list");
        const newItem = document.createElement("div");
        newItem.className = "bet";

        newItem.textContent = `Winning number ${winningNumber}! You ${state} ${amount}`;
        logText.appendChild(newItem);
        scrollToLastChild("result-list")
    };

    function spinWheel(roll) {
        const wheel = document.querySelector(".roulette-wrappers .wheels");

        const order = Array.from({ length: 100 }, (_, i) => i + 1);
        const position = order.indexOf(Number(roll) + 1);

        const rows = 11;
        const card = 75 + 3 * 2;


        let landingPosition = (rows * 100 * card + position * card + 30) - (card*51);
        const randomize = Math.floor(Math.random() * 75) - (75 / 3);
        landingPosition += randomize;

        const object = {
            x: (Math.floor(Math.random() * 50) / 100).toFixed(2),
            y: (Math.floor(Math.random() * 20) / 100).toFixed(2),
        };


        wheel.style.transitionTimingFunction = `cubic-bezier(0, ${object.x}, ${object.y}, 1)`;
        wheel.style.transitionDuration = '5s';
        wheel.style.transform = `translate3d(-${landingPosition}px, 0px, 0px)`;

        setTimeout(() => {
            wheel.style.transitionTimingFunction = '';
            wheel.style.transitionDuration = '';

            const resetTo = -(position * card + randomize + 30 - (card*51));
            wheel.style.transform = `translate3d(${resetTo}px, 0px, 0px)`;
        }, 5 * 1000);
    }

    const handleBetChange = (e) => {
        setBetCode(e.target.value);
    };

    const handleAmountChange = (v) => {
        setAmount(v.target.value);
    };

    const handleExitTable = () => {
        const command = "Exit-Table";
        sendMessage(command);
        sendMessage(`update-balance-amount ${userData[2]}`);
        navigate('/lobby')
    };

    const handleDeleteBet = (betCode) => {
        const command = `delete-bet ${betCode}`;
        console.log(command)
        sendMessage(command);
    }

    const handleSubmitBet = (e) => {
        e.preventDefault()
        if (betCode && amount) {
            if (!validateBetForm(betCode, amount)) return;
            const command = `Add-Bet ${betCode} ${parseFloat(amount).toFixed(2)}`;
            sendMessage(command);
            userData[2] = userData[2] - amount
            setCurrentBalance(userData[2])
            setAmount("")
            setBetCode("")
            setError("")
        }
        else {
            setError("Fields cannot be empty")
        }
    };

    useEffect(() => {
        if (wheelRef.current) {
            initWheel();
        }
    }, []);

    const initWheel = () => {
        if (wheelRef.current) {
            const $wheel = wheelRef.current;
            let rows = "";

            let row = "";
            for (let i = 1; i <= 99; i++) {
                const color = i % 2 === 0 ? 'blacks' : 'reds';
                row += `<div class='cards ${color}'>${i}</div>`;
            }
            row += "<div class='cards greens'>100</div>";
            row = `<div class='rows'>${row}</div>`;

            rows = new Array(201).fill(row).join("");

            $wheel.innerHTML = rows;
        }
    };

    function scrollToLastChild(elemName) {
        const chatList = document.getElementById(elemName);
        if (chatList) {
            chatList.scrollTop = chatList.scrollHeight;
        }
    }

    return (
        <div>
            <nav className="navbar bg-danger">
                <div className="container-fluid">
                    <a className="navbar-brand text-light text" href="#">Spinning Wheel Server</a>
                    <button id="exit-table" className="btn btn-outline-light" onClick={handleExitTable}>Exit-Table</button>
                </div>
            </nav>
            <h1 id="mainText">The Spinning Wheel Game</h1>
            <div className="roulettes">
                <div className="roulette-wrappers">
                    <div className="selectors"></div>
                    <div className="wheels" ref={wheelRef}></div>
                </div>
            </div>
            <div className="container">
                <h2>Control panel of wheel</h2>
                <div className="controlsContainer">
                    <div className="bettingSys controls">
                        <h2>Add Bet</h2>
                        <div className="form-group">
                            <div className="bet-form-container">
                            <label htmlFor="betCode">Choose sector:</label>
                            <input
                                type="text"
                                id="betCode"
                                placeholder="Enter bet code"
                                className="bet-form form-control"
                                value={betCode}
                                onChange={handleBetChange}
                            />
                            </div>
                            <div className="bet-form-container">
                            <label htmlFor="betCode">Write bet:</label>
                            <input
                                type="text"
                                id="amount"
                                placeholder="Enter Amount"
                                className="bet-form form-control"
                                value={amount}
                                onChange={handleAmountChange}
                            />
                            </div>
                            {error && <div className="error-text">{error}</div>} {/* Display validation error */}
                            <button type="submit" className="btn btn-outline-light bet-btn" onClick={handleSubmitBet}>Submit
                                Bet
                            </button>
                        </div>
                    </div>
                    <div className="announcment controls">
                        <h2>State Of Wheel</h2>
                        <p id="tableState" className="mt-3"></p>
                        <p className="mt-4">{availability && availability ? `Playing players : ${availability}` : "Null"}</p>
                        <p className="mt-4">Your current balance : ${currentBalance}</p>
                    </div>
                    <div className="stats controls">
                        <h2>Your Round History</h2>
                        <div id="result-list"></div>
                    </div>
                </div>
            </div>
            <div className="container2">
                <div className="dashboard">
                    <h2>Dashboard</h2>
                    <div className="dashboardContainer">
                        <div className="dashboardSection betList">
                            <h2>List of bets</h2>
                            <div id="listOfBets">
                            </div>
                        </div>
                        <div className="dashboardSection chatSys">
                            <h2>Chat</h2>
                            <div id="chat-list">
                            </div>
                            <div className="chat-form-container">
                                <input
                                    type="text"
                                    id="chat-message"
                                    placeholder="Send message"
                                    className="chat-form form-control"
                                    value={chatMessage}
                                    onChange={(event) => setChatMessage(event.target.value)}
                                    required
                                />
                                <button
                                    type="submit"
                                    className="btn btn-outline-light chat-button"
                                    onClick={() => sendChatMessage(chatMessage)}>
                                    Send message
                                </button>
                            </div>

                        </div>
                    </div>
                </div>
            </div>
            <Footer/>
        </div>
    );
};

export default TablePage;
