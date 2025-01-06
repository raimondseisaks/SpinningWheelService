import React, { useState, useEffect } from 'react';
import { useWebSocket } from './WebSocketHandler';
import './lobbyStyle.css';
import {useNavigate} from "react-router-dom";
import Footer from "./Footer";

const LobbyPage = () => {
    const navigate = useNavigate();
    const [availableTables, setAvailableTables] = useState([]);
    const { socket, userData, sendMessage } = useWebSocket(); // Use WebSocket from context
    const [newTableId, setNewTableId] = useState('');
    const [newTablePassword, setNewTablePassword] = useState('');
    const [joinTableId, setJoinTableId] = useState('');
    const [joinTablePassword, setJoinTablePassword] = useState('');
    const [createTableError, setCreateTableError] = useState('')
    const [joinTableError, setJoinTableError] = useState('')


    useEffect(() => {
        if (socket) {
            handleShowAvailability();
            socket.onmessage = (event) => {
                if (event.data.match(/Table-(\d+) has (\d+) seats/)) {
                    updateTableList(event.data);
                }
                else if (event.data === "table-created-successfully") {
                    navigate('/table')
                }
                else if (event.data === "private-table-joined") {
                    navigate('/table')
                }
                else if (event.data === "incorrect-table-password" || event.data === "private-table-not-exist") {
                    setJoinTableError("Incorrect table name or password")
                }
                else if (event.data === "incorrect-tableName") {
                    setCreateTableError("Table with this name already exist")
                }
            };
        }
    }, [socket]);

    const updateTableList = (message) => {
        const matches = message.match(/Table-(\d+) has (\d+) seats/);
        if (matches) {
            const tableNumber = parseInt(matches[1], 10); // Convert to a number for proper sorting
            const seats = parseInt(matches[2], 10);

            setAvailableTables((prevTables) => {
                const existingIndex = prevTables.findIndex((item) => item.tableNumber === tableNumber);
                let newTables;

                if (existingIndex > -1) {
                    newTables = [...prevTables];
                    newTables[existingIndex].seats = seats;
                } else {
                    newTables = [...prevTables, { tableNumber, seats }];
                }
                return newTables.sort((a, b) => a.tableNumber - b.tableNumber);
            });
        }
    };

    const handleJoinTable = (tableNumber) => {
        const command = `Join-Table Table-${tableNumber}`;
        sendMessage(command);
        navigate('/table')

    };

    const handleShowAvailability = () => {
        const command = "Show-Available-Tables";
        sendMessage(command);
    };

    const handleExitServer = () => {
        const command = "Exit-Server";
        sendMessage(command);
        window.location.href = "http://localhost:3000";
    };

    const handleProfileView = () => {
        navigate("/profile")
    };

    const handleStatsView = () => {
        navigate("/stats")
    }

    const handlePrivateTableJoin = () => {
        const invalidCharsPattern = /[\/\\.\s]/;
        if (!joinTableId && !joinTablePassword) {
            setJoinTableError("Table Name and Password cannot be empty.");
            return
        }
        else if (invalidCharsPattern.test(joinTableId)) {
            setJoinTableError("Invalid format of username (do not use spaces and special chars)")
            return;
        }
        sendMessage(`Join-Private-Table ${joinTableId} ${joinTablePassword}`)

    }

    const handlePrivateTableCreate = () => {
        const invalidCharsPattern = /[\/\\.\s]/;
        if (invalidCharsPattern.test(newTableId)) {
            setCreateTableError("Invalid format of username (do not use spaces and special chars)")
            return;
        }
        else if (!newTableId && !newTablePassword) {
            setCreateTableError("Table Name and Password cannot be empty.");
        }
        else if (newTablePassword.length < 8) {
            setCreateTableError("Password must be at least 8 characters long")
            return
        }

        sendMessage(`Create-Private-Table ${newTableId} ${newTablePassword}`)
    }

    return (
        <div>
            <nav className="navbar bg-danger">
                <div className="container-fluid">
                    <a className="navbar-brand text-light text" href="#">Spinning Wheel Server</a>
                    <div className="nav-controls">
                        <button id="view-profile" className="btn btn-outline-light"
                                onClick={handleProfileView}>
                            Edit Profile Data
                        </button>
                        <button id="view-profile" className="btn btn-outline-light"
                                onClick={handleStatsView}>
                            Your stats
                        </button>
                        <button id="exit-server" className="btn btn-outline-light"
                                onClick={handleExitServer}>Exit-Server
                        </button>
                    </div>
                </div>
            </nav>

            <div className="allTables">
                <h1 className="text">
                    {userData && userData[0] ? `WELCOME BACK ${String(userData[0]).toUpperCase()}  - CHOOSE TABLE` : `None`}
                </h1>
                <div className="text-center">
                    <div className="row mt-5">
                        <div className="col">
                            <img className="tableImg" src={`${process.env.PUBLIC_URL}/Assets/table1.png`} alt="table1"/>
                            <button id="join-table-1" type="submit" className="btn btn-outline-danger mt-2"
                                    onClick={() => handleJoinTable(1)}>Join to
                                Table 1
                            </button>
                        </div>
                        <div className="col">
                            <img className="tableImg" src={`${process.env.PUBLIC_URL}/Assets/table2.png`} alt="table1"/>

                            <button id="join-table-2" type="submit" className="btn btn-outline-danger mt-2"
                                    onClick={() => handleJoinTable(2)}>Join to
                                Table 2
                            </button>
                        </div>
                        <div className="col">
                            <img className="tableImg" src={`${process.env.PUBLIC_URL}/Assets/table3.png`} alt="table1"/>

                            <button id="join-table-3" type="submit" className="btn btn-outline-danger mt-2 "
                                    onClick={() => handleJoinTable(3)}>Join to
                                Table 3
                            </button>
                        </div>
                    </div>

                    <div className="row mt-5">
                        <div className="col">
                            <img className="tableImg" src={`${process.env.PUBLIC_URL}/Assets/table4.png`} alt="table1"/>

                            <button id="join-table-4" type="submit" className="btn btn-outline-danger mt-2"
                                    onClick={() => handleJoinTable(4)}>Join to
                                Table 4
                            </button>
                        </div>
                        <div className="col">
                            <img className="tableImg" src={`${process.env.PUBLIC_URL}/Assets/table5.png`} alt="table1"/>

                            <button id="join-table-5" type="submit" className="btn btn-outline-danger mt-2"
                                    onClick={() => handleJoinTable(5)}>Join to
                                Table 5
                            </button>
                        </div>
                        <div className="col">
                            <img className="tableImg" src={`${process.env.PUBLIC_URL}/Assets/table6.png`} alt="table1"/>
                            <button id="join-table-6" type="submit" className="btn btn-outline-danger mt-2"
                                    onClick={() => handleJoinTable(6)}>Join to
                                Table 6
                            </button>
                        </div>
                    </div>

                    <div className="row mt-5">
                        <div className="col">
                            <img className="tableImg" src={`${process.env.PUBLIC_URL}/Assets/table7.png`} alt="table1"/>

                            <button id="join-table-7" type="submit" className="btn btn-outline-danger mt-2"
                                    onClick={() => handleJoinTable(7)}>Join to
                                Table 7
                            </button>
                        </div>
                        <div className="col">
                            <img className="tableImg" src={`${process.env.PUBLIC_URL}/Assets/table8.png`} alt="table1"/>

                            <button type="submit" className="btn btn-outline-danger mt-2"
                                    onClick={() => handleJoinTable(8)}>Join to Table 8
                            </button>
                        </div>
                        <div className="col">
                            <img className="tableImg" src={`${process.env.PUBLIC_URL}/Assets/table9.png`} alt="table1"/>

                            <button type="submit" className="btn btn-outline-danger mt-2"
                                    onClick={() => handleJoinTable(9)}>Join to Table 9
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <div className="gameRules">
                <h1 id="rulesHeader">Game rules</h1>
                <p id="rulesText"> Spinning wheel contains 100 different sectors, numbered from 1 to 100. During each
                    round, wheel is being rotated for 5 seconds and it stops randomly at some sector.
                    Odd sectors have a winning multiplier equal to : bet x 2
                    Even sectors (except for sector number 100) has a winning multiplier : bet x 3
                    Sector number 100 has a winning multiplier : bet x 50

                    Game rounds happen one after another, using automatic scheduling.</p>
            </div>
            <div>
                <button id="show-button" className="btn btn-outline-danger btn-lg"
                        onClick={handleShowAvailability}>Refresh availability of tables
                </button>
                <div className="availability">
                    <div className="availability-container-1">
                        <ol id="table-list">
                            {availableTables.map(table => (
                                <li className="table-availability"
                                    key={table.tableNumber}>Table {table.tableNumber} has {table.seats} free seats</li>
                            ))}
                        </ol>
                    </div>
                    <div className="availability-container-2">
                        <img className="availability-logo" src="../Assets/loginLogo.webp" alt="table1"/>
                    </div>
                </div>
            </div>

            <div className="private-table-container">
                <div className="dashboard">
                    <h2>Private tables section</h2>
                    <div className="dashboardContainer">
                        <div className="dashboardSection create-table">
                            <h2>Create own table</h2>
                            {createTableError && <p className="text-light fw-bold">{createTableError}</p>}
                            <input
                                type="text"
                                placeholder="Create unique Table-Name"
                                className="form-control table-input"
                                value={newTableId}
                                onChange={(e) => setNewTableId(e.target.value)}
                            />
                            <input
                                type="Password"
                                placeholder="Create Password for table"
                                className="form-control table-input"
                                value={newTablePassword}
                                onChange={(e) => setNewTablePassword(e.target.value)}
                            />
                            <button
                                type="submit"
                                className="btn btn-outline-light chat-button"
                                onClick={handlePrivateTableCreate}>
                                Create Table
                            </button>

                        </div>
                        <div className="dashboardSection join-private-table">
                            <h2>Join to the private table</h2>
                            <div className="">
                                {joinTableError && <p className="text-light fw-bold">{joinTableError}</p>}
                                <input
                                    type="text"
                                    placeholder="Table-Name"
                                    className="form-control table-input"
                                    value={joinTableId}
                                    onChange={(e) => setJoinTableId(e.target.value)}
                                    required
                                />
                                <input
                                    type="Password"
                                    placeholder="Password"
                                    className="form-control table-input"
                                    value={joinTablePassword}
                                    onChange={(e) => setJoinTablePassword(e.target.value)}
                                    required
                                />
                                <button
                                    type="submit"
                                    className="btn btn-outline-light chat-button"
                                    onClick={handlePrivateTableJoin}>
                                    Join Table
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

export default LobbyPage;
