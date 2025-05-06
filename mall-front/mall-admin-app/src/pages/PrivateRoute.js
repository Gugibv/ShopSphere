import React, { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import { isAuthenticated } from "./auth";

const PrivateRoute = ({ children }) => {
  const [checking, setChecking] = useState(true);
  const [authed, setAuthed] = useState(false);

  useEffect(() => {
    const check = async () => {
      try {
        const result = await isAuthenticated();
        setAuthed(result);
      } finally {
        setChecking(false); // ✅ 无论如何结束等待
      }
    };
    check();
  }, []);

  if (checking) {
    return <div style={{ padding: 50 }}>⏳ 正在验证登录状态...</div>; // ✅ 不渲染子页面
  }

  return authed ? children : <Navigate to="/?reason=loginRequired" replace />;
};

export default PrivateRoute;
