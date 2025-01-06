import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './registerStyle.css';
import { useWebSocket } from './WebSocketHandler';

const RegisterPage = () => {
    // State for user input
    const [userId, setUserId] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const { initializeSocket } = useWebSocket();

    const navigate = useNavigate();

    const invalidCharsPattern = /[\/\\.\s]/;

    // Handle form submission
    const handleSubmit = (event) => {
        event.preventDefault();

        if (password !== confirmPassword) {
            setError('Passwords do not match');
            return;
        }

        else if (password.length < 8) {
            setError("Password must be at least 8 characters long")
            return;
        }


        else if (invalidCharsPattern.test(userId)) {
            setError("Invalid format of username")
            return;
        }

        setError('');

        initializeSocket(
            `ws://localhost:8080/${encodeURIComponent("register")}/${encodeURIComponent(userId)}/${encodeURIComponent(email)}/${encodeURIComponent(password)}`,
            () => {
                console.log('WebSocket connection established');
                navigate('/lobby');
            },
            () => {
                setError('This register data already exist (email or username)');
            }
        );
    };

    return (
        <div className="loginContainers">
            {/* Left-side image */}
            <div className="loginImage loginContainer">
                <img
                    className="login-logo"
                    src={`${process.env.PUBLIC_URL}/Assets/loginLogo.webp`}
                    alt="Register Logo"
                />
            </div>

            {/* Right-side form */}
            <div className="loginContainer">
                <div id="login">
                    <h2>Create an Account</h2>
                    {error && <p className="text-danger fw-bold">{error}</p>} {/* Display error messages */}
                    <form id="loginForm" onSubmit={handleSubmit}>
                        <div className="form-group">
                            <input
                                type="text"
                                id="userId"
                                className="form-control register-control"
                                placeholder="Unique Player ID"
                                value={userId}
                                onChange={(e) => setUserId(e.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <input
                                type="email"
                                id="email"
                                className="form-control register-control"
                                placeholder="E-mail"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <input
                                type="password"
                                id="password"
                                className="form-control register-control"
                                placeholder="Password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <input
                                type="password"
                                id="confirmPassword"
                                className="form-control register-control"
                                placeholder="Confirm Password"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                required
                            />
                        </div>
                        <p className="signup-text">
                            Already have an account?{' '}
                            <a href="/login" className="signup-link">
                                Login here
                            </a>
                        </p>
                        <button type="submit" className="btn btn-outline-danger button">
                            Register
                        </button>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default RegisterPage;

