import React, {useEffect, useState} from 'react';
import './ProfilePageStyle.css';
import {useNavigate} from "react-router-dom";
import Footer from "./Footer";
import {useWebSocket} from "./WebSocketHandler";
import BalanceGame from "./BalanceGame";

const ProfilePage = () => {
    const navigate = useNavigate();
    const { socket, userData, sendMessage } = useWebSocket(); // Use WebSocket from context
    const [name, setName] = useState("");
    const [balance, setBalance] = useState("");
    const [email, setEmail] = useState("");
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [confirmNewPassword, setConfirmPassword] = useState("");
    const [passwordErrorMessage, setPasswordErrorMessage] = useState('');
    const [nameErrorMessage, setNameErrorMessage] = useState('');
    const [emailErrorMessage, setEmailErrorMessage] = useState('');
    const [emailPlaceholder, setEmailPlaceholder] = useState(`Your current email : ${userData[1]}`);
    const [playerNamePlaceholder, setPlayerNamePlaceholder] = useState(`Your current name : ${userData[0]}`);
    const [deletePassword, setDeletePassword] = useState('');
    const [deleteUserErrorMessage, setDeleteUserErrorMessage] = useState('');
    const [question, setQuestion] = useState('');
    const [error, setError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');

    const handleSupportSubmit = (event) => {
        event.preventDefault();

        sendMessage(`support ${userData[1]} ${question}`)
        setQuestion("")
    };

    useEffect(() => {
        if (socket) {
            socket.onmessage = (event) => {
                const message = event.data;

                console.log(message)
                if (message.startsWith("incorrect-password")) {
                    setPasswordErrorMessage("Incorrect current password")
                }
                else if (message.startsWith("password-updated")) {
                    alert('Password changed successfully!');
                }
                else if (message.startsWith("email-updated")) {
                    let mailData = message.split(" ")
                    userData[1] = mailData[1]
                    setEmailPlaceholder(`Your current name : ${userData[1]}`)
                    alert(`Your new email : ${userData[1]}`);
                }
                else if (message.startsWith("email-exist")) {
                    setEmailErrorMessage("That email exist")
                }
                else if (message.startsWith("name-updated")) {
                    let nameData = message.split(" ")
                    userData[0] = nameData[1]
                    setPlayerNamePlaceholder(`Your current name : ${userData[0]}`)
                    alert(`Your new name : ${userData[0]}`);
                }
                else if (message.startsWith("name-exist")) {
                    setNameErrorMessage("That name exist")
                }
                else if (message.startsWith("User-Deleted")) {
                    window.location.href = "http://localhost:3000";
                }
                else if (message.startsWith("Incorrect-Delete-Password")) {
                    setDeleteUserErrorMessage("Incorrect password")
                }
                else if (message === "email-sent") {
                    setSuccessMessage("Email-Sent")
                }
            }
        }
    }, [socket]);


    const handleNameUpdate = () => {
        const invalidCharsPattern = /[\/\\.\s]/;
        if (name === "") setNameErrorMessage("Name field must be no empty")
        else if (invalidCharsPattern.test(name)) {
            setNameErrorMessage("Invalid username format")
        }
        else {
            sendMessage(`update-player-name ${name}`)
            setName("")
            setNameErrorMessage("");
        }

    };

    const handleEmailUpdate = () => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

        if (email.trim() === "") {
            setEmailErrorMessage("Email field must not be empty");
        } else if (!emailRegex.test(email)) {
            setEmailErrorMessage("Please enter a valid email address");
        } else {
            sendMessage(`update-email ${email}`)
            setEmail("")
            setEmailErrorMessage("");
        }

    };

    const handlePasswordChange = (e) => {
        e.preventDefault();

        if (newPassword !== confirmNewPassword) {
            setPasswordErrorMessage('New password and confirm password must match.');
            return;
        }
        if (!newPassword || !confirmNewPassword) {
            setPasswordErrorMessage('New password fields cannot be empty.');
            return;
        }
        else if (newPassword === currentPassword) {
            setPasswordErrorMessage("New password matches current password")
            return;
        }
        else if (newPassword.length < 8) {
            setPasswordErrorMessage("New password must be at least 8 characters long")
            return;
        }

        sendMessage(`update-password ${currentPassword} ${newPassword}`)
        setPasswordErrorMessage('');
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
    };

    const handleProfileExit = () => {
        navigate("/lobby")
    }

    const handleProfileDelete = () => {
        if (deletePassword === "") {
            setDeleteUserErrorMessage("Fill password field!")
        }
        sendMessage(`Delete-User ${deletePassword}`)
    }

    return (
        <div>
            <nav className="navbar bg-danger">
                <div className="container-fluid">
                    <a className="navbar-brand text-light text" href="#">Spinning Wheel Server</a>
                    <div className="nav-controls">
                        <button id="exit-server" className="btn btn-outline-light"
                                onClick={handleProfileExit}>Back To The Lobby
                        </button>
                    </div>
                </div>
            </nav>

            <div className="profile-container">

                <h2 className="profile-header">Update User Data</h2>

                <div className="user-data-container">
                    <h2>User Profile</h2>

                    {/* Name */}
                    {nameErrorMessage && <p className="text-light">{nameErrorMessage}</p>}
                    <div className="form-group">
                        <label htmlFor="name">Name</label>
                        <input
                            id="name"
                            type="text"
                            className="form-control"
                            value={name}
                            placeholder={playerNamePlaceholder}
                            onChange={(e) => setName(e.target.value)}
                        />
                    </div>

                    <button
                        className="btn btn-outline-light btn-user"
                        onClick={handleNameUpdate}>
                        Update Profile
                    </button>

                    {/* Email */}
                    {emailErrorMessage && <p className="text-light">{emailErrorMessage}</p>}
                    <div className="form-group">
                        <label htmlFor="email">Email</label>
                        <input
                            id="email"
                            type="email"
                            className="form-control"
                            placeholder={emailPlaceholder}
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                    </div>

                    <button
                        className="btn btn-outline-light btn-user"
                        onClick={handleEmailUpdate}>
                        Update Profile
                    </button>

                    {/* User */}
                    {deleteUserErrorMessage && <p className="text-light">{deleteUserErrorMessage}</p>}
                    <div className="form-group">
                        <label htmlFor="email">Delete profile</label>
                        <input
                            id="password"
                            type="password"
                            className="form-control"
                            placeholder="Current password"
                            value={deletePassword}
                            onChange={(e) => setDeletePassword(e.target.value)}
                        />
                    </div>

                    <button
                        className="btn btn-outline-light btn-user"
                        onClick={handleProfileDelete}>
                        Delete Profile
                    </button>


                </div>

                <div className="change-password">

                    {/* Change Password */}
                    <h2>Change Password</h2>
                    <form onSubmit={handlePasswordChange}>
                        {passwordErrorMessage && <p className="text-light">{passwordErrorMessage}</p>}
                        <div className="form-group">
                            <label htmlFor="currentPassword">Current Password</label>
                            <input
                                id="currentPassword"
                                type="password"
                                className="form-control"
                                value={currentPassword}
                                onChange={(e) => setCurrentPassword(e.target.value)}
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="newPassword">New Password</label>
                            <input
                                id="new-password"
                                type="password"
                                className="form-control"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                            />
                        </div>
                        <div className="form-group">
                            <label htmlFor="newPassword">Confirm New Password</label>
                            <input
                                id="confirm-new-password"
                                type="password"
                                className="form-control"
                                value={confirmNewPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                            />
                        </div>
                        <button className="btn btn-outline-light mt-3" type="submit">
                            Change Password
                        </button>
                    </form>
                </div>

                <BalanceGame/>

                <div className="support-container">
                    <h2>Ask Support a Question</h2>
                    {error && <p className="fw-bold text-light">{error}</p>}
                    {successMessage && <p className="fw-bold text-light">{successMessage}</p>}
                    <form onSubmit={handleSupportSubmit}>
                        <div>
                    <textarea
                        value={question}
                        onChange={(e) => setQuestion(e.target.value)}
                        placeholder="Type your question here..."
                        rows="5"
                        className="form-control"
                        style={{width: '100%'}}
                        required
                            />
                            </div>
                        <button className="btn btn-outline-light mt-3" type="submit">Send Question</button>
                    </form>
                </div>

            </div>

            <Footer/>
        </div>
    );
};

export default ProfilePage;
