// src/pages/WelcomePage.js
import React from "react";

const WelcomePage = () => {
  const handleLogin = () => {
    // 跳转到 SSO 登录页（服务端跳转）
    window.location.href = "https://cls.loc.lhubsg.com:8080/oauth2/authorization/lhubsso";
  };

  return (
    <div style={{ padding: "40px", textAlign: "center" }}>
      <h1>欢迎使用 Security Connect App</h1>
      <button onClick={handleLogin}>登录</button>
    </div>
  );
};

export default WelcomePage;
