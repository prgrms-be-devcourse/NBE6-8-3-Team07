"use client";

import Link from "next/link";
import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { customFetch } from "@/utils/customFetch";
import { FaExternalLinkAlt } from "react-icons/fa";
import { FiLink } from "react-icons/fi";

export default function ClientLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [showLoginPopup, setShowLoginPopup] = useState(false);
  const [showLoginRequiredPopup, setShowLoginRequiredPopup] = useState(false); // 로그인 필요 팝업 상태
  const router = useRouter();
  const [loginError, setLoginError] = useState<string | null>(null);

  useEffect(() => {
    const checkInitialLogin = async () => {
      try {
        // 로그인 상태 확인 요청
        const response = await customFetch("https://nbe6-8-2-team07.onrender.com/users/me");

        if (response.ok) {
          setIsLoggedIn(true);
          // setLoginError(null);
          // 로그인 직후의 알림 처리
          const isLoggingIn = sessionStorage.getItem("isLoggingIn");
          if (isLoggingIn === "true") {
            sessionStorage.removeItem("isLoggingIn");
          }
        } else {
          // customFetch 내부에서 재발급 실패 시 여기로 올 수 있음
          setIsLoggedIn(false);
          const isLoggingIn = sessionStorage.getItem("isLoggingIn");
          if (isLoggingIn === "true") {
            setLoginError("로그인에 실패했습니다. 다시 시도해 주세요.");
            sessionStorage.removeItem("isLoggingIn");
            setShowLoginPopup(true); // 실패 시 팝업 다시 표시
          }
        }
      } catch {
        // 네트워크 에러 등
        setIsLoggedIn(false);
        const isLoggingIn = sessionStorage.getItem("isLoggingIn");
        if (isLoggingIn === "true") {
          setLoginError("로그인 중 문제가 발생했습니다.");
          sessionStorage.removeItem("isLoggingIn");
          setShowLoginPopup(true); // 실패 시 팝업 다시 표시
        }
      }
    };

    checkInitialLogin();
  }, []);

  const handleLoginClick = () => {
    sessionStorage.setItem("isLoggingIn", "true");
    setLoginError(null);
  };
  // 로그아웃 로직
  const handleLogout = async () => {
    try {
      await customFetch("https://nbe6-8-2-team07.onrender.com/logout", {
        method: "POST",
        // @ts-expect-error - noRefresh option is not in RequestInit type but needed for logout
        noRefresh: true, // 로그아웃 시에는 토큰 재발급 시도 안 함
      });
    } catch (error) {
      console.error("Logout failed:", error);
    } finally {
      // 서버에서 쿠키를 삭제하므로 클라이언트에서는 상태만 업데이트
      setIsLoggedIn(false);
      setLoginError(null);
      router.push("/");
    }
  };

  // 로그인이 필요한 메뉴 클릭 핸들러
  const handleProtectedLinkClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
    if (!isLoggedIn) {
      e.preventDefault();
      setShowLoginRequiredPopup(true);
    }
  };

  // 팝업 닫기 핸들러
  const handleClosePopup = () => {
    setShowLoginPopup(false);
    setShowLoginRequiredPopup(false);
    setLoginError(null);
  };

  return (
    <>
      <header className="h-26">
        <div className="container mx-auto flex justify-between items-center p-4">
          <Link href="/" className="font-bold text-xl">
            <img src="/images/logo.png" alt="로고" className="h-25" />
          </Link>
          <nav className="flex items-center space-x-8">
            <div className="relative group">
              <button
                className="cursor-pointer py-2"
                onClick={(e) => {
                  if (!isLoggedIn) {
                    e.preventDefault();
                    setShowLoginRequiredPopup(true);
                  }
                }}
              >
                나의 동화책
              </button>
              <div className="absolute z-50 hidden group-hover:block bg-[#FAF9F6] shadow-lg rounded-md mt-0 py-1 w-full min-w-max left-1/2 -translate-x-1/2">
                <Link
                  href="/fairytale/post"
                  onClick={handleProtectedLinkClick}
                  className="block px-4 py-2 text-sm text-center text-gray-700 hover:bg-gray-100"
                >
                  동화책만들기
                </Link>
                <Link
                  href="/fairytale/get"
                  onClick={handleProtectedLinkClick}
                  className="block px-4 py-2 text-sm text-center text-gray-700 hover:bg-gray-100"
                >
                  동화책펼치기
                </Link>
              </div>
            </div>
            <Link
              href="/fairytale/gallery"
              onClick={handleProtectedLinkClick}
              className="py-2"
            >
              동화갤러리
            </Link>
            <Link href="/introduction" className="py-2">
              소개
            </Link>
            {isLoggedIn ? (
              <button onClick={handleLogout} className="py-2 cursor-pointer">
                로그아웃
              </button>
            ) : (
              <button
                onClick={() => {
                  setShowLoginPopup(true);
                  setLoginError(null);
                }}
                className="py-2 cursor-pointer"
              >
                로그인
              </button>
            )}
          </nav>
        </div>
      </header>

      {/* 로그인 팝업 */}
      {(showLoginPopup || showLoginRequiredPopup) && (
        <div
          className="fixed inset-0 bg-black/20 backdrop-blur-sm flex justify-center items-center z-50 p-4"
          onClick={handleClosePopup}
        >
          <div
            className="bg-white rounded-2xl shadow-lg w-full max-w-sm mx-4 relative"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="px-8 py-10 text-center">
              {/* 로고 및 제목 */}
              <div className="mb-4">
                <img
                  src="/images/logo.png"
                  alt="로고"
                  className="max-w-32 h-auto mx-auto mb-3"
                />
                <h3 className="text-2xl font-bold text-gray-900 mb-2">
                  동화공방
                </h3>
                <div className="w-16 h-0.5 bg-orange-400 mx-auto" />
              </div>

              {/* 안내 문구 */}
              <p className="text-gray-600 mb-4 leading-relaxed">
                {showLoginPopup ? (
                  <>
                    아이와 함께 만드는 특별한 동화
                    <br />
                    <span className="text-orange-500 font-semibold">
                      로그인
                    </span>
                    하고 시작해보세요
                  </>
                ) : (
                  <>
                    이 기능은{" "}
                    <span className="text-orange-500 font-semibold">
                      로그인이 필요해요.
                    </span>
                    <br />
                    로그인 후에 계속 이용해 주세요
                  </>
                )}
              </p>

              {/* 로그인 실패 문구*/}
              {loginError && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-3 mb-4">
                  <p className="text-red-600 text-sm font-medium">
                    {loginError}
                  </p>
                </div>
              )}

              {/* 네이버 로그인 버튼 */}
              <Link
                href="https://nbe6-8-2-team07.onrender.com/oauth2/authorization/naver"
                onClick={handleLoginClick}
              >
                <div className="hover:opacity-90 transition-opacity duration-200">
                  <img
                    src="/images/naver-login.png"
                    alt="네이버 로그인 버튼"
                    className="w-full max-w-[200px] h-auto mx-auto"
                  />
                </div>
              </Link>

              {/* 부가 설명 (로그인 팝업일 때만 표시) */}
              {showLoginPopup && (
                <p className="text-xs text-gray-400 mt-4 leading-relaxed">
                  키워드 몇 개로 아이의 상상이 동화가 됩니다.
                  <br />
                  소중한 추억을 함께 만들어보세요
                </p>
              )}
            </div>

            {/* 닫기 버튼 */}
            <button
              onClick={handleClosePopup}
              className="absolute top-4 right-4 w-8 h-8 flex items-center justify-center rounded-full bg-gray-50 hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-colors duration-200"
            >
              &times;
            </button>
          </div>
        </div>
      )}

      <>{children}</>
      <footer className="bg-[#FFD6A5] text-[#9B4500] py-8 px-4 text-center text-sm">
        <p className="text-base font-semibold mb-2">동화공방</p>
        <p>Team07 | 키워드 기반 AI 동화 생성 서비스</p>
        <p>프로그래머스 데브코스 백엔드 6기 2차 프로젝트</p>
        <div className="flex justify-center gap-8 mt-3">
          <Link href="/introduction" className="flex items-center gap-1 hover:underline hover:text-[#7a3000] transition">
            <FiLink className="text-sm" />
            서비스 소개
          </Link>
          <Link
            href="https://github.com/prgrms-be-devcourse/NBE6-8-2-Team07"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1 hover:underline hover:text-[#7a3000] transition"
          >
            <FaExternalLinkAlt className="text-sm" />
            GitHub 저장소
          </Link>
        </div>
      </footer>
    </>
  );
}
